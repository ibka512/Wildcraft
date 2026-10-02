package dev.wildcraft;

import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.player.StaminaCommands;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Wildcraft implements ModInitializer {
    public static final String MOD_ID = "wildcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        WildcraftItems.initialize();
        PlayerStamina.initialize();
        StaminaCommands.initialize();
        LOGGER.info("Wildcraft P1 common initialization complete; player stamina registered.");
    }
}
