package me.lupo;

import java.awt.image.BufferedImage;

public class Renderer {
    private final Logger log = Logger.getInstance();
    public static class Config {
        public boolean useColor;
        public String charset;
    }

    private final Config config;
    private static final String RESET_COLOR = "\u001B[0m";

    Renderer(boolean useColor, String charset) {
        this.config = new Config();
        this.config.useColor = useColor;
        this.config.charset = charset;
    }

    public String renderFrame(BufferedImage img) {
        log.info("Render frame got called");
        if (img == null) {
            log.error("Image is null");
            return "";
        }

        log.info("Rendering image with dimensions: %dx%d", img.getWidth(), img.getHeight());

        StringBuilder result = new StringBuilder();

        int width = img.getWidth();
        int height = img.getHeight();

        // einmal entscheiden, nicht mehrfach prüfen
        boolean useColor = this.config.useColor;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int rgb = img.getRGB(x, y);

                // RGB extrahieren (bitshift ist schneller als Color-Objekt)
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                char pixelChar = getPixelChar(r, g, b);

                if (useColor) {
                    // ANSI Farbe, nur wenn nötig erzeugen (teuer!)
                    result.append("\u001B[38;2;")
                            .append(r).append(";")
                            .append(g).append(";")
                            .append(b).append("m")
                            .append(pixelChar)
                            .append(RESET_COLOR);
                } else {
                    result.append(pixelChar);
                }
            }
            result.append('\n'); // char statt String → minimal schneller
        }

        return result.toString();
    }

    /**
     * calculates the Luminance from the rgb values
     * @param r the read color value
     * @param g the green color value
     * @param b the blue color value
     * @return the calculated luminance value
     */
    private static double CalculateLuminance(int r, int g, int b) {
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    private char getPixelChar(int r, int g, int b) {
        double luminance = CalculateLuminance(r, g, b);
        // log.debug("Luminance: %f", luminance);

        int index = (int) (luminance * (config.charset.length() - 1) / 255);
        return config.charset.charAt(index);
    }
}
