package dev.wildcraft.test.performance.mixin;
import dev.wildcraft.test.performance.NativeBuoyancyBaseline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiGraphicsExtractor.class)
public abstract class NativeWaterHudAuditMixin {
    @Inject(method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V",at=@At("HEAD"))
    private void text(Font font,FormattedCharSequence text,int x,int y,int colour,CallbackInfo ci){NativeBuoyancyBaseline.hudLine(text);}
}
