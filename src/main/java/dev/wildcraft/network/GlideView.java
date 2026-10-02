package dev.wildcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Observers see the canopy state; stamina remains private to its owner. */
public record GlideView(boolean active, boolean blocked) {
    public static final StreamCodec<ByteBuf, GlideView> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GlideView::active, ByteBufCodecs.BOOL, GlideView::blocked, GlideView::new);
}
