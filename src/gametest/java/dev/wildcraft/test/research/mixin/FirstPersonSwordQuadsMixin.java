package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({SubmitNodeStorage.class,SubmitNodeCollection.class})
public abstract class FirstPersonSwordQuadsMixin {
    @Inject(method="submitItem",at=@At("HEAD"))
    private void quads(PoseStack p,ItemDisplayContext d,int light,int overlay,int outline,int[] t,ItemQuads q,ItemStackRenderState.FoilType foil,CallbackInfo ci){dev.wildcraft.test.FirstPersonSwordAudit.quads(p);dev.wildcraft.test.SwordWaistAudit.quads(p);}
    @Inject(method="submitModel",at=@At("HEAD"))
    private <S>void shield(net.minecraft.client.model.Model<? super S> model,S state,PoseStack poses,net.minecraft.client.renderer.rendertype.RenderType type,int light,int overlay,int color,net.minecraft.client.renderer.texture.UvMapping uv,int outline,CallbackInfo ci){
        if(dev.wildcraft.test.FirstPersonSwordAudit.enabled&&dev.wildcraft.test.FirstPersonSwordAudit.inShieldItem&&model instanceof net.minecraft.client.model.object.equipment.ShieldModel)dev.wildcraft.test.FirstPersonSwordAudit.shieldGpu++;
    }
}
