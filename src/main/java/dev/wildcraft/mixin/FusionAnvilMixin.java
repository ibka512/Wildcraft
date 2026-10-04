package dev.wildcraft.mixin;
import dev.wildcraft.fuse.FusionRepair;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AnvilMenu.class) public abstract class FusionAnvilMixin{
 @Inject(method="createResult",at=@At("TAIL")) private void wildcraft$noSacrificedFusion(CallbackInfo ci){if(FusionRepair.fused(((AnvilMenu)(Object)this).getSlot(1).getItem()))((AnvilMenu)(Object)this).getSlot(2).set(net.minecraft.world.item.ItemStack.EMPTY);}
}
