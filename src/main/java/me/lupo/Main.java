package me.lupo;

// Parsing
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.jetbrains.annotations.NotNull;

// Everything else
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    private static final OptionParser parser = setParser();
    private static final Logger log = Logger.getInstance();

    // Mock args so I can change the args easier in IntelliJ
    private static final boolean MockArgs = true;
    private static final String[] MockArguments = new String[] {
            "--log-level", "error",
            "--image", "funny.gif",
            "--height", "40"
    };

    // TODO: Add Audio Support
    public static void main(String[] args) {
        try (OutputWriter outputWriter = new OutputWriter()) {
            log.setColor(false);
            log.info("Logger initialized. " + Logger.getInstance());

            OptionSet options;
            if (!MockArgs) {
                options = parser.parse(args);
            } else {
                options = parser.parse(MockArguments);
            }

            log.debug("Parsed options: %s", options.asMap());
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
                log.debug("No color mode activated.");
                log.setColor(false);
            } else {
                log.setColor(true);
                log.debug("Color mode activated");
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
                log.debug("No output mode activated.");
            } else {
                log.debug("Output mode activated.");
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
                log.debug("Input image path: %s", imgPath.getAbsolutePath());
            }

            int targetWidth = (Integer) options.valueOf("width");
            int targetHeight = (Integer) options.valueOf("height");
            log.debug("Width: %d, Height: %d", targetWidth, targetHeight);

            if (targetWidth < 0 || targetHeight < 0) {
                log.error("Width and height cannot be negative");
                return;
            }

            String charset = (String) options.valueOf("charset");
            log.debug("Charset: %s", charset);
            Renderer renderer = new Renderer(log.isColor(), charset);

            /*
             * TODO: Replace the current way to load and render frames with some sort of a onFrame callback
             *  and an output thread To save on Memory
             */
            LoadResult frames = FFmpegLoader.load(imgPath.getAbsolutePath(), targetWidth, targetHeight);
            outputWriter.setFps(frames.fps());
            outputWriter.start();

            // Render output frames one by one
            if (!no_output) {
                for (BufferedImage frame : frames.frames()) {
                    String rendered_frame = renderer.renderFrame(frame);
                    outputWriter.append(rendered_frame);
                }
            } else {
                outputWriter.shutdown();
                outputWriter.join(); // wartet bis Thread fertig ist
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
            log.error("Unexpected error: %s", e.getMessage());
            log.debug("Stacktrace: %s", (Object) e.getStackTrace());
        } finally {
            log.info("Bye :3");
        }
    }

    private static @NotNull OptionParser setParser() {
        OptionParser parser = new OptionParser();
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

        parser.accepts("no-output", "Disable output to console, useful for benchmarking");

        return parser;
    }
}
