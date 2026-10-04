package dev.wildcraft.mixin.client;
import dev.wildcraft.client.fuse.FusionArrowRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ArrowRenderer.class) public abstract class FusionArrowRenderMixin{
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;Lnet/minecraft/client/renderer/entity/state/ArrowRenderState;F)V",at=@At("TAIL")) private void wildcraft$arrowExtract(AbstractArrow arrow,ArrowRenderState state,float partial,CallbackInfo ci){FusionArrowRenderer.extract(arrow,state);}
 @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/ArrowRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("TAIL")) private void wildcraft$arrowSubmit(ArrowRenderState state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci){FusionArrowRenderer.submit(state,poses,collector);}
}
