package dev.wildcraft.mixin.client;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The original body is replaced only when a qualified immutable skin frame exists. */
@Mixin(LivingEntityRenderer.class)
public abstract class CombatRigSkinMixin {
    @Inject(method="getRenderType",at=@At("HEAD"),cancellable=true)
    private void wildcraft$continuousSkin(LivingEntityRenderState state,boolean visible,boolean translucent,boolean outline,CallbackInfoReturnable<RenderType> ci){
        if(state instanceof AvatarRenderState avatar&&avatar.getData(CombatRigLayer.FRAME)!=null&&!state.isInvisible)ci.setReturnValue(null);
    }
}
