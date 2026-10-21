package dev.wildcraft.test.cleanboot.mixin;
import dev.wildcraft.test.cleanboot.NativeCleanBoot;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Only the explicit minimal ordinary-menu test JAR, never production metadata. */
@Mixin(Minecraft.class)
public abstract class NativeCleanBootMixin {
    @Inject(method="runTick",at=@At("TAIL"))private void wildcraft$cleanBoot(CallbackInfo ci){NativeCleanBoot.frame((Minecraft)(Object)this);}
}
