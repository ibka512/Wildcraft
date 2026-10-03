package dev.wildcraft.network;

import dev.wildcraft.temperature.TemperatureRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Only the owning player's bounded target reading; no inventory, position or persistent body state. */
public record TemperatureView(float target) {
    public TemperatureView { target = TemperatureRules.clamp(target); }
    public static final StreamCodec<ByteBuf, TemperatureView> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TemperatureView::target, TemperatureView::new);
}
