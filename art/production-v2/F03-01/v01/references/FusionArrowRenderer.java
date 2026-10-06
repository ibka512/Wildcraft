package dev.wildcraft.client.fuse;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcraft.fuse.FusionCombat;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemDisplayContext;
public final class FusionArrowRenderer{
 private static final RenderStateDataKey<ItemStackRenderState> MODEL=RenderStateDataKey.create(() -> "wildcraft:fusion_arrow_model");
 private FusionArrowRenderer(){}
 public static void extract(AbstractArrow arrow,ArrowRenderState state){var v=arrow.getAttached(FusionCombat.ARROW_VIEW);if(v==null){state.setData(MODEL,null);return;}var item=new ItemStackRenderState();Minecraft.getInstance().getItemModelResolver().updateForNonLiving(item,FusionPresentation.display(v),ItemDisplayContext.FIXED,arrow);state.setData(MODEL,item);}
 public static void submit(ArrowRenderState state,PoseStack poses,SubmitNodeCollector collector){var item=state.getData(MODEL);if(item==null||item.isEmpty())return;var bounds=item.getModelBoundingBox();double size=Math.max(bounds.getXsize(),Math.max(bounds.getYsize(),bounds.getZsize()));if(!Double.isFinite(size)||size<.001||size>100)return;var center=bounds.getCenter();poses.pushPose();poses.rotateDegrees(Axis.YP,state.yRot-90);poses.rotateDegrees(Axis.ZP,state.xRot);poses.translate(.35,0,0);float scale=(float)(.22/size);poses.scale(scale,scale,scale);poses.translate(-center.x,-center.y,-center.z);item.submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);poses.popPose();}
}
