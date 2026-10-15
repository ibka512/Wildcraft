package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.SpearAttackAudit;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Player.class)
public abstract class NativeStabAuditMixin {
    @Inject(method="stabAttack",at=@At("HEAD")) private void wildcraft$target(EquipmentSlot slot,Entity target,float base,boolean damage,boolean knockback,boolean dismount,CallbackInfoReturnable<Boolean> ci){SpearAttackAudit.observe((Player)(Object)this,false);}
}
