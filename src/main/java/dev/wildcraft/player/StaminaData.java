package dev.wildcraft.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable saved progress. Capacity is always derived from the recorded peak. */
public record StaminaData(int schemaVersion, int highestLevel, double stamina, int recoveryDelay) {
    public static final int SCHEMA_VERSION = 1;
    private static final Codec<Double> FINITE_NONNEGATIVE = Codec.DOUBLE.validate(value ->
            Double.isFinite(value) && value >= 0 ? DataResult.success(value)
                    : DataResult.error(() -> "Stamina must be finite and nonnegative"));
    public static final Codec<StaminaData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, SCHEMA_VERSION).optionalFieldOf("schemaVersion", SCHEMA_VERSION).forGetter(StaminaData::schemaVersion),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("highestLevel", 0).forGetter(StaminaData::highestLevel),
            FINITE_NONNEGATIVE.optionalFieldOf("stamina", 0.0).forGetter(StaminaData::stamina),
            Codec.intRange(0, StaminaRules.RECOVERY_DELAY_TICKS).optionalFieldOf("recoveryDelay", 0).forGetter(StaminaData::recoveryDelay)
    ).apply(instance, StaminaData::new));

    public StaminaData {
        if (schemaVersion != SCHEMA_VERSION || highestLevel < 0 || !Double.isFinite(stamina)
                || stamina < 0 || recoveryDelay < 0 || recoveryDelay > StaminaRules.RECOVERY_DELAY_TICKS) {
            throw new IllegalArgumentException("Invalid stamina save data");
        }
        // A later balance adjustment may lower capacity, without erasing the peak.
        stamina = Math.min(stamina, StaminaRules.capacity(highestLevel));
    }

    public static StaminaData initial(int currentLevel) {
        int level = Math.max(0, currentLevel);
        return new StaminaData(SCHEMA_VERSION, level, StaminaRules.capacity(level), 0);
    }

    public double capacity() {
        return StaminaRules.capacity(highestLevel);
    }

    public StaminaData observeLevel(int currentLevel) {
        int peak = Math.max(highestLevel, currentLevel);
        return peak == highestLevel ? this : new StaminaData(SCHEMA_VERSION, peak, stamina, recoveryDelay);
    }

    public StaminaData consume(double amount) {
        double remaining = StaminaRules.spend(stamina, capacity(), amount);
        return remaining == stamina ? this : new StaminaData(SCHEMA_VERSION, highestLevel, remaining, StaminaRules.RECOVERY_DELAY_TICKS);
    }

    public StaminaData filled() {
        return new StaminaData(SCHEMA_VERSION, highestLevel, capacity(), 0);
    }

    public StaminaData tick(boolean canRecover) { return tick(canRecover, 1); }

    public StaminaData tick(boolean canRecover, double multiplier) {
        if (stamina == capacity()) {
            return recoveryDelay == 0 ? this : new StaminaData(SCHEMA_VERSION, highestLevel, stamina, 0);
        }
        if (!canRecover) {
            return recoveryDelay == StaminaRules.RECOVERY_DELAY_TICKS ? this
                    : new StaminaData(SCHEMA_VERSION, highestLevel, stamina, StaminaRules.RECOVERY_DELAY_TICKS);
        }
        if (recoveryDelay > 0) {
            return new StaminaData(SCHEMA_VERSION, highestLevel, stamina, recoveryDelay - 1);
        }
        return new StaminaData(SCHEMA_VERSION, highestLevel, Math.min(capacity(), stamina + StaminaRules.RECOVERY_PER_TICK * (Double.isFinite(multiplier) ? Math.clamp(multiplier, 1, 2) : 1)), 0);
    }
}
