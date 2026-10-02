package dev.wildcraft.mixin;

import dev.wildcraft.equipment.BackEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BackUseMixin {
    @Inject(method = "startUsingItem", at = @At("TAIL"))
    private void wildcraft$used(InteractionHand hand, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer p && p.isUsingItem() && p.getUsedItemHand() == hand
                && BackEquipment.category(p.getUseItem()) >= BackEquipment.SHIELD) BackEquipment.register(p, p.getUseItem());
    }
}
