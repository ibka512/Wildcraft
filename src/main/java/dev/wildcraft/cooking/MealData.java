package dev.wildcraft.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Bounded outcome saved on the actual meal stack; never rerolled after cooking or transfer. */
public record MealData(int schema, int kind, int strength, int seconds) {
    public static final int NONE = 0, WARMTH = 1, COOLING = 2, RECOVERY = 3;
    private record Encoded(int schema, int kind, int strength, int seconds) { }
    private static final Codec<Encoded> ENCODED = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter(Encoded::schema),
            Codec.intRange(0, 3).fieldOf("kind").forGetter(Encoded::kind),
            Codec.intRange(0, 2).fieldOf("strength").forGetter(Encoded::strength),
            Codec.intRange(0, 600).fieldOf("seconds").forGetter(Encoded::seconds)
    ).apply(i, Encoded::new));
    public static final Codec<MealData> CODEC = ENCODED.flatXmap(raw -> {
        try { return DataResult.success(new MealData(raw.schema(), raw.kind(), raw.strength(), raw.seconds())); }
        catch (IllegalArgumentException ex) { return DataResult.error(ex::getMessage); }
    }, meal -> DataResult.success(new Encoded(meal.schema(), meal.kind(), meal.strength(), meal.seconds())));
    public static final StreamCodec<ByteBuf, MealData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MealData::schema, ByteBufCodecs.VAR_INT, MealData::kind,
            ByteBufCodecs.VAR_INT, MealData::strength, ByteBufCodecs.VAR_INT, MealData::seconds, MealData::new);

    public MealData {
        if (schema != 1 || kind < 0 || kind > 3 || strength < 0 || strength > 2 || seconds < 0 || seconds > 600
                || (kind == NONE ? strength != 0 || seconds != 0 : strength == 0 || seconds == 0)) {
            throw new IllegalArgumentException("Invalid cooked meal outcome");
        }
    }
    public String nameKey() {
        return "item.wildcraft.meal." + switch (kind) {
            case WARMTH -> "warming"; case COOLING -> "cooling"; case RECOVERY -> "recovery"; default -> "vegetable";
        };
    }
}
