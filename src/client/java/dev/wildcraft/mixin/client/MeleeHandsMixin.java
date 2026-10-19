package dev.wildcraft.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildcraft.client.animation.MeleeHands;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Native weapon transform and skin grip; item submission and gameplay remain native. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MeleeHandsMixin {
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true,order=900)
    private void wildcraft$adoptedSword(PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,float swing,ItemStack stack,float height,PoseStack poses,SubmitNodeCollector collector,int light,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
        if(dev.wildcraft.client.animation.rig.FirstPersonSword.submit(player.avatarRenderState,hands,hand,poses,collector,light))ci.cancel();
    }

    @WrapOperation(method="submitArmWithItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void wildcraft$skinGrip(ItemStackRenderState item,PoseStack poses,SubmitNodeCollector collector,int light,int overlay,int outline,Operation<Void> original,
            PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,float swing,ItemStack stack,float height,PoseStack localPoses,SubmitNodeCollector localCollector,int localLight){
        if(!MeleeHands.active(player.avatarRenderState,hands,hand,stack)){original.call(item,poses,collector,light,overlay,outline);return;}
        poses.pushPose();
        try {MeleeHands.place(player.avatarRenderState,hands,poses,partial);original.call(item,poses,collector,light,overlay,outline);MeleeHands.submit(player.avatarRenderState,hands,poses,collector);}
        finally {poses.popPose();}
    }

}
