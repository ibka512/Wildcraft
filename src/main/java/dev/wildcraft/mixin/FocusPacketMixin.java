package dev.wildcraft.mixin;

import dev.wildcraft.focus.FocusTime;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class FocusPacketMixin {
    @Shadow private boolean waitingForNextTick;
    @Inject(method = "waitForTasks", at = @At("HEAD"))
    private void wildcraft$focusPackets(CallbackInfo ci) {
        var server = (MinecraftServer)(Object)this;
        if (waitingForNextTick && FocusTime.active(server)) {
            FocusTime.pulse(server);
            if (FocusTime.active(server)) server.packetProcessor().processQueuedPackets();
        }
    }
    @ModifyArg(method = "waitForTasks", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/LockSupport;parkNanos(Ljava/lang/Object;J)V"), index = 1)
    private long wildcraft$focusWait(long nanos) {
        return waitingForNextTick && FocusTime.active((MinecraftServer)(Object)this) ? Math.min(nanos, 25_000_000L) : nanos;
    }
}
