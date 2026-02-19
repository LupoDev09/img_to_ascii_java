package me.lupo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    static void main() throws IOException {
        BufferedImage img = null;
        // Try loading the image
        try {
            img = ImageIO.read(new File("strawberry.jpg"));
        } catch (IOException e) {
            throw new IOException("Error Loading image: " + e.getMessage() + " :3");
        }

    }
}
