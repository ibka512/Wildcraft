package dev.wildcraft.player;

/** Development balance values, independent of Minecraft objects and rendering. */
public final class StaminaRules {
    public static final double BASE_CAPACITY = 100;
    public static final double CAPACITY_PER_LEVEL = 2;
    public static final int RECOVERY_DELAY_TICKS = 20;
    public static final double RECOVERY_PER_TICK = 1;
    public static final int SYNC_INTERVAL_TICKS = 4;

    private StaminaRules() {
    }

    public static double capacity(int highestLevel) {
        if (highestLevel < 0) {
            throw new IllegalArgumentException("Highest level must be nonnegative");
        }
        return BASE_CAPACITY + CAPACITY_PER_LEVEL * highestLevel;
    }

    public static double spend(double current, double capacity, double amount) {
        validateCurrent(current, capacity);
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Consumption must be finite and nonnegative");
        }
        return Math.max(0, current - amount);
    }

    public static double recover(double current, double capacity) {
        validateCurrent(current, capacity);
        return Math.min(capacity, current + RECOVERY_PER_TICK);
    }

    private static void validateCurrent(double current, double capacity) {
        if (!Double.isFinite(capacity) || capacity < BASE_CAPACITY
                || !Double.isFinite(current) || current < 0 || current > capacity) {
            throw new IllegalArgumentException("Stamina must be finite and within capacity");
        }
    }
}
