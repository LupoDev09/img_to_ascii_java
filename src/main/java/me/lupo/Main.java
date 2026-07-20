package me.lupo;

// Parsing
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine;

import org.bytedeco.javacv.Frame;
import org.jetbrains.annotations.NotNull;

// Everything else
import java.io.File;

public class Main implements Runnable {
    private static final Logger log = Logger.getInstance();
    // === FLAGS ===
    @ArgGroup(heading = "%nFlags:%n")
    static Flags flags = new Flags();

    static class Flags {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
        boolean help;

        @Option(names = {"--no-color"}, description = "Deactivate color in Terminal output")
        boolean noColor;

        @Option(names = {"--no-audio"}, description = "Deactivate audio")
        boolean noAudio;

        @Option(names = {"--no-output"}, description = "Disable output to console")
        boolean noOutput;

        @Option(names = {"--log-level"}, description = "Set log level (debug, info, warn, error)")
        String logLevel = "ERROR";
    }

    // === INPUT ===
    @ArgGroup(heading = "%nInput:%n")
    Input input = new Input();

    static class Input {
        @Option(
                names = {"-i", "--img", "--image", "--input"},
                description = "Path to the input image",
                required = true
        )
        File image;
    }

    // === SIZE ===
    @ArgGroup(heading = "%nSize:%n", multiplicity = "1..*")
    Size size = new Size();

    static class Size {
        @Option(names = {"--width"}, description = "Width (0 = auto)")
        Integer width; // Bewusst Integer → null = nicht gesetzt

        @Option(names = {"--height"}, description = "Height (0 = auto)")
        Integer height;
    }

    // === RENDER ===
    @ArgGroup(heading = "%nRender:%n")
    Render render = new Render();

    static class Render {
        @Option(names = {"-c", "--charset"}, description = "Charset", defaultValue = " ░▒▓█")
        String charset;
    }
    private static final boolean showMemoryUsage = true;

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

    // TODO: Add Audio Support
    public static void main(String[] args) {
        if (MockArgs) {
            args = MockArguments;
        }
        CommandLine cmd = new CommandLine(new Main());

        cmd.setExecutionExceptionHandler((ex, commandLine, DummyVarForGradlewInTheCLI) -> {
            System.err.println(ex.getMessage());
            commandLine.usage(System.err);
            return 1;
        });

        cmd.setParameterExceptionHandler((ex, DummyVarForGradlewInTheCLI) -> {
            System.err.println(ex.getMessage());
            ex.getCommandLine().usage(System.err);
            return 1;
        });

        // Help wenn keine args
        if (args.length == 0) {
            cmd.usage(System.out);
            return;
        }

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            // === LOGGER SETUP ===
            log.setColor(!flags.noColor);

            try {
                log.setLevel(Logger.Level.valueOf(flags.logLevel.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.error("Invalid log level: %s", flags.logLevel);
                return;
            }

            log.debug("Options parsed successfully");
            log.info("Logger initialized. " + Logger.getInstance());

            if (input.image == null) {
                log.error("Input image is required");
                return;
            }

            // === VALIDATION ===
            if (!input.image.exists()) {
                log.error("Image does not exist: %s", input.image.getAbsolutePath());
                return;
            }

            int targetWidth = (size.width == null) ? 0 : size.width;
            int targetHeight = (size.height == null) ? 0 : size.height;
            log.debug("Width: %d, Height: %d", targetWidth, targetHeight);

            if (targetWidth < 0 || targetHeight < 0) {
                log.error("Width and height cannot be negative");
                return;
            }

            // === INIT ===
            renderer = new Renderer(log.isColor(), render.charset);
            outputWriter = new OutputWriter();
            outputWriter.setFps(1);

            if (!flags.noOutput) {
                outputWriter.start();
            }


            log.debug("Charset: %s", render.charset);
            renderer = new Renderer(log.isColor(), render.charset);
            outputWriter = new OutputWriter();
            outputWriter.setFps(1); // temporärer Default

            // Thread starten, falls Ausgabe gewünscht
            if (!flags.noOutput) {
                outputWriter.start();
            }

            FFmpegLoader.load(input.image.getAbsolutePath(), targetWidth, targetHeight, Main::frameCallback);
        } catch (Exception e) {
            log.error("Unexpected error: %s", e.getMessage());
            log.debug("Stacktrace: %s", (Object) e.getStackTrace());
        } finally {
            // OutputWriter sauber herunterfahren (nur wenn gestartet)
            if (!flags.noOutput) {
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
        if (!flags.noOutput) {
            outputWriter.setFps(fps);
            String rendered_frame = renderer.renderFrame(frame);
            if (showMemoryUsage) rendered_frame += '\n' + GetMemoryUsage() + '\n';
            outputWriter.append(rendered_frame);
        }
    }
}
