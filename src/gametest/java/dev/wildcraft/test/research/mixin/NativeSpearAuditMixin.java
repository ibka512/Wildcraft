package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.SpearAttackAudit;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.PiercingWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(PiercingWeapon.class)
public abstract class NativeSpearAuditMixin {
    @Inject(method="attack",at=@At("HEAD")) private void wildcraft$primary(LivingEntity attacker,EquipmentSlot slot,CallbackInfo ci){if(attacker instanceof Player p)SpearAttackAudit.observe(p,true);}
}
