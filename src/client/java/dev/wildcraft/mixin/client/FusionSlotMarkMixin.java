package dev.wildcraft.mixin.client;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.fuse.FusionContent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiGraphicsExtractor.class)
public abstract class FusionSlotMarkMixin {
    @Inject(method="itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",at=@At("TAIL"))
    private void wildcraft$mark(Font font,ItemStack stack,int x,int y,String count,CallbackInfo ci){
        if(stack.has(FusionContent.VIEW))((GuiGraphicsExtractor)(Object)this).blit(RenderPipelines.GUI_TEXTURED,Wildcraft.id("textures/gui/fuse-status.png"),x-1,y-1,48,0,5,5,64,32);
    }
}
