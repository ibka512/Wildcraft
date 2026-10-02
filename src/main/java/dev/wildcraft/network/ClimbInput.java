package dev.wildcraft.network;

import dev.wildcraft.Wildcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Controls only; the server chooses the wall and owns stamina. */
public record ClimbInput(boolean held, byte vertical, byte sideways) implements CustomPacketPayload {
    public static final ClimbInput RELEASED = new ClimbInput(false, (byte) 0, (byte) 0);
    public static final Type<ClimbInput> TYPE = new Type<>(Wildcraft.id("climb_input"));
    public static final StreamCodec<ByteBuf, ClimbInput> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ClimbInput::held, ByteBufCodecs.BYTE, ClimbInput::vertical,
            ByteBufCodecs.BYTE, ClimbInput::sideways, ClimbInput::new);

    public boolean valid() {
        return vertical >= -1 && vertical <= 1 && sideways >= -1 && sideways <= 1;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
