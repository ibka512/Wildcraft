package dev.wildcraft.test.research.mixin;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.test.EquipmentAudit;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BackTransitions.class)
public abstract class EquipmentBodyAuditMixin {
    @Inject(method="apply",at=@At("TAIL"))
    private static void wildcraft$body(HumanoidModel<?> model,AvatarRenderState avatar,CallbackInfo ci){if(EquipmentAudit.enabled)for(var arm:HumanoidArm.values()){var s=BackTransitions.forArm(avatar,arm);if(s!=null)EquipmentAudit.body[s.category()]++;}}
}
