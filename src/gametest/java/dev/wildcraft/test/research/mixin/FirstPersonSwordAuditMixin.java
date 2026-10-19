package dev.wildcraft.test.research.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.rig.FirstPersonSword;
import dev.wildcraft.test.FirstPersonSwordAudit;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FirstPersonSword.class)
public abstract class FirstPersonSwordAuditMixin {
    @Inject(method="submit",at=@At("HEAD"))
    private static void base(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,InteractionHand hand,PoseStack poses,SubmitNodeCollector collector,int light,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){FirstPersonSwordAudit.parent=new org.joml.Matrix4f(poses.last().pose());}
    @Inject(method="submitArms",at=@At("HEAD"))
    private static void arms(FirstPersonSword.Frame f,AvatarRenderState a,PoseStack p,SubmitNodeCollector c,int light,CallbackInfo ci){FirstPersonSwordAudit.arms(f,a);}
    @WrapOperation(method="submitArms",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"))
    private static void skin(SubmitNodeCollector c,PoseStack p,RenderType type,SubmitNodeCollector.CustomGeometryRenderer renderer,Operation<Void> original){
        if(FirstPersonSwordAudit.enabled){
            var binding=((RenderSetupTexturesAccessor)(Object)((RenderTypeStateAccessor)(Object)type).wildcraft$setup()).wildcraft$textures().get("Sampler0");
            var texture=((TextureBindingLocationAccessor)binding).wildcraft$location();
            if(!texture.toString().equals(FirstPersonSwordAudit.texture))throw new AssertionError("Actual deferred first-person skin material differs from native avatar texture");
        }
        original.call(c,p,type,(SubmitNodeCollector.CustomGeometryRenderer)(pose,buffer)->{if(FirstPersonSwordAudit.enabled)FirstPersonSwordAudit.skinGpu++;renderer.render(pose,buffer);});
    }
    @WrapOperation(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private static void item(ItemStackRenderState item,PoseStack poses,SubmitNodeCollector c,int light,int overlay,int outline,Operation<Void> original,AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,InteractionHand hand,PoseStack parent,SubmitNodeCollector collector,int originalLight){
        var f=FirstPersonSword.frame(a,h);var base=new PoseStack();base.mulPose(FirstPersonSwordAudit.parent);
        FirstPersonSwordAudit.item(f,a,hand,base);
        try{original.call(item,poses,c,light,overlay,outline);}finally{FirstPersonSwordAudit.inSwordItem=false;FirstPersonSwordAudit.inShieldItem=false;}
    }
}
