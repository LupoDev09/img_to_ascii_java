package me.lupo;

// Parsing

import joptsimple.OptionParser;
import joptsimple.OptionSet;

// Everything else
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

public class Main {
    static String ASCII = "@%#*+=-:. ";  // Ascii alphabet to use
    static boolean OUTPUTCOLORS = false; // Whether to use color for the output on the CLI

    static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();

        parser.acceptsAll(List.of("?", "help"), "print this message");

        parser.accepts("color", "Activate color in Terminal output");

        parser.accepts("img", "The image to use")
                .withRequiredArg()
                .defaultsTo("Silly_Cat_Character_smoll.jpg");

        parser.accepts("charset")
                .withRequiredArg()
                .defaultsTo("@%#*+=-:. ");

        parser.acceptsAll(List.of("h", "height"))
                .withRequiredArg()
                .ofType(Integer.class);

        parser.acceptsAll(List.of("w", "width"))
                .withRequiredArg()
                .ofType(Integer.class);

        parser.accepts("fps")
                .withRequiredArg()
                .ofType(Integer.class)
                .defaultsTo(10);

        OptionSet options = parser.parse(args);
        try {
            if (options.has("help")) {
                parser.printHelpOn(System.out);
                return;
            }
        } catch (IOException e) {
            IO.println("Something went wrong while printing help. How da fuck?");
            return;
        }

        ASCII = options.valueOf("charset").toString();
        OUTPUTCOLORS = options.has("color");
        int fps = (Integer) options.valueOf("fps");

        Integer targetWidth = (Integer) options.valueOf("width");
        Integer targetHeight = (Integer) options.valueOf("height");

        String image_path = options.valueOf("img").toString();
        IO.println("Image path: " + image_path);
        File file = new File(image_path);

        // Try loading the image
        if (image_path.toLowerCase().endsWith(".gif")) {
            IO.println("GIF detected :3");


            List<BufferedImage> frames = new ArrayList<>();
            try {
                frames = readGifFrames(file); // Raw Frames
            } catch (IOException e){
                IO.println("Something went wrong reading gif Frames: " + e.getMessage());
                return;
            }
            List<String> framesOut = new ArrayList<>();       // Output Frames

            // Preprocess frames
            for (BufferedImage frame : frames) {
                BufferedImage resized = resize(frame, targetWidth, targetHeight);
                String ascii = imageToAsciiConverter(resized);
                framesOut.add(ascii);
            }

            // Render frames
            long frameTime = 1000 / fps;
            try{
                IO.println("\033[?25l"); // Hide Cursor
                for (String frame : framesOut) {
                    long start = System.currentTimeMillis();

                    clearScreen();
                    System.out.println(frame);

                    long elapsed = System.currentTimeMillis() - start;
                    Thread.sleep(Math.max(0, frameTime - elapsed));
                }
            } catch (Exception e){
                IO.println("Error While rendering frame: " + e.getMessage());
            } finally {
                IO.println("\033[?25h"); // Show cursor
                System.out.flush();
            }
        } else {
            BufferedImage img = ImageIO.read(file);

            if (img == null) {
                throw new IOException("Unsupported image format :(");
            }

            BufferedImage resized = resize(img, targetWidth, targetHeight);
            System.out.println(imageToAsciiConverter(resized));
        }
        IO.println("Bye :3");
    }

    // convert a given rgb value to the corresponding char in the ASCII string
    private static char getAscii(int r, int g, int b) {
        // Grauwert (einfacher Durchschnitt)
        int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

        // Map auf ASCII
        int index = (gray * (ASCII.length() - 1)) / 255;

        return ASCII.charAt(index);
    }

    // resizes the image
    private static BufferedImage resize(BufferedImage img, Integer width, Integer height) {
        int originalWidth = img.getWidth();
        int originalHeight = img.getHeight();

        if (width == null && height == null) {
            return img; // kein scaling
        }

        double aspectRatio = 0.5; // ASCII correction

        if (width != null && height == null) {
            height = (int) (originalHeight * (width / (double) originalWidth) * aspectRatio);
        } else if (width == null) {
            width = (int) (originalWidth * (height / (double) originalHeight) / aspectRatio);
        }

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int srcX = (int) ((x / (double) width) * originalWidth);
                int srcY = (int) ((y / (double) height) * originalHeight);

                resized.setRGB(x, y, img.getRGB(srcX, srcY));
            }
        }

        return resized;
    }

    // Utility to clear the terminal with ansi stuff
    private static void clearScreen() {
        System.out.print("\033[H");
        System.out.flush();
    }

    // reads GIF Frames from a file (that is hopefully a GIF)
    private static List<BufferedImage> readGifFrames(File file) throws IOException {
        List<BufferedImage> frames = new ArrayList<>(); // All Frames from the GIF

        try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            if (!readers.hasNext()) {
                throw new IOException("No GIF reader found :(");
            }

            ImageReader reader = readers.next();
            reader.setInput(stream);

            int numFrames = reader.getNumImages(true);

            BufferedImage master = new BufferedImage(
                    reader.getWidth(0),
                    reader.getHeight(0),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics g = master.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, master.getWidth(), master.getHeight());

            for (int i = 0; i < numFrames; i++) {
                BufferedImage frame = reader.read(i);
                g.drawImage(frame, 0, 0, null); // Overlay
                BufferedImage copy = new BufferedImage(master.getWidth(), master.getHeight(), BufferedImage.TYPE_INT_RGB);
                copy.getGraphics().drawImage(master, 0, 0, null);
                frames.add(copy);
            }
        }

        return frames;
    }

    // Main entry for Images
    private static String imageToAsciiConverter(BufferedImage img) {
        StringBuilder frame = new StringBuilder(); // the frame ase a whole

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                // Get RGB values individually
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                char c = getAscii(r, g, b);
                if (OUTPUTCOLORS) {
                    // Ansi magic
                    frame.append("\u001B[38;2;")
                            .append(r).append(";")
                            .append(g).append(";")
                            .append(b).append("m");
                }
                frame.append(c);
            }
            if (OUTPUTCOLORS) {
                frame.append("\u001B[0m"); // Reset
            }
            frame.append('\n'); // Newline for the next line
        }

        return frame.toString();
    }
}
