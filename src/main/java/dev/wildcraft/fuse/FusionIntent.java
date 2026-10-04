package dev.wildcraft.fuse;
import dev.wildcraft.Wildcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record FusionIntent(boolean split) implements CustomPacketPayload{
    public static final Type<FusionIntent> TYPE=new Type<>(Wildcraft.id("fusion_intent"));
    public static final StreamCodec<ByteBuf,FusionIntent> CODEC=ByteBufCodecs.BOOL.map(FusionIntent::new,FusionIntent::split);
    @Override public Type<FusionIntent> type(){return TYPE;}
}
