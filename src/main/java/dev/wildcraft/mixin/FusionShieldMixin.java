package dev.wildcraft.mixin;
import dev.wildcraft.fuse.FusionCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class FusionShieldMixin{
    @Unique private ItemStack wildcraft$shield;
    @Inject(method="applyItemBlocking",at=@At("HEAD")) private void wildcraft$captureShield(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Float> ci){wildcraft$shield=((LivingEntity)(Object)this).getItemBlockingWith();}
    @Inject(method="applyItemBlocking",at=@At("RETURN")) private void wildcraft$afterBlock(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Float> ci){var shield=wildcraft$shield;wildcraft$shield=null;if(shield!=null&&damage>0&&ci.getReturnValue()>=damage)FusionCombat.shield(shield,(LivingEntity)(Object)this,source);}
}
