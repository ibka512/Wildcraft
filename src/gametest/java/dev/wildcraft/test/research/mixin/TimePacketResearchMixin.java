package dev.wildcraft.test.research.mixin;

import dev.wildcraft.test.research.time.WorldTimeResearch;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep input responsive only while the integrated server is BETWEEN world ticks. */
@Mixin(MinecraftServer.class)
public abstract class TimePacketResearchMixin {
    @Shadow private boolean waitingForNextTick;

    @Inject(method = "waitForTasks", at = @At("HEAD"))
    private void wildcraftResearch$packets(CallbackInfo info) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (Boolean.getBoolean("wildcraft.r1.frameRelease") && waitingForNextTick && WorldTimeResearch.hasCompensation(server)) {
            server.packetProcessor().processQueuedPackets();
        }
    }

    @ModifyArg(method = "waitForTasks", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/LockSupport;parkNanos(Ljava/lang/Object;J)V"), index = 1)
    private long wildcraftResearch$wait(long nanos) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        return Boolean.getBoolean("wildcraft.r1.frameRelease") && waitingForNextTick && WorldTimeResearch.hasCompensation(server)
                ? Math.min(nanos, 25_000_000L) : nanos;
    }
}
