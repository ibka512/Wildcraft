package dev.wildcraft.test.performance.mixin;
import dev.wildcraft.test.performance.NativeRenderBaseline;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Included only by the isolated normal-client probe metadata, never by production or client-gametest runs. */
@Mixin(Minecraft.class)
public abstract class NativeRenderBaselineMixin {
    @Inject(method="runTick",at=@At("TAIL")) private void wildcraft$c7baseline(CallbackInfo ci){if(Boolean.getBoolean("wildcraft.c7.driveBaseline"))dev.wildcraft.test.performance.NativeDrivingBaseline.frame((Minecraft)(Object)this);else NativeRenderBaseline.frame((Minecraft)(Object)this);}
}
