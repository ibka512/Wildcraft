package dev.wildcraft.mixin.client;

import dev.wildcraft.client.focus.FocusClient;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class FocusPostMixin {
    @Inject(method = "getActivePostEffects", at = @At("RETURN"), cancellable = true)
    private void wildcraft$focusPost(CallbackInfoReturnable<List<Identifier>> ci) { ci.setReturnValue(FocusClient.postEffects(ci.getReturnValue())); }
}
