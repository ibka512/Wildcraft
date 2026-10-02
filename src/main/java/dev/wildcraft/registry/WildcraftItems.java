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
    public static final ResourceKey<Item> PARAGLIDER_KEY =
            ResourceKey.create(Registries.ITEM, Wildcraft.id("paraglider"));
    public static final Item PARAGLIDER = Registry.register(BuiltInRegistries.ITEM, PARAGLIDER_KEY,
            new Item(new Item.Properties().setId(PARAGLIDER_KEY).stacksTo(1)));

    private WildcraftItems() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(items -> items.accept(TEST_CORE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(items -> items.accept(PARAGLIDER));
    }
}
