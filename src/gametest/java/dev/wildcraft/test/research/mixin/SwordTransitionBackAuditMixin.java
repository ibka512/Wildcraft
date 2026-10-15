package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.render.BackEquipmentLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observation only after the original real native back-item submission. */
@Mixin(BackEquipmentLayer.class)
public abstract class SwordTransitionBackAuditMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",at=@At("RETURN"))
    private void wildcraft$back(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch,CallbackInfo ci){dev.wildcraft.test.SwordTransitionAudit.back(state);dev.wildcraft.test.BowTransitionAudit.back(state);dev.wildcraft.test.ShieldTransitionAudit.back(state);dev.wildcraft.test.AxeTransitionAudit.back(state);dev.wildcraft.test.TridentTransitionAudit.back(state);dev.wildcraft.test.SpearTransitionAudit.back(state);dev.wildcraft.test.MaceTransitionAudit.back(state);dev.wildcraft.test.CrossbowTransitionAudit.back(state);}
}
