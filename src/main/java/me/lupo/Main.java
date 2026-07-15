package me.lupo;

// Parsing
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.jetbrains.annotations.NotNull;

// Everything else
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String CLEAR_CONSOLE = "\033[2J\033[H";
    private static final String CURSOR_HOME = "\033[H";

    private static final OptionParser parser = setParser();

    private static final Logger log = Logger.getInstance();

    private static final boolean MockArgs = true;
    private static final String[] MockArguments = new String[] {
            "--log-level", "ERROR",
            "--image", "funny.gif",
            "--no-output",
            "--height", "124"
    };

    static void main(String[] args) {
        try {
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

            int targetWidth = (int) options.valueOf("width");
            int targetHeight = (int) options.valueOf("height");
            log.debug("Width: %d, Height: %d", targetWidth, targetHeight);

            if (targetWidth < 0 || targetHeight < 0) {
                log.error("Width and height cannot be negative");
                return;
            }

            String charset = options.valueOf("charset").toString();
            Renderer renderer = new Renderer(log.isColor(), charset);
            LoadResult frames = FFmpegLoader.load(imgPath.getAbsolutePath(), targetWidth, targetHeight);
            ArrayList<String> rendered_frames = renderer.renderFrames(frames.frames());

            try (CursorGuard _ = new CursorGuard()) {// Clear the Console before writing frames to it
                if (!no_output) IO.print(CLEAR_CONSOLE);

                // Render output frames one by one
                for (int i = 0; i < rendered_frames.size(); i++) {
                    String rendered_frame = rendered_frames.get(i);
                    if (!no_output) {
                        IO.print(rendered_frame);

                        // Nur löschen, wenn noch ein Frame danach kommt
                        if (i != rendered_frames.size() - 1) {
                            IO.print(CURSOR_HOME);
                        }

                        Thread.sleep((long) (1000.0 / frames.fps()));
                    }
                }
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
            log.error("Unexpected error: %s", e);
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
