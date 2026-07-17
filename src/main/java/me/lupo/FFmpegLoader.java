package me.lupo;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.Dimension;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGR24;

public class FFmpegLoader {
    private static final Logger log = Logger.getInstance();

    public interface FrameCallback {
        void onFrame(Frame frame, double fps);
    }

    @Contract("_, _, _, _ -> new")
    public static @NotNull Dimension calculateSize(int originalWidth, int originalHeight, int targetWidth, int targetHeight) {
        log.debug(
                "Method calculateSize in FFmpegLoader got called with: originalWidth=%s originalHeight=%s targetWidth=%s targetHeight=%s",
                originalWidth,
                originalHeight,
                targetWidth,
                targetHeight
        );

        // Terminal characters are roughly twice as high as wide,
        // therefore compensate the height/width calculation.
        double aspect = (double) originalWidth / originalHeight;

        if (targetWidth == 0 && targetHeight == 0) {
            return new Dimension(originalWidth, originalHeight);

        } else if (targetWidth == 0) {
            int width = (int) (targetHeight * 2 * aspect);
            return new Dimension(width, targetHeight);

        } else if (targetHeight == 0) {
            int height = (int) ((targetWidth / aspect) * 0.5);
            return new Dimension(targetWidth, height);

        } else {
            return new Dimension(targetWidth, targetHeight);
        }
    }

    public static void load(String path, Integer targetWidth, Integer targetHeight, FrameCallback onFrame) throws FFmpegFrameGrabber.Exception {
        log.debug("Method load in FFmpegLoader got called with: path=%s targetWidth=%s targetHeight=%s",
                path, targetWidth, targetHeight);

        log.info("Loading video frames from path: %s", path);

        FFmpegLogCallback.setLevel(AV_LOG_ERROR);
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path);

        try {
            grabber.setPixelFormat(AV_PIX_FMT_BGR24); // Immer BGR24 liefern
            grabber.start(); // Start the grabber once to set the Image width and height for the scaling

            int originalWidth = grabber.getImageWidth();
            int originalHeight = grabber.getImageHeight();

            Dimension size = calculateSize(
                    originalWidth,
                    originalHeight,
                    targetWidth,
                    targetHeight
            );

            grabber.stop();


            // FFmpeg für das scaling
            grabber.setImageWidth(size.width);
            grabber.setImageHeight(size.height);

            grabber.start();

            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                onFrame.onFrame(frame, grabber.getVideoFrameRate());
            }
        } finally {
            grabber.stop();
            grabber.release();
        }
    }
}
