package dev.wildcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Public appearance only; no inputs, grip locks, inventory or stamina. Not persisted. */
public record ClimbVisual(boolean active, int face, int motion) {
    public static final ClimbVisual IDLE = new ClimbVisual(false, 2, 0);
    public static final StreamCodec<ByteBuf, ClimbVisual> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ClimbVisual::active, ByteBufCodecs.VAR_INT, ClimbVisual::face,
            ByteBufCodecs.VAR_INT, ClimbVisual::motion, ClimbVisual::new);
}
