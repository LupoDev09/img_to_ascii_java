package me.lupo;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;

public class FFmpegLoader {

    private static final Logger log = Logger.getInstance();

    @Contract("_, _, _, _ -> new")
    public static @NotNull Dimension calculateSize(int originalWidth, int originalHeight, int targetWidth, int targetHeight) {
        log.debug(
                "Method calculateSize in FFmpegLoader got called with: originalWidth=%s originalHeight=%s targetWidth=%s targetHeight=%s",
                originalWidth,
                originalHeight,
                targetWidth,
                targetHeight
        );

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

    public static @NotNull LoadResult load(String path, Integer targetWidth, Integer targetHeight) throws Exception {
        log.debug("Method load in FFmpegLoader got called with: path=%s targetWidth=%s targetHeight=%s",
                path, targetWidth, targetHeight);

        log.info("Loading video frames from path: %s", path);

        ArrayList<BufferedImage> frames = new ArrayList<>();

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path);

        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
            FFmpegLogCallback.setLevel(AV_LOG_ERROR);
            grabber.start();

            int originalWidth = grabber.getImageWidth();
            int originalHeight = grabber.getImageHeight();

            Dimension size = calculateSize(
                    originalWidth,
                    originalHeight,
                    targetWidth,
                    targetHeight
            );
            
            Frame frame;

            while ((frame = grabber.grabImage()) != null) {

                BufferedImage img = converter.convert(frame);

                if (img == null) {
                    continue;
                }

                BufferedImage scaled = new BufferedImage(
                        size.width,
                        size.height,
                        BufferedImage.TYPE_INT_RGB
                );

                Graphics2D g = scaled.createGraphics();

                g.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR
                );

                g.drawImage(
                        img,
                        0,
                        0,
                        size.width,
                        size.height,
                        null
                );

                g.dispose();

                frames.add(scaled);
            }

            log.info("Loaded %d frames", frames.size());

            return new LoadResult(frames, grabber.getVideoFrameRate());

        } finally {
            grabber.stop();
            grabber.release();
        }
    }
}
