package dev.wildcraft.network;
import dev.wildcraft.Wildcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** No client-supplied target, duration, damage or movement distance. */
public record MeleeIntent(byte operation,byte direction) implements CustomPacketPayload {
    public static final byte BEGIN=0,KEEP=1,RELEASE=2,CANCEL=3,DODGE=4;
    public static final Type<MeleeIntent> TYPE=new Type<>(Wildcraft.id("melee_intent"));
    public static final StreamCodec<ByteBuf,MeleeIntent> CODEC=StreamCodec.composite(ByteBufCodecs.BYTE,MeleeIntent::operation,ByteBufCodecs.BYTE,MeleeIntent::direction,MeleeIntent::new);
    public boolean valid(){return operation>=BEGIN&&operation<=DODGE&&(operation==DODGE?(direction==1||direction==-1||direction==-2):direction==0);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
