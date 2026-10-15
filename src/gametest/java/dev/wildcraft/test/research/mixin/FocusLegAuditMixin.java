package dev.wildcraft.test.research.mixin;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.test.*;
import org.joml.Matrix4f;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FocusLegRig.class)
public abstract class FocusLegAuditMixin {
    @Inject(method="apply(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Ldev/wildcraft/client/animation/rig/CombatRig$Data;[Lorg/joml/Matrix4f;)V",at=@At("HEAD"))
    private static void before(Player p,AvatarRenderState s,CombatRig.Data d,Matrix4f[] g,CallbackInfo ci){if(FocusLegAudit.enabled||AirBowAudit.enabled)FocusLegAudit.capture(g);}
    @Inject(method="apply(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Ldev/wildcraft/client/animation/rig/CombatRig$Data;[Lorg/joml/Matrix4f;)V",at=@At("RETURN"))
    private static void after(Player p,AvatarRenderState s,CombatRig.Data d,Matrix4f[] g,CallbackInfo ci){if(FocusLegAudit.enabled||AirBowAudit.enabled)FocusLegAudit.check(d,g,s);}
}
