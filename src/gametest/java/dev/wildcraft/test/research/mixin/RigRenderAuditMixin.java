package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.RigAudit;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CombatRigLayer.class)
public abstract class RigRenderAuditMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",at=@At("RETURN"))
    private void wildcraft$audit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch,CallbackInfo ci){
        dev.wildcraft.test.JumpAudit.observe(state);dev.wildcraft.test.AirMeleeAudit.observe(state);
        dev.wildcraft.test.SwordTransitionAudit.observe(state);
        dev.wildcraft.test.HurtAudit.gpu(state);
        dev.wildcraft.test.BowTransitionAudit.observe(state);
        dev.wildcraft.test.ShieldTransitionAudit.observe(state);
        dev.wildcraft.test.AxeTransitionAudit.observe(state);
        dev.wildcraft.test.TridentTransitionAudit.observe(state);
        dev.wildcraft.test.SpearTransitionAudit.observe(state);dev.wildcraft.test.MaceTransitionAudit.observe(state);dev.wildcraft.test.CrossbowTransitionAudit.observe(state);dev.wildcraft.test.GroundTridentAudit.observe(state);dev.wildcraft.test.GroundSpearAudit.observe(state);dev.wildcraft.test.GroundMaceAudit.observe(state);dev.wildcraft.test.AirTridentAudit.observe(state);dev.wildcraft.test.AirSpearAudit.observe(state);dev.wildcraft.test.AirMaceAudit.observe(state);
        if(RigAudit.enabled&&state.getData(CombatRigLayer.FRAME)!=null&&!state.isInvisible&&!state.isSpectator){dev.wildcraft.test.GroundBowAudit.observe(state);dev.wildcraft.test.AirBowAudit.observe(state);dev.wildcraft.test.FocusLegAudit.gpu(state);RigAudit.bodies++;RigAudit.last=state.getData(CombatRigLayer.FRAME);RigAudit.texture=state.skin.body().texturePath().toString();if(dev.wildcraft.test.DodgeFeedbackAudit.enabled&&RigAudit.last.clip().equals("dodge_native_base"))dev.wildcraft.test.DodgeFeedbackAudit.nativeGpuFrames++;}
    }
}
