package dev.wildcraft.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Keep the native carried-index cache and packet order before a custom weapon intent. */
@Mixin(MultiPlayerGameMode.class)
public interface CarriedItemAccess {
    @Invoker("ensureHasSentCarriedItem")
    void wildcraft$syncCarriedItem();
}
