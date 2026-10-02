package dev.wildcraft.test.research.time;

public final class TimeClockCheck {
    public static void main(String[] args) {
        for (int rate : new int[] {5, 10, 20, 60}) {
            ActivePlayClock clock = new ActivePlayClock();
            clock.advance(0, false);
            double billed = 0;
            for (int i = 1; i <= rate * 10; i++) billed += clock.advance(i * 10_000_000_000L / (rate * 10), false);
            check(Math.abs(billed - 10) < 0.00001, "Ten active seconds at " + rate);
        }
        ActivePlayClock clock = new ActivePlayClock();
        check(clock.advance(123, false) == 0, "First sample never bills offline time");
        check(clock.advance(50_000_123, false) == 0.05, "Ordinary step");
        check(clock.advance(90_000_000_000L, true) == 0, "Pause has no fee");
        check(clock.advance(120_000_000_000L, false) == 0, "Resume discards paused interval");
        check(clock.advance(180_000_000_000L, false) == 0.25, "Stall bounded to 250ms");
        check(clock.advance(180_000_000_000L, false) == 0, "Same instant cannot double-bill");
        System.out.println("WILDCRAFT R1 active clock: 10 cases passed; 5/10/20/60Hz, pause, resume, stall, duplicate");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
