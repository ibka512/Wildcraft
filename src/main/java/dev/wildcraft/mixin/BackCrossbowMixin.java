package dev.wildcraft.mixin;

import dev.wildcraft.equipment.BackEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class BackCrossbowMixin {
    @Inject(method = "use", at = @At("RETURN"))
    private void wildcraft$shot(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        if (player instanceof ServerPlayer p && ci.getReturnValue().consumesAction()) BackEquipment.register(p, p.getItemInHand(hand));
    }
}
