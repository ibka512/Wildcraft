package dev.wildcraft.test.research.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.rig.FirstPersonHeavy;
import dev.wildcraft.test.FirstPersonSwordAudit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(FirstPersonHeavy.class)
public abstract class FirstPersonHeavyAuditMixin {
    @Inject(method="submit",at=@At("HEAD"))
    private static void base(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,InteractionHand hand,PoseStack poses,SubmitNodeCollector collector,int light,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){FirstPersonSwordAudit.parent=new org.joml.Matrix4f(poses.last().pose());}
    @WrapOperation(method="submit",at=@At(value="INVOKE",target="Ldev/wildcraft/client/animation/rig/FirstPersonSword;submitArms(Ldev/wildcraft/client/animation/rig/FirstPersonSword$Frame;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private static void grips(dev.wildcraft.client.animation.rig.FirstPersonSword.Frame f,AvatarRenderState a,PoseStack p,SubmitNodeCollector c,int light,Operation<Void> original,AvatarRenderState actor,FirstPersonHandsAndItemsRenderState h,InteractionHand hand,PoseStack parent,SubmitNodeCollector collector,int originalLight){
        var v=a.getData(dev.wildcraft.client.animation.MeleePoses.LAYER);
        dev.wildcraft.test.GripAudit.observe(false,v.kind(),v.phase());
        if(h.offHandItem.isEmpty()&&dev.wildcraft.client.animation.MeleeHands.support(v))dev.wildcraft.test.GripAudit.observe(true,v.kind(),v.phase());
        original.call(f,a,p,c,light);
    }
    @WrapOperation(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private static void item(ItemStackRenderState item,PoseStack poses,SubmitNodeCollector c,int light,int overlay,int outline,Operation<Void> original,AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,InteractionHand hand,PoseStack parent,SubmitNodeCollector collector,int originalLight){
        var f=FirstPersonHeavy.frame(a,h).pose();var base=new PoseStack();base.mulPose(FirstPersonSwordAudit.parent);
        FirstPersonSwordAudit.item(f,a,hand,base);
        try{original.call(item,poses,c,light,overlay,outline);}finally{FirstPersonSwordAudit.inSwordItem=false;FirstPersonSwordAudit.inShieldItem=false;}
    }
}
