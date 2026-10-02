package dev.wildcraft.test.research.mixin;

import dev.wildcraft.test.research.time.WorldTimeResearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class IntegratedTimeResearchMixin {
    @Inject(method = "publishServer(Lnet/minecraft/server/MinecraftServer$MultiplayerScope;I)Z", at = @At("HEAD"))
    private void wildcraftResearch$lan(CallbackInfoReturnable<Boolean> info) { WorldTimeResearch.close((MinecraftServer) (Object) this); }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void wildcraftResearch$pause(CallbackInfo info) {
        if (Minecraft.getInstance().isPaused()) WorldTimeResearch.close((MinecraftServer) (Object) this);
    }
}
