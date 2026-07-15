package me.lupo;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public record LoadResult(
        ArrayList<BufferedImage> frames,
        double fps
) {}