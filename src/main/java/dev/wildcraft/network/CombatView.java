package dev.wildcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Transient authoritative presentation; no combat inventory or saved lock. */
public record CombatView(int targetId, boolean defending) {
    public static final CombatView IDLE=new CombatView(-1,false);
    public static final StreamCodec<ByteBuf,CombatView> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,CombatView::targetId,ByteBufCodecs.BOOL,CombatView::defending,CombatView::new);
}
