package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.OffShieldLandingAudit;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class OffShieldLandingAuditMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("RETURN"),order=1600)
    private void observe(Avatar p,AvatarRenderState state,float partial,CallbackInfo ci){OffShieldLandingAudit.observe(p,state);}
}
