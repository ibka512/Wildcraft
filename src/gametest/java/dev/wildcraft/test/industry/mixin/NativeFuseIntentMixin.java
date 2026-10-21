package dev.wildcraft.test.industry.mixin;

import dev.wildcraft.fuse.FusionActions;
import dev.wildcraft.fuse.FusionIntent;
import dev.wildcraft.test.industry.NativeFuseProbe;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe original ownership/result without changing, delaying or replaying any packet. */
@Mixin(FusionActions.class)
public abstract class NativeFuseIntentMixin {
    @Inject(method="receive",at=@At("RETURN"))
    private static void wildcraft$observeNativeFuse(ServerPlayer p,FusionIntent input,CallbackInfoReturnable<Boolean> ci){NativeFuseProbe.received(p,input,ci.getReturnValue());}
}
