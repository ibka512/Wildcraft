package dev.wildcraft.test.research.mixin;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerCapeModel.class)
public abstract class SwordWaistCapeAuditMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("RETURN"),order=1500)
    private void pose(AvatarRenderState state,CallbackInfo ci){dev.wildcraft.test.SwordWaistAudit.capePose((PlayerCapeModel)(Object)this,state);}
}
