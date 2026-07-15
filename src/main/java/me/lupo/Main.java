package me.lupo;

// Parsing
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

// Everything else
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    private static OptionParser parser;

    private static final Logger log = Logger.getInstance();

    private static final boolean MockArgs = true;
    private static final String[] MockArguments = new String[] {
            "--log-level", "debug",
            "--image", "Silly_Cat_Character_smoll.jpg",
            "--width", "0",
            "--height", "50"
    };

    public static void main(String[] args) {
        try {
            log.setColor(false);
            log.info("Logger initialized. " + Logger.getInstance());

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

            parser.accepts("no-output", "Disable output to console, useful for benchmarking");

            OptionSet options;
            if (!MockArgs) {
                options = parser.parse(args);
            } else {
                options = parser.parse(MockArguments);
            }

            log.info("Parsed options: %s", options.asMap());
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

            if (options.has("no-color")) {
                log.info("No color mode activated.");
                log.setColor(false);
            } else {
                log.setColor(true);
                log.info("Color mode activated");
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

            boolean no_output = options.has("no-output");
            if (no_output) {
                log.info("No output mode activated.");
            } else {
                log.info("Output mode activated.");
            }

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

            Integer target_width, target_height;
            target_width = (Integer) options.valueOf("width");
            target_height = (Integer) options.valueOf("height");
            log.info("Width: %d, Height: %d", target_width, target_height);
            if (target_width == 0 && target_height == 0) {
                log.error("It can't be both width and height null");
                parser.printHelpOn(System.out);
                return;
            }

            if (target_width < 0) {
                log.error("Width cannot be negative: %d", target_width);
                parser.printHelpOn(System.out);
                return;
            } else if (target_height < 0) {
                log.error("Height cannot be negative: %d", target_height);
                parser.printHelpOn(System.out);
                return;
            }


            int width = 0, height = 0;
            double aspect = (double) target_width / target_height;
            if (target_width == 0) {
                width = (int) (target_height * aspect); // Breite berechnen
            } else if (target_height == 0) {
                height = (int) (target_width / aspect); // Höhe berechnen
            } else {
                width = target_width;
                height = target_height;
            }

            String charset = options.valueOf("charset").toString();
            Renderer renderer = new Renderer(log.isColor(), charset);
            BufferedImage frame = FFmpegLoader.load(imgPath.getAbsolutePath(), width, height);
            String rendered = renderer.renderFrame(frame);
            if (!no_output) {
                log.info("Rendered frame outputting it");
                IO.print(rendered);
                log.info("Rendered frame outputted");
            }

        } catch (OptionException e) {
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
