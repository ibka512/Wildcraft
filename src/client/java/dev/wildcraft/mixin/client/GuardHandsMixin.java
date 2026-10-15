package dev.wildcraft.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.GuardPoses;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class GuardHandsMixin {
    @Unique private boolean wildcraft$guardPose;
    @Inject(method="submitArmWithItem",at=@At("HEAD"))
    private void wildcraft$pushShield(PlayerRenderState p,FirstPersonHandsAndItemsRenderState h,float partial,float pitch,InteractionHand hand,float swing,ItemStack item,float height,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo ci){
        var v=p.avatarRenderState==null?null:p.avatarRenderState.getData(GuardPoses.SHIELD);wildcraft$guardPose=item.is(Items.SHIELD)&&v!=null;
        if(wildcraft$guardPose){poses.pushPose();poses.translate(0,.025*v.pulse(),-.13*v.pulse());}
    }
    @Inject(method="submitArmWithItem",at=@At("RETURN"))
    private void wildcraft$popShield(PlayerRenderState p,FirstPersonHandsAndItemsRenderState h,float partial,float pitch,InteractionHand hand,float swing,ItemStack item,float height,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo ci){if(wildcraft$guardPose){poses.popPose();wildcraft$guardPose=false;}}
}
