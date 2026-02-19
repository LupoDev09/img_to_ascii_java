package me.lupo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    String ASCII = "";

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
        IO.println(imageToAsciiConverter(img, img.getHeight(), img.getWidth()));
    }

    private static String imageToAsciiConverter(BufferedImage img, int img_height, int img_width){
        StringBuilder frame = new StringBuilder();

        for(int y = 0; y < img_height; y++){
            for (int x = 0; x < img_width; x++){
                frame.append(getAscii(img.getRGB()));
            }
            frame.append('\n');
        }

        return frame.toString();
    }

    private static int getAscii(int r, int g, int b){

    }
}
