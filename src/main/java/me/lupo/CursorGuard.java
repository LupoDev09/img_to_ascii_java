package me.lupo;

public class CursorGuard implements AutoCloseable {
    Logger log = Logger.getInstance();

    public CursorGuard() {
        log.debug("CursorGuard created");
        IO.println("\033[?25l"); // Hide cursor
    }

    @Override
    public void close() {
        log.debug("CursorGuard cleaned up");
        IO.println("\033[?25h"); // Show cursor
    }
}
