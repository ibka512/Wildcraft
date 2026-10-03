package dev.wildcraft.client.focus;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import dev.wildcraft.Wildcraft;

/** Native controls and focus-aware scrolling; the six focus preferences and independent temperature tint. */
public final class FocusSettingsScreen extends Screen {
    private final Screen parent;
    private int panelX, panelY, panelWidth, panelHeight;
    public FocusSettingsScreen(Screen parent) { super(Component.translatable("focus.wildcraft.settings")); this.parent = parent; }
    @Override protected void init() {
        var o = FocusOptions.get();
        panelWidth = Math.min(320, width - 24); panelHeight = Math.min(254, height - 16);
        panelX = (width - panelWidth) / 2; panelY = (height - panelHeight) / 2;
        boolean compact = height < 240;
        int contentWidth = panelWidth - 38, half = (contentWidth - 6) / 2;
        var content = LinearLayout.vertical().spacing(5);
        var strength = Button.builder(strengthLabel(), b -> {
            o.strength = (o.strength + 1) % 4; FocusOptions.save(); b.setMessage(strengthLabel());
        }).size(contentWidth, 20).build();
        strength.setTooltip(Tooltip.create(Component.translatable("focus.wildcraft.help.strength")));
        content.addChild(strength);
        var first = LinearLayout.horizontal().spacing(6);
        first.addChild(toggle("vignette", () -> o.vignette, v -> o.vignette = v, half));
        first.addChild(toggle("reticle", () -> o.reticle, v -> o.reticle = v, half)); content.addChild(first);
        var second = LinearLayout.horizontal().spacing(6);
        second.addChild(toggle("pattern", () -> o.pattern, v -> o.pattern = v, half));
        second.addChild(toggle("pulse", () -> o.lowStaminaPulse, v -> o.lowStaminaPulse = v, half)); content.addChild(second);
        content.addChild(toggle("fov", () -> o.fov, v -> o.fov = v, contentWidth));
        var temperature = Button.builder(temperatureLabel(), b -> {
            var settings = dev.wildcraft.client.temperature.TemperatureOptions.get();
            settings.edgeTint = !settings.edgeTint; dev.wildcraft.client.temperature.TemperatureOptions.save(); b.setMessage(temperatureLabel());
        }).size(contentWidth, 20).build();
        temperature.setTooltip(Tooltip.create(Component.translatable("temperature.wildcraft.help.tint")));
        content.addChild(temperature);
        int top = panelY + (compact ? 46 : 66);
        int doneY = panelY + panelHeight - 30;
        var scroll = new ScrollableLayout(minecraft, content, Math.max(20, doneY - top - (compact ? 5 : height >= 270 ? 43 : 8)), ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.arrangeElements(); scroll.setX(panelX + 14); scroll.setY(top);
        scroll.visitWidgets(this::addRenderableWidget);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(panelX + 14, doneY, panelWidth - 28, 20).build());
    }
    private Component temperatureLabel() {
        return Component.translatable("temperature.wildcraft.tint", Component.translatable(
                dev.wildcraft.client.temperature.TemperatureOptions.get().edgeTint ? "options.on" : "options.off"));
    }
    private Component strengthLabel() {
        return Component.translatable("focus.wildcraft.strength", Component.translatable("focus.wildcraft.strength." + FocusOptions.get().strength));
    }
    private Button toggle(String key, BooleanSupplier value, Consumer<Boolean> update, int width) {
        var button = Button.builder(label(key, value.getAsBoolean()), b -> {
            update.accept(!value.getAsBoolean()); FocusOptions.save(); b.setMessage(label(key, value.getAsBoolean()));
        }).size(width, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable("focus.wildcraft.help." + key)));
        return button;
    }
    private Component label(String key, boolean enabled) {
        return Component.translatable("focus.wildcraft." + key, Component.translatable(enabled ? "options.on" : "options.off"));
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        g.fill(panelX, panelY, panelX+panelWidth, panelY+panelHeight, 0xEF18271C);
        g.outline(panelX, panelY, panelWidth, panelHeight, 0xFF607658);
        boolean compact = height < 240;
        g.blit(RenderPipelines.GUI_TEXTURED, Wildcraft.id("icon.png"), panelX+14, panelY+12, 0, 0, 20, 20, 128, 128, 128, 128);
        g.text(font, "WILDCRAFT", panelX+40, panelY+10, 0xFFB6EB91);
        g.text(font, title, panelX+40, panelY+21, 0xFFFFFFFF);
        var hint = Component.translatable("focus.wildcraft.settings.hint");
        if (!compact) {
            int y = panelY+43;
            for (var line : font.split(hint, panelWidth-28)) { g.text(font, line, panelX+14, y, 0xFFBBC7B0); y+=10; }
        }
        if (height >= 270) {
            int helpY = panelY+panelHeight-65;
            g.fill(panelX+14, helpY-4, panelX+panelWidth-14, helpY+24, 0x80293629);
            for (var line : font.split(Component.translatable("focus.wildcraft.help.strength"), panelWidth-40)) {
                g.text(font, line, panelX+20, helpY, 0xFFBBC7B0); helpY+=10;
            }
        }
        super.extractRenderState(g, mouseX, mouseY, partial);
    }
}
