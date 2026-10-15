package dev.wildcraft.test.research.mixin;
import dev.wildcraft.client.animation.rig.DodgeFeedbackRig;
import dev.wildcraft.test.DodgeFeedbackAudit;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(DodgeFeedbackRig.class)
public abstract class DodgeFeedbackAuditMixin {
    @Inject(method="extract",at=@At("HEAD"))private static void reset(Player p,AvatarRenderState s,CallbackInfo ci){DodgeFeedbackAudit.before(s);}
    @Inject(method="apply",at=@At("HEAD"))private static void before(AvatarRenderState s,dev.wildcraft.client.animation.rig.CombatRigLayer.Frame f,String variant,String name,float time,int serial,CallbackInfo ci){DodgeFeedbackAudit.before(s);}
    @Inject(method="extract",at=@At("RETURN"))private static void after(Player p,AvatarRenderState s,CallbackInfo ci){DodgeFeedbackAudit.after(s);}
}
