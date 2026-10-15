package dev.wildcraft.mixin.client;

import dev.wildcraft.client.focus.FocusClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class FocusFrameMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void wildcraft$focusFrame(CallbackInfo ci) {
        var client = (Minecraft)(Object)this;
        dev.wildcraft.client.input.ActionInput.frame(client);
        dev.wildcraft.client.combat.SkillControls.frame(client);
        FocusClient.frame(client);
        dev.wildcraft.client.combat.CombatPresentation.frame(client);
    }
}
