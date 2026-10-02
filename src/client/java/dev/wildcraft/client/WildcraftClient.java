package dev.wildcraft.client;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.client.hud.StaminaHud;
import net.fabricmc.api.ClientModInitializer;

public final class WildcraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StaminaHud.initialize();
        Wildcraft.LOGGER.info("Wildcraft P1 client initialization complete; stamina HUD registered.");
    }
}
