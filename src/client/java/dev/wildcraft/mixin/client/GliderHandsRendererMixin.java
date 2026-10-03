package dev.wildcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.render.GliderHands;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class GliderHandsRendererMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void wildcraft$grip(float partial, PoseStack poses, SubmitNodeCollector collector,
                               PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, CallbackInfo ci) {
        if (hands.getDataOrDefault(GliderHands.OPEN, false)) {
            if (player.avatarRenderState != null) GliderHands.submit(poses, collector, player.avatarRenderState);
            ci.cancel();
        }
    }
}
