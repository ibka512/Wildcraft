package dev.wildcraft.mixin.client;
import dev.wildcraft.client.combat.MeleeControls;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Vanilla still consumes its own queues; only a claimed concrete weapon gesture is suppressed. */
@Mixin(Minecraft.class)
public abstract class MeleeInputMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void wildcraft$ownedAttack(CallbackInfoReturnable<Boolean> ci){if((MeleeControls.suppressNative()||dev.wildcraft.client.combat.SkillControls.suppressNative()))ci.setReturnValue(false);}
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void wildcraft$ownedHold(boolean held,CallbackInfo ci){if((MeleeControls.suppressNative()||dev.wildcraft.client.combat.SkillControls.suppressNative()))ci.cancel();}
}
