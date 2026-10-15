package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observe original native generated item quad submission only; no item replacement. */
@Mixin({SubmitNodeStorage.class,SubmitNodeCollection.class})
public abstract class SpearNativeQuadsAuditMixin {
 @Inject(method="submitItem",at=@At("HEAD"))
 private void quads(PoseStack poses,ItemDisplayContext display,int light,int overlay,int outline,int[] tints,ItemQuads quads,ItemStackRenderState.FoilType foil,CallbackInfo ci){dev.wildcraft.test.SpearTransitionAudit.nativeQuads(poses);dev.wildcraft.test.AirSpearAudit.nativeQuads(poses);dev.wildcraft.test.AirMaceAudit.nativeQuads(poses);dev.wildcraft.test.JumpAudit.nativeQuads(poses);dev.wildcraft.test.GroundSpearAudit.nativeQuads(poses);dev.wildcraft.test.GroundMaceAudit.nativeQuads(poses);dev.wildcraft.test.MaceTransitionAudit.nativeQuads(poses);dev.wildcraft.test.CrossbowTransitionAudit.nativeQuads(poses);}
}
