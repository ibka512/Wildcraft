package dev.wildcraft.test.research.mixin;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.test.GuardRigAudit;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuardRig.class)
public abstract class GuardRigAuditMixin {
    @Inject(method="apply",at=@At("HEAD"))
    private static void before(Player p,AvatarRenderState s,CombatRig.Data d,Matrix4f[] global,float weight,CallbackInfo ci){GuardRigAudit.before(s,global);}
    @Inject(method="apply",at=@At("RETURN"))
    private static void after(Player p,AvatarRenderState s,CombatRig.Data d,Matrix4f[] global,float weight,CallbackInfo ci){GuardRigAudit.after(p,s,d,global);}
}
