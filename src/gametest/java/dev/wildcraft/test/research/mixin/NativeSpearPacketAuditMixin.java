package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.SpearAttackAudit;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class NativeSpearPacketAuditMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handlePlayerAction",at=@At("HEAD")) private void wildcraft$packet(ServerboundPlayerActionPacket packet,CallbackInfo ci){if(packet.getAction()==ServerboundPlayerActionPacket.Action.STAB&&player.getUUID().equals(SpearAttackAudit.player))SpearAttackAudit.nativePackets++;}
}
