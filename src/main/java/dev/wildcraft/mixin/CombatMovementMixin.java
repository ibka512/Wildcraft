package dev.wildcraft.mixin;

import dev.wildcraft.combat.Combat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keep native movement normalization, collision and aim; change only its reference yaw. */
@Mixin(Entity.class)
public abstract class CombatMovementMixin {
    @ModifyArg(method="moveRelative",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"),index=2)
    private float wildcraft$targetMovement(float yaw){return (Object)this instanceof Player p?Combat.facing(p,yaw):yaw;}
}
