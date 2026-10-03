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
        dev.wildcraft.client.render.CharacterPoses.initialize();
        dev.wildcraft.client.focus.FocusClient.initialize();
        ClimbControls.initialize();
        GlideControls.initialize();
        ParagliderLayer.initialize();
        dev.wildcraft.client.render.BackEquipmentLayer.initialize();
        Wildcraft.LOGGER.info("Wildcraft core art v1 client initialization complete; HUD, authored poses, traversal and equipment layers registered.");
    }
}
