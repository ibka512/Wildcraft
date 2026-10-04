package dev.wildcraft.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.wildcraft.fuse.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractArrow.class)
public abstract class FusionArrowMixin{
    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean wildcraft$oneImpact(Entity target,DamageSource source,float damage,Operation<Boolean> original){
        var arrow=(AbstractArrow)(Object)this;if(!(arrow.level() instanceof ServerLevel level))return original.call(target,source,damage);
        var m=FusionCombat.material(arrow.getPickupItemStackOrigin(),level);if(m.isEmpty())return original.call(target,source,damage);
        int kind=FusionRules.kind(m.get());FusionCombat.clearArrow(arrow);boolean hit=original.call(target,source,damage+FusionRules.bonus(kind));if(hit&&target instanceof LivingEntity living)FusionCombat.effect(kind,living,source);return hit;
    }
    @Inject(method="onHitBlock",at=@At("HEAD")) private void wildcraft$spentAtWall(BlockHitResult hit,CallbackInfo ci){var arrow=(AbstractArrow)(Object)this;if(arrow.level() instanceof ServerLevel level&&FusionCombat.material(arrow.getPickupItemStackOrigin(),level).isPresent())FusionCombat.clearArrow(arrow);}
}
