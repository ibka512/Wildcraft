package dev.wildcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.render.BackTransitions;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandLayer.class)
public abstract class BackHandSettleMixin {
    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void wildcraft$settle(ItemStackRenderState item, PoseStack poses, SubmitNodeCollector collector, int light, int overlay, int outline,
            Operation<Void> original, ArmedEntityRenderState state, ItemStackRenderState renderItem, ItemStack stack, HumanoidArm arm,
            PoseStack localPoses, SubmitNodeCollector localCollector, int localLight) {
        poses.pushPose();
        if (state instanceof AvatarRenderState avatar) BackTransitions.hand(poses,avatar,arm);
        original.call(item,poses,collector,light,overlay,outline);
        poses.popPose();
    }
}
