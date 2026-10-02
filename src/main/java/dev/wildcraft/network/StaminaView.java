package dev.wildcraft.network;

import dev.wildcraft.player.StaminaData;
import dev.wildcraft.player.StaminaRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Only the owner's display values are sent; recovery timers stay on the server. */
public record StaminaView(int highestLevel, double stamina) {
    public static final StreamCodec<ByteBuf, StaminaView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StaminaView::highestLevel,
            ByteBufCodecs.DOUBLE, StaminaView::stamina,
            StaminaView::new);

    public static StaminaView of(StaminaData data) {
        return new StaminaView(data.highestLevel(), data.stamina());
    }

    public double capacity() {
        return StaminaRules.capacity(highestLevel);
    }
}
