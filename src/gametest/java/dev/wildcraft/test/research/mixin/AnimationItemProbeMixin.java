package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.research.animation.SkinProbeLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandLayer.class)
public abstract class AnimationItemProbeMixin {
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void wildcraft$nativeGrip(ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci){
        if(!SkinProbeLayer.enabled||item.isEmpty()||!(state instanceof AvatarRenderState avatar))return;
        var matrix=SkinProbeLayer.heldPose(avatar,item,stack,arm);if(matrix==null)return;
        pose.pushPose();pose.mulPose(matrix);item.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();SkinProbeLayer.heldCalls++;ci.cancel();
    }
}
