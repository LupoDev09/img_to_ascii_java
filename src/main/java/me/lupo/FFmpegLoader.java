package me.lupo;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class FFmpegLoader {

    private static final Logger log = Logger.getInstance();

    public static @NotNull BufferedImage load(String path, Integer width, Integer height) throws Exception {
        log.debug("Load in class FFmpegLoader got called");
        log.info("Loading image from path: %s", path);

        ProcessBuilder pb = getProcessBuilder(path, width, height);
        log.debug("ProcessBuilder command: %s", String.join(" ", pb.command()));

        log.debug("Starting FFmpeg process to load image");
        Process process = pb.start();

        // stderr separat lesen (wichtig!)
        new Thread(() -> {
            try (InputStream err = process.getErrorStream()) {
                err.transferTo(System.err); // oder Logger
            } catch (Exception ignored) {}
        }).start();

        log.debug("Reading image from FFmpeg output stream");
        InputStream is = process.getInputStream();
        log.debug("InputStream available: %d", is.available());

        byte[] data = process.getInputStream().readAllBytes();
        log.info("Bytes: " + data.length);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
        if (img == null) {
            log.error("ImageIO failed to decode image");
            log.error("Bytes: " + data.length);
            log.error("Data: " + new String(data));
            throw new RuntimeException("ImageIO failed to decode image");
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("FFmpeg exited with code " + exit);
        }

        return img;
    }

    private static @NotNull ProcessBuilder getProcessBuilder(String path, Integer width, Integer height) {
        String scale;
        ProcessBuilder pb;

        if ((width == null && height == null) || (width == 0 && height == 0)) {
            pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", path,
                    "-f", "image2pipe",
                    "-vframes", "1",
                    "-pix_fmt", "rgb24",
                    "-vcodec", "png",
                    "-loglevel", "error",
                    "-"
            );
        } else {
            if (height == null || height == 0) {
                scale = width + ":-1"; // auto height
            } else if (width == null || width == 0) {
                scale = "-1:" + height; // auto width
            } else {
                scale = width + ":" + height;
            }

            pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", path,
                    "-vf scale=" + scale,
                    "-f", "image2pipe",
                    "-vframes", "1",
                    "-vcodec", "png",
                    "-hide_banner",
                    "-loglevel", "error",
                    "-"
            );
        }
        pb.redirectErrorStream(false);
        return pb;
    }
}
