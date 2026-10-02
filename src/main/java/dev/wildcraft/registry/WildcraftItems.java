package dev.wildcraft.registry;

import dev.wildcraft.Wildcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class WildcraftItems {
    public static final ResourceKey<Item> TEST_CORE_KEY =
            ResourceKey.create(Registries.ITEM, Wildcraft.id("test_core"));
    public static final Item TEST_CORE = Registry.register(
            BuiltInRegistries.ITEM,
            TEST_CORE_KEY,
            new Item(new Item.Properties().setId(TEST_CORE_KEY))
    );

    private WildcraftItems() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(items -> items.accept(TEST_CORE));
    }
}
