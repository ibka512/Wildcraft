package dev.wildcraft.test.research.mixin;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.test.HurtAudit;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HurtRig.class)
public abstract class HurtRigAuditMixin {
    @Inject(method="apply",at=@At("HEAD"))
    private static void wildcraft$before(CombatRig.Data data,Matrix4f[] global,Matrix4f world,CallbackInfo ci){HurtAudit.before(data,global);}
    @Inject(method="apply",at=@At("RETURN"))
    private static void wildcraft$after(CombatRig.Data data,Matrix4f[] global,Matrix4f world,CallbackInfo ci){HurtAudit.after(data,global,world);}
}
