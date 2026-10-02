package dev.wildcraft.mixin;

import dev.wildcraft.traversal.Climbing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ClimbingTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void wildcraft$climbTravel(Vec3 input, CallbackInfo info) {
        if ((Object) this instanceof Player player && Climbing.movePredicted(player)) {
            info.cancel();
        }
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void wildcraft$authorizedClimb(CallbackInfoReturnable<Boolean> info) {
        if ((Object) this instanceof Player player && Climbing.active(player)) {
            info.setReturnValue(true);
        }
    }
}
