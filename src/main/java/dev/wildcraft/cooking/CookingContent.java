package dev.wildcraft.cooking;

import dev.wildcraft.Wildcraft;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class CookingContent {
    public static final DataComponentType<MealData> MEAL_DATA=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Wildcraft.id("meal"),
            DataComponentType.<MealData>builder().persistent(MealData.CODEC).networkSynchronized(MealData.STREAM_CODEC).build());
    private static final ResourceKey<Block> POT_KEY=ResourceKey.create(Registries.BLOCK,Wildcraft.id("cooking_pot"));
    public static final CookingPotBlock POT=Registry.register(BuiltInRegistries.BLOCK,POT_KEY,
            new CookingPotBlock(BlockBehaviour.Properties.of().setId(POT_KEY).strength(2).noOcclusion()));
    private static final ResourceKey<Item> POT_ITEM_KEY=ResourceKey.create(Registries.ITEM,Wildcraft.id("cooking_pot"));
    public static final Item POT_ITEM=Registry.register(BuiltInRegistries.ITEM,POT_ITEM_KEY,
            new BlockItem(POT,new Item.Properties().setId(POT_ITEM_KEY)));
    public static final BlockEntityType<CookingPotEntity> POT_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Wildcraft.id("cooking_pot"),
            new BlockEntityType<>(CookingPotEntity::new,Set.of(POT)));
    public static final MenuType<CookingMenu> MENU=Registry.register(BuiltInRegistries.MENU,Wildcraft.id("cooking_pot"),new MenuType<>(CookingMenu::new,FeatureFlags.DEFAULT_FLAGS));
    private static final ResourceKey<Item> MEAL_KEY=ResourceKey.create(Registries.ITEM,Wildcraft.id("meal"));
    public static final Item MEAL=Registry.register(BuiltInRegistries.ITEM,MEAL_KEY,new MealItem(new Item.Properties().setId(MEAL_KEY).stacksTo(1)
            .food(new FoodProperties.Builder().nutrition(8).saturationModifier(.6F).alwaysEdible().build()).usingConvertsTo(Items.BOWL)));
    private CookingContent() { }
    public static ItemStack meal(MealData data) {var stack=new ItemStack(MEAL);stack.set(MEAL_DATA,data);return stack;}
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(o -> o.accept(POT_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(o -> CookingRecipes.ALL.forEach(r -> o.accept(meal(r.outcome()))));
    }
}
