package me.lupo;

// Parsing
import joptsimple.OptionParser;
import joptsimple.OptionSet;

// Everything else
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            Logger.init(Logger.LogLevel.INFO, true);
            Logger.getInstance().info("Logger initialized. " + Logger.getInstance().toString());

            OptionParser parser = new OptionParser();

            parser.acceptsAll(List.of("?", "help"), "print this message");
            parser.accepts("no-color", "Deactivate color in Terminal output");

            parser.accepts("log-level", "Set log level (debug, info, warn, error)")
                    .withRequiredArg()
                    .ofType(String.class);
            parser.acceptsAll(List.of("img", "image", "input", "i"), "Path to the input image")
                    .withRequiredArg()
                    .ofType(String.class);


            OptionSet options = parser.parse(args);
            try {
                if (options.has("help")) {
                    Logger.getInstance().info("Help requested, printing help and exiting.");
                    parser.printHelpOn(System.out);
                    return;
                }
            } catch (IOException e) {
                Logger.getInstance().error("Something went wrong while printing help. How da fuck?", e);
                return;
            }

            if (options.has("log-level")) {
                String value = options.valueOf("log-level").toString();
                value = value.toUpperCase();
                try {
                    Logger.getInstance().setLogLevel(Logger.LogLevel.valueOf(value));
                    Logger.getInstance().info("Log level set to: {}", value);
                } catch (IllegalArgumentException e) {
                    Logger.getInstance().error("Invalid log level: {}", value);
                    System.exit(1);
                }
            }
            Logger.getInstance().info("Parsed options: {}", options.asMap());


            File imgPath;
            if (!options.has("image")) {
                Logger.getInstance().error("No input image provided. Use --image <path> to specify an image.");
                parser.printHelpOn(System.out);
                return;
            } else {
                imgPath = new File(options.valueOf("image").toString());
                if (!imgPath.exists()) {
                    Logger.getInstance().error("Specified image path does not exist: {}", imgPath);
                    parser.printHelpOn(System.out);
                    return;
                }
                Logger.getInstance().info("Input image path: {}", imgPath);
            }

            if (options.has("no-color")) {
                Logger.getInstance().info("No color mode activated.");
                Logger.getInstance().setColor(false);
            }


        } catch (Exception e) {
            if (Logger.getInstance() != null) {
                Logger.getInstance().error("An unexpected error occurred.", e);
            } else {
                System.err.println("An unexpected error occurred: " + e.getMessage());
                System.err.println(Arrays.toString(e.getStackTrace()));
            }
        } finally {
            if (Logger.getInstance() != null) {
                Logger.getInstance().info("Bye :3");
                Logger.getInstance().stopLogger();
            }
        }
    }
}
