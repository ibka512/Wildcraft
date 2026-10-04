package dev.wildcraft.mixin;
import dev.wildcraft.fuse.FusionRepair;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GrindstoneMenu.class) public abstract class FusionGrindstoneMixin{
 @Inject(method="computeResult",at=@At("HEAD"),cancellable=true) private void wildcraft$refuseTwo(ItemStack a,ItemStack b,CallbackInfoReturnable<ItemStack> ci){if(FusionRepair.two(a,b))ci.setReturnValue(ItemStack.EMPTY);}
 @Inject(method="computeResult",at=@At("RETURN")) private void wildcraft$keepOne(ItemStack a,ItemStack b,CallbackInfoReturnable<ItemStack> ci){FusionRepair.keep(ci.getReturnValue(),a,b);}
}
