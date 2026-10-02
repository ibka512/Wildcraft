package dev.wildcraft.network;

import dev.wildcraft.Wildcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Open/close intent only; inventory, stamina and permission are server-owned. */
public record GlideInput(boolean open) implements CustomPacketPayload {
    public static final GlideInput CLOSED = new GlideInput(false);
    public static final GlideInput OPEN = new GlideInput(true);
    public static final Type<GlideInput> TYPE = new Type<>(Wildcraft.id("glide_input"));
    public static final StreamCodec<ByteBuf, GlideInput> CODEC = ByteBufCodecs.BOOL.map(GlideInput::new, GlideInput::open);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
