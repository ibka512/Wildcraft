package dev.wildcraft.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.wildcraft.combat.ParryActions;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractArrow.class)
public abstract class ParryArrowMixin {
    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;deflect(Lnet/minecraft/world/entity/projectile/ProjectileDeflection;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityReference;ZD)Z"))
    private boolean wildcraft$reflect(AbstractArrow a,ProjectileDeflection nativeDeflection,Entity defender,EntityReference<Entity> owner,boolean byAttack,double power,Operation<Boolean> original){
        var reflected=ParryActions.takeReflection(a);if(reflected==null)return original.call(a,nativeDeflection,defender,owner,byAttack,power);
        return original.call(a,(ProjectileDeflection)(projectile,e,random,v)->{projectile.setDeltaMovement(reflected.velocity());projectile.needsSync=true;},reflected.defender(),EntityReference.of(reflected.defender()),false,1D);
    }
    @Inject(method="onHitEntity",at=@At("RETURN")) private void wildcraft$clear(EntityHitResult hit,CallbackInfo ci){ParryActions.clearReflection((AbstractArrow)(Object)this);}
}
