package dev.wildcraft.temperature;

/** Relative environment index, never Celsius or a body-temperature resource. */
public final class TemperatureRules {
    public static final int SAMPLE_TICKS = 10;
    public static final int HEAT_RADIUS = 4;
    public static final int MAX_BLOCK_READS = 729;
    public static final int MAX_HEAT_RAYS = 16;
    public static final float WATER_COOLING = -1.2F;
    public static final double HYSTERESIS = .12;
    public static final String[] STATES = {"extreme_cold", "cold", "cool", "comfortable", "warm", "hot", "extreme_hot"};
    private TemperatureRules() { }
    public static float clamp(float value) { return Float.isFinite(value) ? Math.clamp(value, -3F, 3F) : 0; }
    public static float biome(float climate) {
        if (!Float.isFinite(climate)) return 0;
        if (climate <= 0) return -2.8F;
        if (climate < .25F) return -1.8F;
        if (climate < .5F) return -.8F;
        if (climate <= .8F) return 0;
        if (climate < 1.5F) return .8F;
        return 1.8F;
    }
    public static float target(float base, float weather, boolean water, float heat) {
        return clamp(base + weather + (water ? WATER_COOLING : 0) + Math.clamp(heat, 0F, 2.4F));
    }
    public static double smooth(double current, double target, double seconds) {
        if (!Double.isFinite(current)) current = 0;
        if (!Double.isFinite(target)) target = 0;
        double dt = Double.isFinite(seconds) ? Math.clamp(seconds, 0, .25) : 0;
        return Math.clamp(current + (Math.clamp(target, -3, 3) - current) * -Math.expm1(-dt / 1.2), -3, 3);
    }
    public static int band(double value, int previous) {
        if (!Double.isFinite(value)) value = 0;
        int index = Math.clamp(previous, 0, 6);
        while (index < 6 && value > index - 2.5 + HYSTERESIS) index++;
        while (index > 0 && value < index - 3.5 - HYSTERESIS) index--;
        return index;
    }
    public static int initialBand(double value) { return Math.clamp((int)Math.floor(value + 3.5), 0, 6); }
}
