package dev.wildcraft.network;

import dev.wildcraft.Wildcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Input only: the server chooses targets and starts the actual owned shield. */
public record CombatIntent(byte operation, boolean defend, byte range, boolean neutral) implements CustomPacketPayload {
    public static final byte KEEP=0, SELECT=1, CLEAR=2, LEFT=3, RIGHT=4, TOGGLE=5;
    public static final Type<CombatIntent> TYPE=new Type<>(Wildcraft.id("combat_intent"));
    public static final StreamCodec<ByteBuf,CombatIntent> CODEC=StreamCodec.composite(
        ByteBufCodecs.BYTE,CombatIntent::operation,ByteBufCodecs.BOOL,CombatIntent::defend,
        ByteBufCodecs.BYTE,CombatIntent::range,ByteBufCodecs.BOOL,CombatIntent::neutral,CombatIntent::new);
    public boolean valid(){return operation>=KEEP&&operation<=TOGGLE&&range>=0&&range<=2;}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
