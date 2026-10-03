package dev.wildcraft.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class WildcraftDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(WildcraftModelProvider::new);
        pack.addProvider(FocusEffectProvider::new);
        pack.addProvider((output, lookup) -> new WildcraftLanguageProvider(output, "en_us", lookup));
        pack.addProvider((output, lookup) -> new WildcraftLanguageProvider(output, "zh_cn", lookup));
    }
}
