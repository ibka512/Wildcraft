package dev.wildcraft.mixin;

import dev.wildcraft.equipment.BackEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class BackItemUseMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void wildcraft$axe(UseOnContext context, CallbackInfoReturnable<InteractionResult> ci) {
        ItemStack stack = (ItemStack)(Object)this;
        if (context.getPlayer() instanceof ServerPlayer p && stack.is(ItemTags.AXES) && ci.getReturnValue().consumesAction()) BackEquipment.register(p, stack);
    }
}
