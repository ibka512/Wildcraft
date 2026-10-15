package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.TridentSpecialRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TridentSpecialRenderer.class)
public abstract class TridentSpecialAuditMixin {
 @Inject(method="submit",at=@At("HEAD"))
 private void model(PoseStack poses,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline,CallbackInfo ci){dev.wildcraft.test.TridentTransitionAudit.nativeModel(poses);dev.wildcraft.test.GroundTridentAudit.nativeModel(poses);dev.wildcraft.test.AirTridentAudit.nativeModel(poses);}
}
