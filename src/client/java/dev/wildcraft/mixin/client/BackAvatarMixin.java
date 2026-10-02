package dev.wildcraft.mixin.client;

import dev.wildcraft.client.render.BackEquipmentLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class BackAvatarMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void wildcraft$back(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (dev.wildcraft.equipment.BackEquipment.holstersShield(player)) {
                if (state.rightHandItemStack.is(net.minecraft.world.item.Items.SHIELD)) {
                    state.rightHandItemStack = net.minecraft.world.item.ItemStack.EMPTY; state.rightHandItemState.clear();
                }
                if (state.leftHandItemStack.is(net.minecraft.world.item.Items.SHIELD)) {
                    state.leftHandItemStack = net.minecraft.world.item.ItemStack.EMPTY; state.leftHandItemState.clear();
                }
            }
            BackEquipmentLayer.extract(player, state);
        }
    }
}
