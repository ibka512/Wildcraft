package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.research.animation.SkinProbeLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** This test mixin has no effect outside explicitly enabled animation qualification. */
@Mixin(LivingEntityRenderer.class)
public abstract class AnimationSkinProbeMixin {
    @Inject(method="getRenderType",at=@At("HEAD"),cancellable=true)
    private void wildcraft$skinProbe(LivingEntityRenderState state,boolean visible,boolean translucent,boolean outline,CallbackInfoReturnable<RenderType> ci){
        if(SkinProbeLayer.enabled&&state instanceof AvatarRenderState&&!state.isInvisible)ci.setReturnValue(null);
    }
}
