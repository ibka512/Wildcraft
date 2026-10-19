package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import dev.wildcraft.test.SwordWaistAudit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Item-state identity scopes native sword quads, excluding nested Fuse decoration. */
@Mixin(ItemStackRenderState.class)
public abstract class SwordWaistItemAuditMixin {
    private static final java.util.ArrayDeque<Boolean> PREVIOUS=new java.util.ArrayDeque<>();
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",at=@At("HEAD"))
    private void begin(PoseStack p,SubmitNodeCollector c,int light,int overlay,int outline,CallbackInfo ci){PREVIOUS.push(SwordWaistAudit.inItem);SwordWaistAudit.inItem=SwordWaistAudit.enabled&&(Object)this==SwordWaistAudit.expectedItem;}
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",at=@At("RETURN"))
    private void end(PoseStack p,SubmitNodeCollector c,int light,int overlay,int outline,CallbackInfo ci){SwordWaistAudit.inItem=PREVIOUS.pop();}
}
