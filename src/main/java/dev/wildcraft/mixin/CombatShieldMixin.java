package dev.wildcraft.mixin;

import dev.wildcraft.combat.Combat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(LivingEntity.class)
public abstract class CombatShieldMixin {
    @ModifyArg(method="applyItemBlocking",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"),index=1)
    private float wildcraft$shieldFacing(float yaw){return (Object)this instanceof Player p?Combat.facing(p,yaw):yaw;}
    @ModifyExpressionValue(method="jumpFromGround",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float wildcraft$jumpFacing(float yaw){return (Object)this instanceof Player p?Combat.facing(p,yaw):yaw;}
}
