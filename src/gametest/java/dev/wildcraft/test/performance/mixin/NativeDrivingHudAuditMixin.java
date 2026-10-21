package dev.wildcraft.test.performance.mixin;
import dev.wildcraft.test.performance.NativeDrivingBaseline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Only the isolated normal-client performance probe; no production instrumentation. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class NativeDrivingHudAuditMixin {
    @Inject(method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V",at=@At("HEAD"))
    private void wildcraft$nativeDrivingText(Font font,FormattedCharSequence text,int x,int y,int color,CallbackInfo ci){NativeDrivingBaseline.hudLine(text);dev.wildcraft.test.performance.NativePartsBaseline.hudLine(text);}
}
