package dev.wildcraft.network;

import dev.wildcraft.cooking.MealEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Only levels and rounded remaining seconds, privately sent to the consumer. */
public record MealView(int warmth, int warmSeconds, int cooling, int coolSeconds, int recovery, int recoverySeconds) {
    public static final StreamCodec<ByteBuf, MealView> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MealView::warmth, ByteBufCodecs.VAR_INT, MealView::warmSeconds,
            ByteBufCodecs.VAR_INT, MealView::cooling, ByteBufCodecs.VAR_INT, MealView::coolSeconds,
            ByteBufCodecs.VAR_INT, MealView::recovery, ByteBufCodecs.VAR_INT, MealView::recoverySeconds, MealView::new);
    public static MealView of(MealEffects e) { return new MealView(e.warmth().strength(), seconds(e.warmth()),
            e.cooling().strength(),seconds(e.cooling()),e.recovery().strength(),seconds(e.recovery())); }
    private static int seconds(MealEffects.Effect e) { return (e.millis()+999)/1000; }
}
