package dev.wildcraft.mechanics;

import dev.wildcraft.Wildcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Empty intent: server derives the actual vehicle and owner; no position or energy fields. */
public record MachineToggle() implements CustomPacketPayload {
    public static final Type<MachineToggle> TYPE = new Type<>(Wildcraft.id("machine_toggle"));
    public static final StreamCodec<FriendlyByteBuf, MachineToggle> CODEC = StreamCodec.unit(new MachineToggle());
    @Override public Type<MachineToggle> type() { return TYPE; }
}
