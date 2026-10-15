package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.render.ParagliderMesh;
import dev.wildcraft.test.GlideRigAudit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParagliderMesh.class)
public abstract class GlideRigMeshPoseAuditMixin {
    @Inject(method="submit",at=@At("HEAD"))
    private static void wildcraft$pose(PoseStack poses,SubmitNodeCollector collector,int light,boolean firstPerson,CallbackInfo ci){
        if(GlideRigAudit.enabled&&firstPerson)GlideRigAudit.canopyPose=new org.joml.Matrix4f(poses.last().pose());
    }
}
