package dev.wildcraft.mixin.client;

import dev.wildcraft.focus.FocusPauseSource;
import dev.wildcraft.focus.FocusTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
public abstract class IntegratedFocusMixin implements FocusPauseSource {
    @Override public boolean wildcraft$focusPaused() { return Minecraft.getInstance().isPaused(); }
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void wildcraft$focusPause(CallbackInfo ci) {
        if (wildcraft$focusPaused()) FocusTime.close((MinecraftServer)(Object)this, false, false);
    }
    @Inject(method = "publishServer(Lnet/minecraft/server/MinecraftServer$MultiplayerScope;I)Z", at = @At("HEAD"))
    private void wildcraft$focusLan(CallbackInfoReturnable<Boolean> ci) {
        FocusTime.close((MinecraftServer)(Object)this, true, true);
    }
}
