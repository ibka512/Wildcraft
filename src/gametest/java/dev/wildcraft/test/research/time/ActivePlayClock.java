package dev.wildcraft.test.research.time;

/** Test-only monotonic clock. A stalled frame never creates an unbounded bill. */
public final class ActivePlayClock {
    public static final long MAX_STEP_NANOS = 250_000_000L;
    private long previous;
    private boolean started;
    private boolean wasPaused;

    public double advance(long now, boolean paused) {
        if (!started) {
            previous = now;
            started = true;
            wasPaused = paused;
            return 0;
        }
        long elapsed = Math.max(0, now - previous);
        previous = now;
        boolean skip = paused || wasPaused;
        wasPaused = paused;
        return skip ? 0 : Math.min(elapsed, MAX_STEP_NANOS) / 1_000_000_000.0;
    }
}
