package dev.wildcraft.mixin.client;

import dev.wildcraft.traversal.Gliding;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class ParagliderHandsMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void wildcraft$holster(LocalPlayer player, float partialTicks, FirstPersonHandsAndItemsRenderState state, CallbackInfo info) {
        if (Gliding.active(player)) {
            state.mainHandItem = state.offHandItem = ItemStack.EMPTY;
            state.mainHandRenderState.clear();
            state.offHandRenderState.clear();
            state.handRenderSelection = null;
        }
    }
}
