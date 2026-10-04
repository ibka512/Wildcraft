package dev.wildcraft.fabrication;

import dev.wildcraft.Wildcraft;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class FabricationContent {
    private static final ResourceKey<Block> KEY=ResourceKey.create(Registries.BLOCK,Wildcraft.id("fabricator"));
    public static final FabricatorBlock BLOCK=Registry.register(BuiltInRegistries.BLOCK,KEY,new FabricatorBlock(BlockBehaviour.Properties.of().setId(KEY).strength(2).pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)));
    private static final ResourceKey<Item> ITEM_KEY=ResourceKey.create(Registries.ITEM,Wildcraft.id("fabricator"));
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,ITEM_KEY,new FabricatorItem(new Item.Properties().setId(ITEM_KEY).stacksTo(1)));
    public static final BlockEntityType<FabricatorEntity> ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Wildcraft.id("fabricator"),new BlockEntityType<>(FabricatorEntity::new,Set.of(BLOCK)));
    public static final MenuType<FabricatorMenu> MENU=Registry.register(BuiltInRegistries.MENU,Wildcraft.id("fabricator"),new MenuType<>(FabricatorMenu::new,FeatureFlags.DEFAULT_FLAGS));
    private FabricationContent(){ }
    public static void initialize(){CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(o -> o.accept(ITEM));}
}
