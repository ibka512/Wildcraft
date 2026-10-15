package dev.wildcraft.mixin.client;

import dev.wildcraft.client.animation.CombatPoses;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native armor animates separately; apply the same mask without applying it twice to a player. */
@Mixin(HumanoidModel.class)
public abstract class CombatArmorPoseMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("TAIL"))
    private void wildcraft$combatArmor(HumanoidRenderState state,CallbackInfo info){
        if(state instanceof AvatarRenderState avatar&&!((Object)this instanceof PlayerModel)){
            CombatPoses.apply((HumanoidModel<?>)(Object)this,avatar);
            dev.wildcraft.client.animation.MeleePoses.apply((HumanoidModel<?>)(Object)this,avatar);
            dev.wildcraft.client.animation.GuardPoses.applyShield((HumanoidModel<?>)(Object)this,avatar);
            dev.wildcraft.client.render.BackTransitions.apply((HumanoidModel<?>)(Object)this,avatar);
        }
        if(state instanceof net.minecraft.client.renderer.entity.state.ZombieRenderState zombie)dev.wildcraft.client.animation.GuardPoses.applyThreat((HumanoidModel<?>)(Object)this,zombie);
    }
}
