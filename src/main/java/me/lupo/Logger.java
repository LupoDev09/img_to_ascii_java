package me.lupo;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.BlockingQueue;

public class Logger extends Thread {
    private static Logger instance;

    private Logger(LogLevel level) {
        this.logLevel = level;
        this.logQueue = new java.util.concurrent.LinkedBlockingQueue<>();
        this.running = true;
    }

    public enum LogLevel {
        INFO,
        WARN,
        ERROR
    }

    private static class LogMessage {
        String message;
        LogLevel logLevel;

        LogMessage(String message, LogLevel logLevel) {
            this.message = message;
            this.logLevel = logLevel;
        }
    }

    private LogLevel logLevel;
    private final BlockingQueue<LogMessage> logQueue;
    private boolean running;
    private static final LogMessage POISON_PILL = new LogMessage("", null);

    private boolean color;
    private static final String RESET = "\033[0m";   // setzt Farbe zurück
    private static final String GREEN = "\033[32m";  // INFO
    private static final String YELLOW = "\033[33m"; // WARN
    private static final String RED = "\033[31m";    // ERROR

    // ========================
    // Singleton
    // ========================

    /**
     * Constructs a Logger with the level 'level'
     * @param level the level a message has to have to be printed
     */
    public static synchronized void init(LogLevel level, boolean color) {
        if (instance != null) {
            throw new IllegalStateException("Logger wurde noch nicht initialisiert!");
        }
        instance = new Logger(level);
        instance.color = color;
        instance.start();
    }

    /**
     * Returns the singleton instance of the Logger
     * @return the singleton instance of the Logger
     */
    public static synchronized Logger getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Logger wurde noch nicht initialisiert!");
            // verhindert, dass man ihn benutzt bevor main ihn erstellt
        }
        return instance;
    }

    /**
     * Stops the logger thread and waits for it to finish
     */
    public void stopLogger() {
        this.running = false;
        this.logQueue.add(POISON_PILL); // fügt ein spezielles Element hinzu, um den Thread zu stoppen
        try {
            this.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==========================
    // Setter and getter
    // ==========================

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public synchronized void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isColor() {
        return color;
    }

    public synchronized void setColor(boolean Color) {
        this.color = Color;
    }

    // ==========================
    // Log at different loglevels
    // ==========================

    /**
     * Logs a message at loglevel INFO
     * @param message the message to log
     */
    public void info(String message) {
        logQueue.add(new LogMessage(message, LogLevel.INFO));
    }

    /**
     * Logs a message at loglevel WARN
     * @param message the message to log
     */
    public void warn(String message) {
        logQueue.add(new LogMessage(message, LogLevel.WARN));
    }

    /**
     * Logs a message at loglevel ERROR
     * @param message the message to log
     */
    public void error(String message) {
        logQueue.add(new LogMessage(message, LogLevel.ERROR));
    }

    /**
     * Logs a formated message at loglevel INFO
     * @param message the message to log
     * @param args the arguments for the message
     */
    public void info(String message, Object... args) {
        String formatted = String.format(message, args);
        logQueue.add(new LogMessage(formatted, LogLevel.INFO));
    }

    /**
     * Logs a formated message at loglevel Warn
     * @param message the message to log
     * @param args the arguments for the message
     */
    public void warn(String message, Object... args) {
        String formatted = String.format(message, args);
        logQueue.add(new LogMessage(formatted, LogLevel.WARN));
    }

    /**
     * Logs a formated message at loglevel Error
     * @param message the message to log
     * @param args the arguments for the message
     */
    public void error(String message, Object... args) {
        String formatted = String.format(message, args);
        logQueue.add(new LogMessage(formatted, LogLevel.ERROR));
    }

    /**
     * Logs a formated message at loglevel Error with a Throwable
     * @param message the message to log
     * @param t the Throwable to log
     * @param args the arguments for the message
     */
    public void error(String message, Throwable t, Object... args) {
        String formatted = String.format(message, args);
        // add statt add -> wirft keine Exception wenn Queue voll wäre
        logQueue.add(new LogMessage(
                formatted + "\n" + getStackTrace(t),
                LogLevel.ERROR
        ));
    }

    /**
     * returns a Stacktrace as a String
     * @param t the Throwable to get the stack trace for
     * @return the stack trace as a String
     */
    private String getStackTrace(@NotNull Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }


    // ========================
    // Threading stuff
    // ========================

    @Override
    public void run() {
        try {
            while (running || !logQueue.isEmpty()) {
                // Läuft bis running false ist und die logQueue lehr

                LogMessage msg = logQueue.take(); // blockiert, bis ein Element verfügbar ist

                if (msg == POISON_PILL) {
                    break;
                }

                // Die LogMessage-Objekte werden nur ausgegeben, wenn ihr LogLevel größer oder gleich dem Logger-LogLevel ist
                if (msg.logLevel.ordinal() >= logLevel.ordinal()) {
                    if (color) {
                        switch (msg.logLevel) {
                            case INFO:
                                System.out.print(GREEN);
                                break;
                            case WARN:
                                System.out.print(YELLOW);
                                break;
                            case ERROR:
                                System.out.print(RED);
                                break;
                        }
                    }

                    String time = java.time.LocalTime.now().toString();

                    System.out.println("[" + time + "] [" + msg.logLevel + "] " + msg.message);
                    if (color) {
                        System.out.print(RESET);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Interrupt-Handling
        }
    }

    @Override
    public String toString() {
        return "Logger{" +
                "logLevel=" + logLevel +
                ", logQueue=" + logQueue.toString() +
                ", color=" + color +
                '}';
    }
}
