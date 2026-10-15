package dev.wildcraft.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.wildcraft.combat.MeleeActions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Only a scoped server-owned native attack gains the paid charged modifier. */
@Mixin(Player.class)
public abstract class MeleeDamageMixin {
    @ModifyExpressionValue(method={"attack","stabAttack"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;baseDamageScaleFactor()F"))
    private float wildcraft$chargedDamage(float value){return value*MeleeActions.damageScale((Player)(Object)this);}
    @ModifyExpressionValue(method={"attack","stabAttack"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getKnockback(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F"))
    private float wildcraft$chargedKnockback(float value){return value+MeleeActions.knockback((Player)(Object)this);}
}
