package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.NativeFallbackAudit;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({SubmitNodeStorage.class,SubmitNodeCollection.class})
public abstract class NativeFallbackCollectorMixin {
 @Inject(method="submitItem",at=@At("HEAD"))
 private void item(PoseStack poses,ItemDisplayContext display,int light,int overlay,int outline,int[] tints,ItemQuads quads,ItemStackRenderState.FoilType foil,CallbackInfo ci){if(!NativeFallbackAudit.enabled)return;if(NativeFallbackAudit.kind==1)NativeFallbackAudit.bowQuads++;else if(NativeFallbackAudit.kind==2)NativeFallbackAudit.otherQuads++;}
 @Inject(method="submitModel",at=@At("HEAD"))
 private <S>void model(Model<? super S> model,S state,PoseStack poses,RenderType type,int light,int overlay,int color,UvMapping uv,int outline,CallbackInfo ci){
  if(!NativeFallbackAudit.enabled)return;
  var binding=((RenderSetupTexturesAccessor)(Object)((RenderTypeStateAccessor)(Object)type).wildcraft$setup()).wildcraft$textures().get("Sampler0");var texture=binding==null?null:((TextureBindingLocationAccessor)binding).wildcraft$location();
  if(model instanceof PlayerModel&&state instanceof AvatarRenderState a&&a.getData(CombatRigLayer.FRAME)==null&&!a.isInvisible&&!a.isSpectator&&texture!=null&&texture.equals(a.skin.body().texturePath())){if(((PlayerModelShapeAccessor)model).wildcraft$isSlim()!=(a.skin.model()==net.minecraft.world.entity.player.PlayerModelType.SLIM))throw new AssertionError("Native fallback model geometry differs from actual selected skin");NativeFallbackAudit.bodies++;NativeFallbackAudit.skin=texture.toString();}
  if(texture!=null&&texture.equals(NativeFallbackAudit.expectedArmorTexture))NativeFallbackAudit.armorModels++;
  if(NativeFallbackAudit.kind==3&&model instanceof ShieldModel)NativeFallbackAudit.shieldModels++;
 }
}
