package dev.wildcraft.client;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.client.hud.StaminaHud;
import dev.wildcraft.client.input.ClimbControls;
import dev.wildcraft.client.input.GlideControls;
import dev.wildcraft.client.render.ParagliderLayer;
import net.fabricmc.api.ClientModInitializer;

public final class WildcraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StaminaHud.initialize();
        ClimbControls.initialize();
        GlideControls.initialize();
        ParagliderLayer.initialize();
        dev.wildcraft.client.render.BackEquipmentLayer.initialize();
        Wildcraft.LOGGER.info("Wildcraft P3.1 client initialization complete; HUD, traversal controls and paraglider layer registered.");
    }
}
