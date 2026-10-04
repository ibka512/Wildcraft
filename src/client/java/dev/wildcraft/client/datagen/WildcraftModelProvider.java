package dev.wildcraft.client.datagen;

import dev.wildcraft.registry.WildcraftItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

public final class WildcraftModelProvider extends FabricModelProvider {
    public WildcraftModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        generator.createTrivialCube(dev.wildcraft.energy.EnergyContent.CHARGER);
        generator.registerSimpleItemModel(dev.wildcraft.energy.EnergyContent.CHARGER, net.minecraft.client.data.models.model.ModelLocationUtils.getModelLocation(dev.wildcraft.energy.EnergyContent.CHARGER));
        generator.createNonTemplateModelBlock(dev.wildcraft.cooking.CookingContent.POT);
        generator.registerSimpleItemModel(dev.wildcraft.cooking.CookingContent.POT, net.minecraft.client.data.models.model.ModelLocationUtils.getModelLocation(dev.wildcraft.cooking.CookingContent.POT));
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        generator.generateFlatItem(dev.wildcraft.mechanics.MechanicsContent.BODY, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(dev.wildcraft.mechanics.MechanicsContent.FAN, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(dev.wildcraft.mechanics.MechanicsContent.WING, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(dev.wildcraft.mechanics.MechanicsContent.WHEEL, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(dev.wildcraft.energy.EnergyContent.BATTERY, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(dev.wildcraft.cooking.CookingContent.MEAL, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(WildcraftItems.TEST_CORE, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(WildcraftItems.PARAGLIDER, ModelTemplates.FLAT_ITEM);
    }

    @Override
    public String getName() {
        return "Wildcraft item models";
    }
}
