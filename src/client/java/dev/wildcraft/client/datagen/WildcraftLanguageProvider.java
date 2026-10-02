package dev.wildcraft.client.datagen;

import java.util.concurrent.CompletableFuture;

import dev.wildcraft.registry.WildcraftItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

public final class WildcraftLanguageProvider extends FabricLanguageProvider {
    private final String language;

    public WildcraftLanguageProvider(FabricPackOutput output, String language,
                                    CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, language, lookup);
        this.language = language;
    }

    @Override
    public void generateTranslations(HolderLookup.Provider lookup, TranslationBuilder translations) {
        translations.add(WildcraftItems.TEST_CORE,
                language.equals("zh_cn") ? "Wildcraft 测试核心" : "Wildcraft Test Core");
        boolean chinese = language.equals("zh_cn");
        translations.add(WildcraftItems.PARAGLIDER, chinese ? "滑翔伞" : "Paraglider");
        translations.add("slot.wildcraft.paraglider", chinese ? "滑翔伞装备位 · 仅放滑翔伞" : "Paraglider slot · paraglider only");
        translations.add("hud.wildcraft.gliding", chinese ? "滑翔中 · 再按%s收伞" : "Gliding · press %s to close");
        translations.add("key.wildcraft.climb", chinese ? "按住攀爬 / 松开放手" : "Hold to climb / release to let go");
        translations.add("key.category.wildcraft.controls", "Wildcraft");
        translations.add("hud.wildcraft.climbing", chinese ? "攀爬中 · 松开%s放手" : "Climbing · release %s");
        translations.add("hud.wildcraft.stamina", chinese ? "精力 %s / %s" : "Stamina %s / %s");
        translations.add("hud.wildcraft.peak_level", chinese ? "历史最高等级 %s" : "Peak level %s");
        translations.add("command.wildcraft.stamina.status", chinese ? "精力 %s / %s；历史最高等级 %s" : "Stamina %s / %s; peak level %s");
        translations.add("command.wildcraft.stamina.consumed", chinese ? "已消耗 %s 精力" : "Consumed %s stamina");
        translations.add("command.wildcraft.stamina.filled", chinese ? "精力已补满" : "Stamina refilled");
    }
}
