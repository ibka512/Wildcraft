package dev.wildcraft.mixin.client;

import dev.wildcraft.client.focus.FocusClient;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class FocusCameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void wildcraft$focusFov(float partial, CallbackInfoReturnable<Float> ci) { ci.setReturnValue(ci.getReturnValue() * FocusClient.fovMultiplier()); }
}
