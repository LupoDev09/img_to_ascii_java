package me.lupo;

import org.bytedeco.javacv.Frame;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;


public class Renderer {
    private final Logger log = Logger.getInstance();
    public static class Config {
        public boolean useColor;
        public String charset;
    }

    private final Config config;

    Renderer(boolean useColor, String charset) {
        this.config = new Config();
        this.config.useColor = useColor;
        this.config.charset = charset;
    }

    public String renderFrame(@NotNull Frame frame) {
        log.info("Render frame got called");

        log.info("Rendering image with dimensions: %dx%d", frame.imageWidth, frame.imageHeight);

        // einmal entscheiden, nicht mehrfach prüfen
        boolean useColor = this.config.useColor;

        int width = frame.imageWidth;
        int height = frame.imageHeight;

        int estimatedSize = width * height * (useColor ? 20 : 2); // The estimated size of the output string, considering color codes and characters
        StringBuilder result = new StringBuilder(estimatedSize);

        ByteBuffer buffer = (ByteBuffer) frame.image[0];
        byte[] array = buffer.hasArray() ? buffer.array() : null;
        int offset = buffer.hasArray() ? buffer.arrayOffset() : 0;


        int stride = frame.imageStride;

        int lastR = -1, lastG = -1, lastB = -1;
        for (int y = 0; y < height; y++) {
            int rowStart = y * stride;
            for (int x = 0; x < width; x++) {

                int pos = rowStart + x * 3;

                int base = offset + pos;

                int b = array != null ? (array[base] & 0xff) : (buffer.get(pos) & 0xff);
                int g = array != null ? (array[base + 1] & 0xff) : (buffer.get(pos + 1) & 0xff);
                int r = array != null ? (array[base + 2] & 0xff) : (buffer.get(pos + 2) & 0xff);

                char pixelChar = getPixelChar(r, g, b);

                if (useColor) {
                    // ANSI Farbe, nur wenn nötig erzeugen
                    if (r != lastR || g != lastG || b != lastB) {
                        result.append('\u001B')
                                .append("[38;2;")
                                .append(r).append(';')
                                .append(g).append(';')
                                .append(b).append('m');

                        lastR = r;
                        lastG = g;
                        lastB = b;
                    }

                    result.append(pixelChar);
                } else {
                    result.append(pixelChar);
                }
            }
            result.append('\n'); // char statt String → minimal schneller
        }

        return result.toString();
    }

    /**
     * Gets the Char based of the luminance of the pixel, based on the given RGB values.
     * @param r the read color value
     * @param g the green color value
     * @param b the blue color value
     * @return the char
     */
    private char getPixelChar(int r, int g, int b) {
        int luminance = (r * 299 + g * 587 + b * 114) / 1000;

        // Only uncomment this sucker if you are sure this thing gets run for every pixel
        // log.debug("Luminance: %f", luminance);

        int index = luminance * (config.charset.length() - 1) / 255;
        return config.charset.charAt(index);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [useColor=" + config.useColor + "]" +
                " [charset=" + config.charset + "]";
    }
}
