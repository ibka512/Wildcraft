package dev.wildcraft.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Saved remaining active milliseconds; offline wall time never appears in this format. */
public record MealEffects(int schema, Effect warmth, Effect cooling, Effect recovery) {
    public record Effect(int strength, int millis) {
        public static final Effect EMPTY = new Effect(0, 0);
        private static final Codec<Effect> RAW = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 2).fieldOf("strength").forGetter(Effect::strength),
                Codec.intRange(0, 600_000).fieldOf("millis").forGetter(Effect::millis)).apply(i, Effect::new));
        public static final Codec<Effect> CODEC = RAW.validate(e -> (e.strength == 0) == (e.millis == 0)
                ? DataResult.success(e) : DataResult.error(() -> "Inconsistent meal effect"));
        public Effect elapse(int ms) { return millis <= ms ? EMPTY : new Effect(strength, millis - ms); }
        public Effect merge(int level, int duration) {
            if (level < strength) return this;
            return new Effect(level, level == strength ? Math.max(millis, duration) : duration);
        }
    }
    public static final MealEffects EMPTY = new MealEffects(1, Effect.EMPTY, Effect.EMPTY, Effect.EMPTY);
    public static final Codec<MealEffects> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter(MealEffects::schema),
            Effect.CODEC.fieldOf("warmth").forGetter(MealEffects::warmth),
            Effect.CODEC.fieldOf("cooling").forGetter(MealEffects::cooling),
            Effect.CODEC.fieldOf("recovery").forGetter(MealEffects::recovery)).apply(i, MealEffects::new));
    public MealEffects eat(MealData meal) {
        int level=meal.strength(), ms=meal.seconds()*1000;
        return switch(meal.kind()) {
            case MealData.WARMTH -> new MealEffects(1,warmth.merge(level,ms),cooling,recovery);
            case MealData.COOLING -> new MealEffects(1,warmth,cooling.merge(level,ms),recovery);
            case MealData.RECOVERY -> new MealEffects(1,warmth,cooling,recovery.merge(level,ms));
            default -> this;
        };
    }
    public MealEffects elapse(int ms) { return new MealEffects(1,warmth.elapse(ms),cooling.elapse(ms),recovery.elapse(ms)); }
}
