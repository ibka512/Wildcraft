package dev.wildcraft.test.research.mixin;

import dev.wildcraft.test.research.time.NativeTimeProbe;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class NativeFrameResearchMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void wildcraftResearch$release(CallbackInfo info) {
        Minecraft client = (Minecraft) (Object) this;
        if (NativeTimeProbe.holdingUse() || dev.wildcraft.test.equipment.BackNativeClientProbe.holdingUse()) client.options.keyUse.setDown(true);
        if (Boolean.getBoolean("wildcraft.r1.frameRelease") && client.player != null && client.gameMode != null
                && dev.wildcraft.test.research.time.WorldTimeResearch.active(client.player)
                && dev.wildcraft.test.research.time.WorldTimeResearch.compensated
                && client.player.isUsingItem() && client.player.getUseItem().getItem() instanceof net.minecraft.world.item.BowItem
                && !client.options.keyUse.isDown() && client.gui.screen() == null && !client.isPaused()) {
            // Invoke the existing release path once. Do not tick the player or use the item twice.
            client.gameMode.releaseUsingItem(client.player);
        }
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void wildcraftResearch$heldInput(CallbackInfo info) {
        if (NativeTimeProbe.holdingUse() || dev.wildcraft.test.equipment.BackNativeClientProbe.holdingUse()) ((Minecraft) (Object) this).options.keyUse.setDown(true);
    }
    @Inject(method = "runTick", at = @At("TAIL"))
    private void wildcraftResearch$frame(CallbackInfo info) { NativeTimeProbe.frame((Minecraft) (Object) this); dev.wildcraft.test.equipment.BackNativeClientProbe.frame((Minecraft) (Object)this); }
}
