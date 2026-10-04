package dev.wildcraft.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ProjectileWeaponItem.class)
public interface ProjectileWeaponItemAccess{
    @Invoker("useAmmo") static ItemStack wildcraft$useAmmo(ItemStack weapon,ItemStack projectile,LivingEntity holder,boolean virtual){throw new AssertionError();}
}
