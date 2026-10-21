package dev.wildcraft.test.research.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.art.ArtMesh;
import dev.wildcraft.test.BatteryPoseAudit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ArtMesh.class)
public abstract class BatteryPoseGeometryMixin {
    @WrapOperation(method="geometry",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"))
    private static void geometry(SubmitNodeCollector c,PoseStack p,RenderType type,SubmitNodeCollector.CustomGeometryRenderer draw,Operation<Void> original){
        if(!BatteryPoseAudit.enabled||!BatteryPoseAudit.inBattery){original.call(c,p,type,draw);return;}
        var expected=new org.joml.Matrix4f(BatteryPoseAudit.parent);
        original.call(c,p,type,(SubmitNodeCollector.CustomGeometryRenderer)(pose,buffer)->{BatteryPoseAudit.geometry(pose.pose(),expected);draw.render(pose,buffer);});
    }
}
