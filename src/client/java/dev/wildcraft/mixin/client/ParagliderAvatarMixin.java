package dev.wildcraft.mixin.client;

import dev.wildcraft.client.render.ParagliderLayer;
import dev.wildcraft.traversal.Gliding;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class ParagliderAvatarMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void wildcraft$canopyState(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo info) {
        boolean open = entity instanceof Player player && Gliding.active(player);
        state.setData(ParagliderLayer.OPEN, open);
        if (entity instanceof Player player) dev.wildcraft.client.render.CharacterPoses.extract(player, state, partialTicks);
        if (open) {
            state.rightHandItemState.clear();
            state.leftHandItemState.clear();
            state.rightHandItemStack = ItemStack.EMPTY;
            state.leftHandItemStack = ItemStack.EMPTY;
        }
    }
}
