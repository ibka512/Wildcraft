package dev.wildcraft.mixin.client;

import dev.wildcraft.client.render.BackTransitions;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** End visual take/stow at the accepted native event, including a use with no sustained using-item state. */
@Mixin(MultiPlayerGameMode.class)
public abstract class BackNativeUseMixin {
    @Inject(method="useItem",at=@At("RETURN"))
    private void wildcraft$nativeUse(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){
        if(ci.getReturnValue().consumesAction())BackTransitions.nativeUse(player);
    }
}
