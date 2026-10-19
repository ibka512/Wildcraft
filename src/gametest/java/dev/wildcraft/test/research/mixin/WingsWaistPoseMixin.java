package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.WingsWaistAudit;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ElytraModel.class)
public abstract class WingsWaistPoseMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("RETURN"))
    private void pose(HumanoidRenderState s,CallbackInfo ci){WingsWaistAudit.pose((ElytraModel)(Object)this,s);}
}
