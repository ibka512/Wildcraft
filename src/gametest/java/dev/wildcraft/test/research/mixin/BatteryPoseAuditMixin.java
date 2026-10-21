package dev.wildcraft.test.research.mixin;
import dev.wildcraft.client.art.BatteryItemRenderer;
import dev.wildcraft.test.BatteryPoseAudit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BatteryItemRenderer.class)
public abstract class BatteryPoseAuditMixin {
    @Inject(method="submit(Ljava/lang/Integer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",at=@At("HEAD"))
    private void begin(Integer energy,PoseStack poses,SubmitNodeCollector c,int light,int overlay,boolean foil,int outline,CallbackInfo ci){if(BatteryPoseAudit.enabled){BatteryPoseAudit.inBattery=true;BatteryPoseAudit.parent=new org.joml.Matrix4f(poses.last().pose());}}
    @Inject(method="submit(Ljava/lang/Integer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",at=@At("RETURN"))
    private void end(Integer energy,PoseStack poses,SubmitNodeCollector c,int light,int overlay,boolean foil,int outline,CallbackInfo ci){BatteryPoseAudit.inBattery=false;}
}
