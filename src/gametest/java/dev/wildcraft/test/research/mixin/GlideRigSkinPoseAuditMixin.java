package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import dev.wildcraft.test.GlideRigAudit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CombatRigLayer.class)
public abstract class GlideRigSkinPoseAuditMixin {
    @Inject(method="submitSkin",at=@At("HEAD"))
    private static void wildcraft$pose(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,CombatRigLayer.Frame frame,boolean armsOnly,CallbackInfo ci){
        if(GlideRigAudit.enabled&&armsOnly){GlideRigAudit.viewPose=new org.joml.Matrix4f(poses.last().pose()).mul(CombatRigLayer.BASIS);GlideRigAudit.viewFrame=frame;}
    }
}
