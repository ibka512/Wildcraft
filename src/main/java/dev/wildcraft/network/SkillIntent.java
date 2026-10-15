package dev.wildcraft.network;
import dev.wildcraft.Wildcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Preferences and a fresh confirmation only; no client supplied threat, timing or hit. */
public record SkillIntent(byte operation,byte bow,byte dodge) implements CustomPacketPayload {
    public static final byte PREFERENCES=0,CONFIRM=1,CANCEL=2;
    public static final Type<SkillIntent> TYPE=new Type<>(Wildcraft.id("skill_intent"));
    public static final StreamCodec<ByteBuf,SkillIntent> CODEC=StreamCodec.composite(ByteBufCodecs.BYTE,SkillIntent::operation,ByteBufCodecs.BYTE,SkillIntent::bow,ByteBufCodecs.BYTE,SkillIntent::dodge,SkillIntent::new);
    public boolean valid(){return operation>=0&&operation<=2&&bow>=0&&bow<=2&&dodge>=0&&dodge<=2;}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
