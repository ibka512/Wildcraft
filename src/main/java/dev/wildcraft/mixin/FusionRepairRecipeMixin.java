package dev.wildcraft.mixin;
import dev.wildcraft.fuse.FusionRepair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RepairItemRecipe.class) public abstract class FusionRepairRecipeMixin{
 @Inject(method="matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",at=@At("HEAD"),cancellable=true) private void wildcraft$noTwo(CraftingInput input,Level level,CallbackInfoReturnable<Boolean> ci){if(input.items().stream().filter(FusionRepair::fused).count()>1)ci.setReturnValue(false);}
 @Inject(method="assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"),cancellable=true) private void wildcraft$assembleNoTwo(CraftingInput input,CallbackInfoReturnable<ItemStack> ci){if(input.items().stream().filter(FusionRepair::fused).count()>1)ci.setReturnValue(ItemStack.EMPTY);}
 @Inject(method="assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",at=@At("RETURN")) private void wildcraft$keepMaterial(CraftingInput input,CallbackInfoReturnable<ItemStack> ci){if(!ci.getReturnValue().isEmpty()){for(var item:input.items())if(FusionRepair.fused(item)){FusionRepair.keep(ci.getReturnValue(),item,ItemStack.EMPTY);break;}}}
}
