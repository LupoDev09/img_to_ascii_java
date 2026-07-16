package me.lupo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class OutputWriter extends Thread implements AutoCloseable {
    Logger log = Logger.getInstance();

    BlockingQueue<String> framesToWrite = new LinkedBlockingQueue<>();
    private volatile double fps;

    private static final String CLEAR_CONSOLE = "\033[2J\033[H";
    private static final String CURSOR_HOME = "\033[H";
    private static final String POISON = "__END__";

    public OutputWriter(double fps) {
        setFps(fps);
    }

    public OutputWriter() {
        setFps(1);
    }

    public void shutdown() throws InterruptedException {
        framesToWrite.add(POISON); // Add the poison pill to unblock the thread if it's waiting
        this.join(); // wartet bis Thread fertig ist
    }

    public void setFps(double fps) {
        this.fps = (fps <= 0) ? 1 : fps;
    }

    public void append(String frame) {
        log.debug("append got called");
        try {
            framesToWrite.put(frame);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        log.debug("run got called");
        try (CursorGuard DummyVarForGradlewInTheCLI = new CursorGuard()) {
            System.out.print(CLEAR_CONSOLE); // Clear console to remove unnecessary stuff
            long nextFrameTime = System.nanoTime();

            while (true) {
                String frame = framesToWrite.poll(10, TimeUnit.MILLISECONDS);
                if (frame != null) {

                    // Use a Poison pill to stop the thread
                    if (frame.equals(POISON)) {
                        log.debug("Poison pill received, exiting thread.");
                        break;
                    }

                    // Output
                    System.out.print(CURSOR_HOME);
                    System.out.print(frame);
                    System.out.flush();

                    // Calc time needed to wait
                    long frameDuration = (long) (1_000_000_000.0 / this.fps);

                    nextFrameTime += frameDuration;

                    long sleepNanos = nextFrameTime - System.nanoTime();
                    if (sleepNanos > 0) {
                        Thread.sleep(sleepNanos / 1_000_000);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        log.debug("close got called");
        shutdown();
    }
}
