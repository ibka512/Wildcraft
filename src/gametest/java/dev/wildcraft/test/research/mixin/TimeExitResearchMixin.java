package dev.wildcraft.test.research.mixin;

import dev.wildcraft.test.research.time.WorldTimeResearch;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class TimeExitResearchMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"))
    private void wildcraftResearch$release(CallbackInfo info) { WorldTimeResearch.beforeRelease((LivingEntity) (Object) this); }

    @Inject(method = "travel", at = @At("HEAD"))
    private void wildcraftResearch$descent(CallbackInfo info) {
        if ((Object) this instanceof Player p && WorldTimeResearch.active(p) && p.getDeltaMovement().y < -0.15) {
            var v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, -0.15, v.z);
        }
    }
}
