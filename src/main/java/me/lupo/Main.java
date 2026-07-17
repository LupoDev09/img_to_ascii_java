package me.lupo;

// Parsing
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.bytedeco.javacv.Frame;
import org.jetbrains.annotations.NotNull;

// Everything else
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    private static final Logger log = Logger.getInstance();
    private static final OptionParser parser = setParser();
    private static boolean no_output = false;
    private static OutputWriter outputWriter;
    private static Renderer renderer;

    private static final String RESET_COLOR = "\u001B[0m";

    // Mock args so I can change the args easier in IntelliJ
    private static final boolean MockArgs = false;
    private static final String[] MockArguments = new String[] {
            "--log-level", "error",
            "--image", "funny.gif",
            "--height", "40"
    };

    private static final boolean showMemoryUsage = true;

    // TODO: Add Audio Support
    public static void main(String[] args) {
        try {
            log.setColor(false);
            log.setLevel(Logger.Level.ERROR);
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

            no_output = options.has("no-output");
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
            renderer = new Renderer(log.isColor(), charset);
            outputWriter = new OutputWriter();
            outputWriter.setFps(1); // temporärer Default

            // Thread starten, falls Ausgabe gewünscht
            if (!no_output) {
                outputWriter.start();
            }

            FFmpegLoader.load(imgPath.getAbsolutePath(), targetWidth, targetHeight, Main::frameCallback);
        } catch (OptionException e) {
            log.info("Missing required options");
            try {
                parser.printHelpOn(System.out);
            } catch (IOException ioException) {
                log.error("Something went wrong while printing help. How da fuck? %s", ioException.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error: %s", e.getMessage());
            log.debug("Stacktrace: %s", (Object) e.getStackTrace());
        } finally {
            // OutputWriter sauber herunterfahren (nur wenn gestartet)
            if (!no_output) {
                try {
                    if (outputWriter != null) outputWriter.shutdown();
                } catch (InterruptedException e) {
                    log.error("Interrupted while shutting down output writer");
                    Thread.currentThread().interrupt();
                }
            }
            System.out.print(RESET_COLOR);
            System.out.println("Bye :3");
        }
    }


    /**
     * sets up the CLI parser
     * @return the configured OptionParser
     */
    private static @NotNull OptionParser setParser() {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(List.of("?", "help"), "print this message");
        parser.accepts("no-color", "Deactivate color in Terminal output");
        parser.accepts("no-audio", "Deactivate audio");
        parser.accepts("no-output", "Disable output to console, useful for benchmarking");

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

        return parser;
    }


    /**
     * returns a string with the current heap usage the heap Total size and the max Heap size
     * @return the String described above
     */
    public static @NotNull String GetMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();        // Currently allocated heap
        long freeMemory = runtime.freeMemory();          // Free heap space
        long usedMemory = totalMemory - freeMemory;      // Actually used heap
        long maxMemory = runtime.maxMemory();            // Max heap it can grow to

        return String.format("Heap Used: %d MB, Heap Total: %d MB, Heap Max: %d MB",
                usedMemory / 1024 / 1024,
                totalMemory / 1024 / 1024,
                maxMemory / 1024 / 1024);
    }


    /**
     * the callback function for the FFmpegLoader, it will be called for every frame received
     * @param frame the frame to proces
     * @param fps the fps of the frame (after the first one basically useless)
     */
    private static void frameCallback (@NotNull Frame frame, double fps) {
        log.debug("Frame received with dimensions: %dx%d", frame.imageWidth, frame.imageHeight);
        if (!no_output) {
            outputWriter.setFps(fps);
            String rendered_frame = renderer.renderFrame(frame);
            if (showMemoryUsage) rendered_frame += '\n' + GetMemoryUsage() + '\n';
            outputWriter.append(rendered_frame);
        }
    }

}
