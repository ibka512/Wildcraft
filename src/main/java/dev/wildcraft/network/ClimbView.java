package dev.wildcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A blocked grip must be released before trying again. */
public record ClimbView(boolean active, boolean blocked) {
    public static final ClimbView IDLE = new ClimbView(false, false);
    public static final StreamCodec<ByteBuf, ClimbView> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ClimbView::active, ByteBufCodecs.BOOL, ClimbView::blocked, ClimbView::new);
}
