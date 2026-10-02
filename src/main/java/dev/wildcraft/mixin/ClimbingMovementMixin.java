package dev.wildcraft.mixin;

import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ClimbingMovementMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerPositionChange", at = @At("HEAD"), cancellable = true)
    private void wildcraft$checkClimbMovement(double x, double y, double z, float yaw, float pitch,
                                             boolean onGround, boolean horizontalCollision, CallbackInfo info) {
        if (!Climbing.acceptMovement(player, x, y, z) || !Gliding.acceptMovement(player, x, y, z)) {
            info.cancel();
        }
    }
}
