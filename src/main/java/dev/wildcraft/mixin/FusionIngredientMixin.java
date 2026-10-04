package dev.wildcraft.mixin;
import dev.wildcraft.fuse.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Ingredient.class) public abstract class FusionIngredientMixin{
 @Inject(method="test(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true) private void wildcraft$noArrowIngredient(ItemStack s,CallbackInfoReturnable<Boolean> ci){if(FusionRules.arrow(s)&&FusionRepair.fused(s))ci.setReturnValue(false);}
}
