package dev.wildcraft.client;

import dev.wildcraft.Wildcraft;
import net.fabricmc.api.ClientModInitializer;

public final class WildcraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Wildcraft.LOGGER.info("Wildcraft P0 client initialization complete.");
    }
}
