package dev.wildcraft.energy;

import dev.wildcraft.Wildcraft;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class EnergyContent {
    public static final DataComponentType<BatteryData> BATTERY_DATA=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Wildcraft.id("battery"),
            DataComponentType.<BatteryData>builder().persistent(BatteryData.CODEC).networkSynchronized(BatteryData.STREAM_CODEC).build());
    private static final ResourceKey<Item> BATTERY_KEY=ResourceKey.create(Registries.ITEM,Wildcraft.id("battery"));
    public static final Item BATTERY=Registry.register(BuiltInRegistries.ITEM,BATTERY_KEY,
            new BatteryItem(new Item.Properties().setId(BATTERY_KEY).stacksTo(1).component(BATTERY_DATA,BatteryData.EMPTY)));
    private static final ResourceKey<Block> CHARGER_KEY=ResourceKey.create(Registries.BLOCK,Wildcraft.id("charger"));
    public static final ChargerBlock CHARGER=Registry.register(BuiltInRegistries.BLOCK,CHARGER_KEY,
            new ChargerBlock(BlockBehaviour.Properties.of().setId(CHARGER_KEY).strength(2)));
    private static final ResourceKey<Item> CHARGER_ITEM_KEY=ResourceKey.create(Registries.ITEM,Wildcraft.id("charger"));
    public static final Item CHARGER_ITEM=Registry.register(BuiltInRegistries.ITEM,CHARGER_ITEM_KEY,new BlockItem(CHARGER,new Item.Properties().setId(CHARGER_ITEM_KEY)));
    public static final BlockEntityType<ChargerEntity> CHARGER_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Wildcraft.id("charger"),new BlockEntityType<>(ChargerEntity::new,Set.of(CHARGER)));
    public static final MenuType<ChargerMenu> MENU=Registry.register(BuiltInRegistries.MENU,Wildcraft.id("charger"),new MenuType<>(ChargerMenu::new,FeatureFlags.DEFAULT_FLAGS));
    private EnergyContent(){ }
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(o -> o.accept(CHARGER_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o -> o.accept(BATTERY));
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> FixedEnergy.clear());
    }
}
