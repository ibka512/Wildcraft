package dev.wildcraft.mixin;
import dev.wildcraft.combat.ThreatActions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MeleeAttackGoal.class)
public abstract class ThreatGoalMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow protected abstract boolean isTimeToAttack();
    @Shadow protected abstract void resetAttackCooldown();
    @Inject(method="checkAndPerformAttack",at=@At("HEAD"),cancellable=true)
    private void wildcraft$windup(LivingEntity target,CallbackInfo ci){
        if(!ThreatActions.supported(mob))return;
        if(ThreatActions.step(mob,target,isTimeToAttack())){resetAttackCooldown();mob.swingForAttack(InteractionHand.MAIN_HAND);mob.doHurtTarget((net.minecraft.server.level.ServerLevel)mob.level(),target);}
        ci.cancel();
    }
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void wildcraft$stagger(CallbackInfo ci){if(ThreatActions.supported(mob)&&ThreatActions.staggered(mob)){mob.getNavigation().stop();ci.cancel();}}
    @Inject(method="canContinueToUse",at=@At("RETURN"),cancellable=true)
    private void wildcraft$finishPhase(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){if(ThreatActions.supported(mob)&&ThreatActions.keepGoal(mob))ci.setReturnValue(true);}
    @Inject(method="stop",at=@At("TAIL"))
    private void wildcraft$cancel(CallbackInfo ci){if(ThreatActions.supported(mob))ThreatActions.cancel(mob);}
}
