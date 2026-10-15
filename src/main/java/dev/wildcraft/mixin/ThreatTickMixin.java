package dev.wildcraft.mixin;
import dev.wildcraft.combat.ThreatActions;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class ThreatTickMixin {
    @Inject(method="tick",at=@At("RETURN")) private void wildcraft$phase(CallbackInfo ci){ThreatActions.tick((LivingEntity)(Object)this);}
}
