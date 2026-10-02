package dev.wildcraft.test.research.mixin;

import dev.wildcraft.test.research.time.TimeControlEpoch;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerTickRateManager.class)
public abstract class TimeEpochMixin implements TimeControlEpoch {
    @Unique private long wildcraftResearch$revision;

    @Inject(method = "setTickRate", at = @At("HEAD"))
    private void wildcraftResearch$write(CallbackInfo info) { wildcraftResearch$revision++; }

    @Override public long wildcraftResearch$epoch() { return wildcraftResearch$revision; }
}
