package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.CounterAttackAudit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class NativeCounterAuditMixin {
    @Inject(method="attack",at=@At("HEAD"))
    private void wildcraft$countNativeCounter(Entity target,CallbackInfo ci){CounterAttackAudit.observed((Player)(Object)this);}
}
