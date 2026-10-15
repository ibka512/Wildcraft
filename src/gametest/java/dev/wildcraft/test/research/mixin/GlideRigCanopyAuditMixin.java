package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.rig.GlideRig;
import dev.wildcraft.client.render.ParagliderLayer;
import dev.wildcraft.test.GlideRigAudit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParagliderLayer.class)
public abstract class GlideRigCanopyAuditMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",at=@At("RETURN"))
    private void wildcraft$world(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch,CallbackInfo ci){
        if(GlideRigAudit.enabled&&state.getData(GlideRig.CANOPY)!=null){GlideRigAudit.worldCanopies++;}
    }
}
