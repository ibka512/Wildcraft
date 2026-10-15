package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.RigAudit;
import dev.wildcraft.client.animation.rig.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CombatRig.class)
public abstract class RigGripAuditMixin {
    @Inject(method="heldPose",at=@At("RETURN"))
    private static void wildcraft$grip(CombatRigLayer.Frame frame,ItemStackRenderState item,HumanoidArm arm,boolean shield,CallbackInfoReturnable<Matrix4f> ci){
        if(RigAudit.enabled&&!dev.wildcraft.test.DodgeFeedbackAudit.checkingNativeItem){if(!ci.getReturnValue().isFinite())throw new AssertionError("Nonfinite native grip");RigAudit.hands++;if(shield)RigAudit.shields++;}
    }
}
