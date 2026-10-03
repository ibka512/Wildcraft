package dev.wildcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Presentation and bow duration only. Clients cannot request time control or report fees. */
public record FocusView(boolean active, long session, double bowSeconds) {
    public static final FocusView OFF = new FocusView(false, 0, 0);
    public static final StreamCodec<ByteBuf, FocusView> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FocusView::active,
            ByteBufCodecs.VAR_LONG, FocusView::session,
            ByteBufCodecs.DOUBLE, FocusView::bowSeconds, FocusView::new);
}
