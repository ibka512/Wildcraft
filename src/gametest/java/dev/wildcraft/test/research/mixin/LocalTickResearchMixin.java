package dev.wildcraft.test.research.mixin;

import dev.wildcraft.test.research.localtime.LocalTickProbe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The entire gate remains in the test JAR. Never change the world's rate. */
@Mixin(ServerLevel.class)
public abstract class LocalTickResearchMixin {
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void wildcraftResearch$localGate(Entity entity,CallbackInfo ci){if(LocalTickProbe.skip(entity))ci.cancel();}
}
