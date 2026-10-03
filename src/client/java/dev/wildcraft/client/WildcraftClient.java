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
        net.minecraft.client.gui.screens.MenuScreens.register(dev.wildcraft.cooking.CookingContent.MENU, dev.wildcraft.client.cooking.CookingScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(dev.wildcraft.energy.EnergyContent.MENU, dev.wildcraft.client.energy.ChargerScreen::new);
        StaminaHud.initialize();
        dev.wildcraft.client.temperature.TemperatureHud.initialize();
        dev.wildcraft.client.cooking.MealHud.initialize();
        dev.wildcraft.client.render.CharacterPoses.initialize();
        dev.wildcraft.client.focus.FocusClient.initialize();
        ClimbControls.initialize();
        GlideControls.initialize();
        ParagliderLayer.initialize();
        dev.wildcraft.client.render.BackEquipmentLayer.initialize();
        Wildcraft.LOGGER.info("Wildcraft core art v1 client initialization complete; HUD, authored poses, traversal and equipment layers registered.");
    }
}
