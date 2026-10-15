package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.rig.GlideRig;
import dev.wildcraft.client.render.GliderHands;
import dev.wildcraft.test.GlideRigAudit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GliderHands.class)
public abstract class GlideRigHandsAuditMixin {
    @Inject(method="submit",at=@At("RETURN"))
    private static void wildcraft$view(PoseStack poses,SubmitNodeCollector collector,AvatarRenderState state,CallbackInfo ci){
        if(GlideRigAudit.enabled&&state.getData(GlideRig.CANOPY)!=null){GlideRigAudit.viewArms++;GlideRigAudit.viewCanopies++;GlideRigAudit.texture=state.skin.body().texturePath().toString();}
    }
}
