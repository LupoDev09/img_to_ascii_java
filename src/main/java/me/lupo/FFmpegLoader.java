package me.lupo;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.LineUnavailableException;
import java.awt.Dimension;

import static org.bytedeco.ffmpeg.global.avutil.*;

public class FFmpegLoader {
    private static final Logger log = Logger.getInstance();

    public interface FrameCallback {
        void onFrame(Frame frame, double fps);
    }

    @Contract("_, _ -> new")
    private static @NotNull Dimension calculateSize(@NotNull Dimension sourceDimensions, @NotNull Dimension targetDimensions) {
        log.debug(
                "Method calculateSize in FFmpegLoader got called with: sourceDimensions=%s targetDimensions=%s",
                sourceDimensions,
                targetDimensions
        );

        // Terminal characters are roughly twice as high as wide,
        // therefore compensate the height/width calculation.
        double aspect = (double) sourceDimensions.width / sourceDimensions.height;

        if (targetDimensions.width == 0 && targetDimensions.height == 0) {
            return sourceDimensions;

        } else if (targetDimensions.width == 0) {
            int width = (int) (targetDimensions.height * 2 * aspect);
            return new Dimension(width, targetDimensions.height);

        } else if (targetDimensions.height == 0) {
            int height = (int) ((targetDimensions.width / aspect) * 0.5);
            return new Dimension(targetDimensions.width, height);

        } else {
            return targetDimensions;
        }
    }

    public static void load(String path, @NotNull Dimension targetDimensions, FrameCallback onFrame) {
        log.debug("Method load in FFmpegLoader got called with: path=%s targetDimensions=%s", path, targetDimensions);
        log.info("Loading video frames from path: %s", path);

        try (AudioPlayer player = new AudioPlayer();
             FFmpegFrameGrabber probe = new FFmpegFrameGrabber(path);
             FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path)) {
            FFmpegLogCallback.setLevel(AV_LOG_ERROR);

            probe.start();

            Dimension sourceDimensions = new Dimension(probe.getImageWidth(), probe.getImageHeight());
            log.info("Source video dimensions: %dx%d", sourceDimensions.width, sourceDimensions.height);

            Dimension size = calculateSize(sourceDimensions, targetDimensions);
            log.info("Calculated target dimensions: %dx%d", size.width, size.height);

            probe.stop();

            grabber.setSampleFormat(AV_SAMPLE_FMT_S16); // Immer 16bit liefern
            grabber.setPixelFormat(AV_PIX_FMT_BGR24);   // Immer BGR24 liefern
            grabber.setAudioChannels(2);                // Immer Stereo liefern

            // FFmpeg für das scaling
            grabber.setImageWidth(size.width);
            grabber.setImageHeight(size.height);

            grabber.start();

            Frame frame;
            boolean didTheAudioLineFail = false;
            while ((frame = grabber.grab()) != null) {
                if (frame.image != null) {
                    onFrame.onFrame(frame, grabber.getVideoFrameRate());
                }

                if (!didTheAudioLineFail && (frame.samples != null)) {
                    try {
                        if (!player.isPlaying()) {
                            player.start(frame);
                        }
                        player.play(frame);
                    } catch (LineUnavailableException e) {
                        didTheAudioLineFail = true;
                        log.warn("Failed to create audio line: {}", e.getMessage());
                    }
                }
            }
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }
}
