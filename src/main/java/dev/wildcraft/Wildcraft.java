package dev.wildcraft;

import dev.wildcraft.registry.WildcraftItems;
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
        LOGGER.info("Wildcraft P0 common initialization complete; test core registered.");
    }
}
