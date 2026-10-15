package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.RigAudit;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class RigFixtureMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("RETURN"),order=1400)
    private void wildcraft$skin(Avatar entity,AvatarRenderState state,float partial,CallbackInfo ci){
        if(RigAudit.enabled&&RigAudit.bodyYaw!=null){state.bodyRot=RigAudit.bodyYaw;}
        if(RigAudit.enabled&&RigAudit.fixture!=null){state.skin=RigAudit.fixture;state.showLeftSleeve=state.showRightSleeve=RigAudit.sleeves;}
    }
}
