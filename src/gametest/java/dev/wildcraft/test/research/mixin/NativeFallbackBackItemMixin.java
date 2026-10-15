package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.NativeFallbackAudit;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ItemStackRenderState.class)
public abstract class NativeFallbackBackItemMixin {
 @WrapMethod(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V")
 private void item(PoseStack poses,SubmitNodeCollector collector,int light,int overlay,int outline,Operation<Void> original){int previous=NativeFallbackAudit.kind;try{if(NativeFallbackAudit.enabled&&(Object)this==NativeFallbackAudit.expectedBackShield)NativeFallbackAudit.kind=3;original.call(poses,collector,light,overlay,outline);}finally{NativeFallbackAudit.kind=previous;}}
}
