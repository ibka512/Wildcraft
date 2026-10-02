package dev.wildcraft.client;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.client.hud.StaminaHud;
import dev.wildcraft.client.input.ClimbControls;
import net.fabricmc.api.ClientModInitializer;

public final class WildcraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StaminaHud.initialize();
        ClimbControls.initialize();
        Wildcraft.LOGGER.info("Wildcraft P2 client initialization complete; HUD and climbing controls registered.");
    }
}
