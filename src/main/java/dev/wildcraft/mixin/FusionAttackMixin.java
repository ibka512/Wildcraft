package dev.wildcraft.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.wildcraft.fuse.FusionCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class FusionAttackMixin{
    @Unique private float wildcraft$fusionScale;
    @Inject(method="attack",at=@At("HEAD")) private void wildcraft$attackScale(net.minecraft.world.entity.Entity target,CallbackInfo ci){var p=(Player)(Object)this;float s=p.getAttackStrengthScale(.5f);wildcraft$fusionScale=.2f+.8f*s*s;}
    // Native critical, armor, enchantments, damage gating and durability remain downstream.
    @ModifyExpressionValue(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/Item;getAttackDamageBonus(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    private float wildcraft$fusionDamage(float value){var p=(Player)(Object)this;return p.level() instanceof ServerLevel level?value+FusionCombat.bonus(p.getWeaponItem(),level)*wildcraft$fusionScale:value;}
}
