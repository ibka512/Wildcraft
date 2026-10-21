package dev.wildcraft.test.industry.mixin;
import dev.wildcraft.test.industry.NativeGroundAudit;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observe only the original packet path on the server thread, including native early returns. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class NativeGroundMovementMixin {
 @Shadow public ServerPlayer player;
 @Inject(method="handleMovePlayer",at=@At("HEAD")) private void wildcraft$beforeNativeMove(ServerboundMovePlayerPacket packet,CallbackInfo ci){NativeGroundAudit.packet(player,packet,"packet-head");}
 @Inject(method="handleMovePlayer",at=@At("RETURN")) private void wildcraft$afterNativeMove(ServerboundMovePlayerPacket packet,CallbackInfo ci){NativeGroundAudit.packet(player,packet,"packet-return");}
}
