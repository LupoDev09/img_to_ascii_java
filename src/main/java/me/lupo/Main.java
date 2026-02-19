package me.lupo;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    static String ASCII = "@%#*+=-:. ";

    static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();

        parser.acceptsAll(List.of("?", "help"), "print this message");
        parser.accepts("img", "The image to use").withRequiredArg().defaultsTo("Silly_Cat_Character_smoll.jpg");

        parser.acceptsAll(List.of("h", "height"))
                .withRequiredArg()
                .ofType(Integer.class);

        parser.acceptsAll(List.of("w", "width"))
                .withRequiredArg()
                .ofType(Integer.class);

        OptionSet options = parser.parse(args);
        if (options.has("help")) {
            IO.println("Usage: java -jar LUPO.jar");
            return;
        }

        Integer targetWidth = (Integer) options.valueOf("width");
        Integer targetHeight = (Integer) options.valueOf("height");

        BufferedImage img = null;
        String image_path = options.valueOf("img").toString();
        IO.println("Image path: " + image_path);

        // Try loading the image
        try {
            img = ImageIO.read(new File(image_path));
        } catch (IOException e) {
            throw new IOException("Error Loading image: " + e.getMessage() + " :3");
        }
        IO.println("Loaded Image :3");
        IO.println("Converting...");
        BufferedImage resized = resize(img, targetWidth, targetHeight);
        System.out.println(imageToAsciiConverter(resized));

    }

    // convert a given Value to the corresponding char in the ASCII string
    private static char getAscii(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;

        // Grauwert (einfacher Durchschnitt)
        int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

        // Map auf ASCII
        int index = (gray * (ASCII.length() - 1)) / 255;

        return ASCII.charAt(index);
    }

    private static BufferedImage resize(BufferedImage img, Integer width, Integer height) {
        int originalWidth = img.getWidth();
        int originalHeight = img.getHeight();

        if (width == null && height == null) {
            return img; // kein scaling
        }

        double aspectRatio = 0.5; // ASCII correction

        if (width != null && height == null) {
            height = (int) (originalHeight * (width / (double) originalWidth) * aspectRatio);
        } else if (height != null && width == null) {
            width = (int) (originalWidth * (height / (double) originalHeight) / aspectRatio);
        }

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int srcX = x * originalWidth / width;
                int srcY = y * originalHeight / height;

                resized.setRGB(x, y, img.getRGB(srcX, srcY));
            }
        }

        return resized;
    }

    // Main entry for Images
    private static String imageToAsciiConverter(BufferedImage img) {
        StringBuilder frame = new StringBuilder();

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                frame.append(getAscii(img.getRGB(x, y)));
            }
            frame.append('\n');
        }

        return frame.toString();
    }
}
