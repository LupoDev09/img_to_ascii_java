package me.lupo;

// Parsing
import joptsimple.OptionParser;
import joptsimple.OptionSet;

// Everything else
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    private static OptionParser parser;
    private static final Logger log = Logger.getInstance();

    public static void main(String[] args) {
        try {
            log.info("Logger initialized. " + Logger.getInstance().toString());

            parser = new OptionParser();

            parser.acceptsAll(List.of("?", "help"), "print this message");
            parser.accepts("no-color", "Deactivate color in Terminal output");
            parser.accepts("no-audio", "Deactivate audio");

            parser.accepts("log-level", "Set log level (debug, info, warn, error)")
                    .withRequiredArg()
                    .ofType(String.class);

            parser.acceptsAll(List.of("img", "image", "input", "i"), "Path to the input image")
                    .withRequiredArg()
                    .ofType(String.class)
                    .required();
            parser.acceptsAll(List.of("width", "w"), "Width of the output ASCII art 0 = auto")
                    .withRequiredArg()
                    .ofType(Integer.class)
                    .defaultsTo(0);
            parser.acceptsAll(List.of("height", "h"), "Height of the output ASCII art 0 = auto")
                    .withRequiredArg()
                    .ofType(Integer.class)
                    .defaultsTo(0);
            parser.acceptsAll(List.of("c", "charset"), "Charset for the output ASCII art")
                    .withRequiredArg()
                    .ofType(String.class)
                    .defaultsTo(" ░▒▓█");

            parser.acceptsAll(List.of("fps", "frames-per-second"), "Frames per second for the output ASCII art")
                    .withRequiredArg()
                    .ofType(Integer.class);

            OptionSet options = parser.parse(args);
            try {
                if (options.has("help")) {
                    log.info("Help requested, printing help and exiting.");
                    parser.printHelpOn(System.out);
                    return;
                }
            } catch (IOException e) {
                log.error("Something went wrong while printing help. How da fuck? %s", e.getMessage());
                return;
            }

            if (options.has("log-level")) {
                String value = options.valueOf("log-level").toString();
                value = value.toUpperCase();
                try {
                    log.setLevel(Logger.Level.valueOf(value));
                    log.info("Log level set to: %s", value);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid log level: %s", value);
                    System.exit(1);
                }
            }
            log.info("Parsed options: %s", options.asMap());


            File imgPath;
            if (!options.has("image")) {
                log.error("No input image provided. Use --image <path> to specify an image.");
                parser.printHelpOn(System.out);
                return;
            } else {
                imgPath = new File(options.valueOf("image").toString());
                if (!imgPath.exists()) {
                    log.error("Specified image path does not exist: %s", imgPath.getAbsolutePath());
                    parser.printHelpOn(System.out);
                    return;
                }
                log.info("Input image path: %s", imgPath.getAbsolutePath());
            }

            if (options.has("no-color")) {
                log.info("No color mode activated.");
                log.setColor(false);
            }

            Integer width, height;
            width = (Integer) options.valueOf("width");
            height = (Integer) options.valueOf("height");
            log.info("Width: %d, Height: %d", width, height);
            if (width < 0) {
                log.error("Width cannot be negative: %d", width);
                parser.printHelpOn(System.out);
                return;
            } else if (height < 0) {
                log.error("Height cannot be negative: %d", height);
                parser.printHelpOn(System.out);
                return;
            }

            String charset = options.valueOf("charset").toString();
        } catch (joptsimple.OptionException e) {
            log.error("Missing required options");
            try {
                parser.printHelpOn(System.out);
            } catch (IOException ioException) {
                log.error("Something went wrong while printing help. How da fuck? %s", ioException.getMessage());
            }
        }
        catch (Exception e) {
            log.error("An unexpected error occurred. %s", e.getMessage());
        } finally {
            log.info("Bye :3");
        }
    }
}
