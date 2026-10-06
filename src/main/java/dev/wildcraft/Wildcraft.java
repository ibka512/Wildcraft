package dev.wildcraft;

import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.player.StaminaCommands;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Wildcraft implements ModInitializer {
    public static final String MOD_ID = "wildcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Identifier AFTER_ATTACHMENT_TRANSFER = id("after_attachment_transfer");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_ATTACHMENT_TRANSFER);
        dev.wildcraft.art.ArtFeedback.initialize();
        WildcraftItems.initialize();
        dev.wildcraft.energy.EnergyContent.initialize();
        dev.wildcraft.mechanics.MechanicsContent.initialize();
        dev.wildcraft.fabrication.FabricationContent.initialize();
        dev.wildcraft.fuse.FusionContent.initialize();
        dev.wildcraft.cooking.CookingContent.initialize();
        dev.wildcraft.cooking.CookingEffects.initialize();
        dev.wildcraft.registry.WildcraftSounds.initialize();
        GliderEquipment.initialize();
        dev.wildcraft.equipment.BackEquipment.initialize();
        Climbing.initialize();
        dev.wildcraft.weather.WindSystem.initialize();
        Gliding.initialize();
        PlayerStamina.initialize();
        dev.wildcraft.focus.FocusTime.initialize();
        StaminaCommands.initialize();
        dev.wildcraft.temperature.EnvironmentTemperature.initialize();
        dev.wildcraft.temperature.TemperatureCommands.initialize();
        LOGGER.info("Wildcraft wind/weather initialized; fusion, fabrication, mechanics, energy, cooking, temperature and traversal registered.");
    }
}
