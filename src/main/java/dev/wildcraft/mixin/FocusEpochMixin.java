package dev.wildcraft.mixin;

import dev.wildcraft.focus.TimeControlEpoch;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerTickRateManager.class)
public abstract class FocusEpochMixin implements TimeControlEpoch {
    @Unique private long wildcraft$focusRevision;

    @Inject(method = "setTickRate", at = @At("HEAD"))
    private void wildcraft$focusWrite(CallbackInfo info) { wildcraft$focusRevision++; }

    @Override public long wildcraft$timeEpoch() { return wildcraft$focusRevision; }
}
