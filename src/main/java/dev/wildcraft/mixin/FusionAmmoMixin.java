package dev.wildcraft.mixin;
import dev.wildcraft.fuse.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ProjectileWeaponItem.class)
public abstract class FusionAmmoMixin{
    @Inject(method="useAmmo",at=@At("HEAD"),cancellable=true) private static void wildcraft$realFuseAmmo(ItemStack weapon,ItemStack projectile,LivingEntity holder,boolean virtual,CallbackInfoReturnable<ItemStack> ci){
        if(holder.level().isClientSide()||!projectile.has(FusionContent.DATA))return;
        if(virtual){var normal=projectile.copy();normal.remove(FusionContent.DATA);normal.remove(FusionContent.VIEW);ci.setReturnValue(ProjectileWeaponItemAccess.wildcraft$useAmmo(weapon,normal,holder,true));}
        else{var used=projectile.split(1);if(projectile.isEmpty()&&holder instanceof Player p)p.getInventory().removeItem(projectile);ci.setReturnValue(used);}
    }
}
