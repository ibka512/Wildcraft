package dev.wildcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.EquipmentHands;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class EquipmentHandsMixin {
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void wildcraft$return(PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,float swing,ItemStack stack,float height,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo ci) {
        if(EquipmentHands.holster(player.avatarRenderState,hands,hand)){
            EquipmentHands.empty(player.avatarRenderState,hands,hand,poses,collector);ci.cancel();
        }else if(EquipmentHands.stale(player.avatarRenderState,hands,hand,stack)){
            ci.cancel();
        }
    }
    @WrapOperation(method="submitArmWithItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void wildcraft$draw(ItemStackRenderState item,PoseStack poses,SubmitNodeCollector collector,int light,int overlay,int outline,Operation<Void> original,
            PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,float swing,ItemStack stack,float height,PoseStack localPoses,SubmitNodeCollector localCollector,int localLight) {
        if(!EquipmentHands.active(player.avatarRenderState,hands,hand,stack)){original.call(item,poses,collector,light,overlay,outline);return;}
        poses.pushPose();
        try {EquipmentHands.place(player.avatarRenderState,hands,hand,item,stack,poses,height);original.call(item,poses,collector,light,overlay,outline);EquipmentHands.submit(player.avatarRenderState,hands,hand,item,stack,poses,collector);}
        finally {poses.popPose();}
    }
}
