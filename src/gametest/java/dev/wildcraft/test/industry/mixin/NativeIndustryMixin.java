package dev.wildcraft.test.industry.mixin;
import dev.wildcraft.test.industry.NativeIndustryBaseline;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Only the explicitly selected ordinary industry probe. */
@Mixin(Minecraft.class)
public abstract class NativeIndustryMixin {
    @Inject(method="runTick",at=@At("TAIL"))private void wildcraft$industry(CallbackInfo ci){NativeIndustryBaseline.frame((Minecraft)(Object)this);}
}
