package me.lupo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    static String ASCII = "@%#*+=-:. ";

    static void main() throws IOException {
        BufferedImage img = null;
        // Try loading the image
        try {
            img = ImageIO.read(new File("Silly_Cat_Character_smoll.jpg"));
        } catch (IOException e) {
            throw new IOException("Error Loading image: " + e.getMessage() + " :3");
        }
        IO.println("Loaded Image :3");
        IO.println("try image to ascii conversion");
        System.out.println(imageToAsciiConverter(img));
    }

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

    private static char getAscii(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;

        // Grauwert (einfacher Durchschnitt)
        int gray = (r + g + b) / 3;

        // Map auf ASCII
        int index = (gray * (ASCII.length() - 1)) / 255;

        return ASCII.charAt(index);
    }
}
