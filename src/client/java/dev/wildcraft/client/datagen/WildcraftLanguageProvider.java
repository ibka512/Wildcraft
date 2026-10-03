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
        translations.add("subtitles.wildcraft.focus.enter", chinese ? "进入专注" : "Enter focus");
        translations.add("subtitles.wildcraft.focus.exit", chinese ? "退出专注" : "Leave focus");
        translations.add(WildcraftItems.PARAGLIDER, chinese ? "滑翔伞" : "Paraglider");
        translations.add("key.wildcraft.focus_settings", chinese ? "Wildcraft 画面设置" : "Wildcraft presentation settings");
        translations.add("key.category.wildcraft.focus_settings", chinese ? "Wildcraft · 画面" : "Wildcraft \u00b7 Presentation");
        translations.add("focus.wildcraft.settings", chinese ? "Wildcraft · 画面设置" : "Wildcraft \u00b7 Presentation");
        translations.add("focus.wildcraft.settings.hint", chinese ? "仅调整表现，技能、精力与环境读数保持生效" : "Presentation only; skill, stamina and environment readings stay active");
        translations.add("focus.wildcraft.strength", chinese ? "效果强度：%s" : "Effect strength: %s");
        translations.add("focus.wildcraft.strength.0", chinese ? "关闭" : "Off");
        translations.add("focus.wildcraft.strength.1", chinese ? "弱" : "Weak");
        translations.add("focus.wildcraft.strength.2", chinese ? "标准" : "Standard");
        translations.add("focus.wildcraft.strength.3", chinese ? "强" : "Strong");
        translations.add("focus.wildcraft.vignette", chinese ? "暗角：%s" : "Vignette: %s");
        translations.add("focus.wildcraft.reticle", chinese ? "准星增强：%s" : "Reticle: %s");
        translations.add("focus.wildcraft.pattern", chinese ? "边缘纹理：%s" : "Edge pattern: %s");
        translations.add("focus.wildcraft.pulse", chinese ? "低精力轻闪：%s" : "Low stamina pulse: %s");
        translations.add("focus.wildcraft.fov", chinese ? "轻微视野收窄：%s" : "Slight FOV: %s");
        translations.add("focus.wildcraft.help.strength", chinese ? "关闭/弱/标准/强；各开关偏好独立保留。" : "Off/weak/standard/strong; toggle preferences are retained.");
        translations.add("focus.wildcraft.help.vignette", chinese ? "轻微压暗画面边缘，保持瞄准中心清晰。" : "Gently shades the edges, keeping the aiming centre clear.");
        translations.add("focus.wildcraft.help.reticle", chinese ? "专注时增强准星，低精力用颜色与刻度提示。" : "Focus reticle; low stamina changes both colour and ticks.");
        translations.add("focus.wildcraft.help.pattern", chinese ? "仅在画面外围显示静态细纹。" : "Static contour lines at the periphery only.");
        translations.add("focus.wildcraft.help.pulse", chinese ? "可选低精力轻闪；默认关闭。" : "Optional gentle low-stamina pulse; off by default.");
        translations.add("focus.wildcraft.help.fov", chinese ? "可选轻微视野收窄；默认关闭。" : "Optional slight FOV reduction; off by default.");
        String[] thermalNames = chinese ? new String[]{"极冷", "寒冷", "偏冷", "舒适", "偏暖", "炎热", "极热"}
                : new String[]{"Very cold", "Cold", "Cool", "Comfortable", "Warm", "Hot", "Very hot"};
        for (int i = 0; i < thermalNames.length; i++) translations.add("hud.wildcraft.temperature." + dev.wildcraft.temperature.TemperatureRules.STATES[i], thermalNames[i]);
        translations.add("command.wildcraft.temperature.status", chinese ? "环境 %s；指数 %s（非摄氏度）；群系 %s / 天气 %s / 水 %s / 热源 %s；读取 %s / 射线 %s" : "Environment %s; index %s (not Celsius); biome %s / weather %s / water %s / heat %s; reads %s / rays %s");
        translations.add("temperature.wildcraft.tint", chinese ? "温度边缘色调：%s" : "Temperature edge tint: %s");
        translations.add("temperature.wildcraft.help.tint", chinese ? "轻微冷热边缘提示；专注时暂停，关闭后温度读数仍保留。" : "Subtle warm/cold edges; suppressed during focus. Readout remains when disabled.");
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
