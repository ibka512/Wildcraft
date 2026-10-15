package dev.wildcraft.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.wildcraft.combat.ParryActions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LivingEntity.class)
public abstract class ParryShieldMixin {
    @ModifyExpressionValue(method="applyItemBlocking",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getItemBlockingWith()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack wildcraft$freshShield(ItemStack nativeShield,ServerLevel level,DamageSource source,float damage){return ParryActions.blockingItem((LivingEntity)(Object)this,source,damage,nativeShield);}
}
