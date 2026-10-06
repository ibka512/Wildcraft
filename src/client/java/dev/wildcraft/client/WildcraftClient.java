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
        net.minecraft.client.gui.screens.MenuScreens.register(dev.wildcraft.fabrication.FabricationContent.MENU, dev.wildcraft.client.fabrication.FabricatorScreen::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(dev.wildcraft.mechanics.MechanicsContent.MACHINE, dev.wildcraft.client.mechanics.MachineRenderer::new);
        net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.ID_MAPPER.put(Wildcraft.id("meal_kind"),dev.wildcraft.client.art.MealKindProperty.CODEC);
        net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.ID_MAPPER.put(Wildcraft.id("battery_energy"),dev.wildcraft.client.art.BatteryEnergyProperty.CODEC);
        net.minecraft.client.renderer.special.SpecialModelRenderers.ID_MAPPER.put(Wildcraft.id("battery"),dev.wildcraft.client.art.BatteryItemRenderer.Unbaked.CODEC);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(dev.wildcraft.energy.EnergyContent.CHARGER_ENTITY,dev.wildcraft.client.art.ArtBlockRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(dev.wildcraft.fabrication.FabricationContent.ENTITY,dev.wildcraft.client.art.ArtBlockRenderer::new);
        dev.wildcraft.client.art.ArtFeedbackClient.initialize();
        StaminaHud.initialize();
        dev.wildcraft.client.temperature.TemperatureHud.initialize();
        dev.wildcraft.client.cooking.MealHud.initialize();
        dev.wildcraft.client.weather.WindHud.initialize();
        dev.wildcraft.client.mechanics.MachinePresentation.initialize();
        dev.wildcraft.client.fuse.FusionPresentation.initialize();
        dev.wildcraft.client.render.CharacterPoses.initialize();
        dev.wildcraft.client.focus.FocusClient.initialize();
        ClimbControls.initialize();
        GlideControls.initialize();
        ParagliderLayer.initialize();
        dev.wildcraft.client.render.BackEquipmentLayer.initialize();
        Wildcraft.LOGGER.info("Wildcraft second art client initialized; adopted models, menus, HUD, equipment poses and confirmed feedback registered.");
    }
}
