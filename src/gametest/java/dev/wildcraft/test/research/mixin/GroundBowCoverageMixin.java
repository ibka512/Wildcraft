package dev.wildcraft.test.research.mixin;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import dev.wildcraft.test.GroundBowAudit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Detect a missing body too, rather than observing only selected/GPU-successful frames. */
@Mixin(CombatRigLayer.class)
public abstract class GroundBowCoverageMixin {
    @Inject(method="extract",at=@At("RETURN"))
    private static void coverage(Player p,AvatarRenderState state,float partial,CallbackInfo ci){GroundBowAudit.coverage(p,state);dev.wildcraft.test.AirBowAudit.coverage(p,state);}
}
