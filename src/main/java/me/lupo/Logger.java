package me.lupo;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-sichere Singleton-Logger-Klasse mit verschiedenen Leveln.
 * Verwendung: Logger.getInstance().info("Nachricht");
 */
public final class Logger {

    /**
     * Verfügbare Log-Level, aufsteigend nach Schweregrad.
     */
    public enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }

    // Singleton-Instanz
    private static final Logger INSTANCE = new Logger();

    // Aktuelles Log-Level – volatile für Sichtbarkeit über Threads hinweg
    private volatile Level currentLevel = Level.INFO;

    // Synchronisations-Objekt für atomare Ausgabe
    private final Object writeLock = new Object();

    // Datumsformat für Zeitstempel
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Ausgabekanäle (können bei Bedarf umkonfiguriert werden)
    private final PrintStream out = System.out;
    private final PrintStream err = System.err;

    private boolean useColor = true; // Flag für Farbunterstützung

    private static final String RESET_COLOR = "\u001B[0m"; // ANSI Reset
    private static final String DEBUG_COLOR = "\u001B[34m"; // Blau
    private static final String INFO_COLOR = "\u001B[32m";  // Grün
    private static final String WARN_COLOR = "\u001B[33m";  // Gelb
    private static final String ERROR_COLOR = "\u001B[31m"; // Rot
    private static final String FATAL_COLOR = "\u001B[35m"; // Magenta

    // Privater Konstruktor verhindert externe Instanziierung
    private Logger() {}

    /**
     * Liefert die einzige Instanz des Loggers.
     */
    public static Logger getInstance() {
        return INSTANCE;
    }

    /**
     * Setzt das aktuelle Log-Level. Nachrichten mit einem niedrigeren Level
     * werden ignoriert.
     */
    public void setLevel(Level level) {
        this.currentLevel = level;
    }

    /**
     * Gibt das aktuelle Log-Level zurück.
     */
    public Level getLevel() {
        return currentLevel;
    }

    /**
     * Zentrale Log-Methode. Schreibt die Nachricht nur, wenn das übergebene Level
     * größer oder gleich dem aktuellen Level ist.
     */
    private void log(@NotNull Level level, String message) {
        if (level.ordinal() < currentLevel.ordinal()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(dateFormatter);
        String line = String.format("%s [%s] %s", timestamp, level, message);
        // Je nach Schweregrad auf out oder err ausgeben
        PrintStream target = (level == Level.ERROR || level == Level.FATAL) ? err : out;
        synchronized (writeLock) {
            if (useColor) {
                target.print(getColorString(level));
            }
            target.println(line);
            if (useColor) {
                target.print(RESET_COLOR);
            }
        }
    }

    // --- Convenience-Methoden für die einzelnen Level ---

    public void debug(String msg) {
        log(Level.DEBUG, msg);
    }

    public void info(String msg) {
        log(Level.INFO, msg);
    }

    public void warn(String msg) {
        log(Level.WARN, msg);
    }

    public void error(String msg) {
        log(Level.ERROR, msg);
    }

    public void fatal(String msg) {
        log(Level.FATAL, msg);
    }

    public void debug(String format, Object... args) {
        log(Level.DEBUG, String.format(format, args));
    }

    public void info(String format, Object... args) {
        log(Level.INFO, String.format(format, args));
    }

    public void warn(String format, Object... args) {
        log(Level.WARN, String.format(format, args));
    }

    public void error(String format, Object... args) {
        log(Level.ERROR, String.format(format, args));
    }

    public void fatal(String format, Object... args) {
        log(Level.FATAL, String.format(format, args));
    }

    /**
     * Spezielle Methode für Fehler mit Exception (stacktrace auf stderr).
     */
    public void error(String msg, Throwable t) {
        if (Level.ERROR.ordinal() < currentLevel.ordinal()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(dateFormatter);
        synchronized (writeLock) {
            err.printf("%s [ERROR] %s%n", timestamp, msg);
            t.printStackTrace(err);
        }
    }


    public void setColor(boolean b) {
        useColor = b;
    }

    public boolean isColor() {
        return useColor;
    }

    private String getColorString(@NotNull Level level) {
        return switch (level) {
            case DEBUG -> DEBUG_COLOR; // Blau
            case INFO -> INFO_COLOR;  // Grün
            case WARN -> WARN_COLOR;  // Gelb
            case ERROR -> ERROR_COLOR; // Rot
            case FATAL -> FATAL_COLOR; // Magenta
        };
    }

    @Override
    public String toString() {
        return "Logger{" +
                "currentLevel=" + currentLevel +
                ", useColor=" + useColor +
                ", dateFormatter=" + dateFormatter.format(LocalDateTime.now()) +
                '}';
    }
}