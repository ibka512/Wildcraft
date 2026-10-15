package dev.wildcraft.mixin.client;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class CombatRigAvatarMixin {
    // Native/legacy extraction and equipment settles (default order 1000) must complete first.
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("RETURN"),order=1500)
    private void wildcraft$rig(Avatar entity,AvatarRenderState state,float partial,CallbackInfo ci){
        state.setData(CombatRigLayer.FRAME,null);if(entity instanceof Player p){CombatRigLayer.extract(p,state,partial);dev.wildcraft.client.animation.rig.DodgeFeedbackRig.extract(p,state);}
    }
}
