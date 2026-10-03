package dev.wildcraft.client.focus;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FocusSettingsScreen extends Screen {
    private final Screen parent;
    public FocusSettingsScreen(Screen parent) { super(Component.translatable("focus.wildcraft.settings")); this.parent = parent; }
    @Override protected void init() {
        var o = FocusOptions.get();
        int w = Math.min(300, width - 24), x = (width - w) / 2;
        // Seven settings arranged in two columns fit small game windows too.
        int half = (w - 6) / 2;
        int y = Math.max(42, height / 2 - 55);
        addRenderableWidget(Button.builder(strengthLabel(), b -> {
            o.strength = (o.strength + 1) % 4; FocusOptions.save(); b.setMessage(strengthLabel());
        }).bounds(x, y, w, 20).build());
        toggle("vignette", () -> o.vignette, v -> o.vignette = v, x, y + 25, half);
        toggle("reticle", () -> o.reticle, v -> o.reticle = v, x + half + 6, y + 25, half);
        toggle("pattern", () -> o.pattern, v -> o.pattern = v, x, y + 50, half);
        toggle("pulse", () -> o.lowStaminaPulse, v -> o.lowStaminaPulse = v, x + half + 6, y + 50, half);
        toggle("fov", () -> o.fov, v -> o.fov = v, x, y + 75, w);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x, y + 105, w, 20).build());
    }
    private Component strengthLabel() {
        return Component.translatable("focus.wildcraft.strength", Component.translatable("focus.wildcraft.strength." + FocusOptions.get().strength));
    }
    private void toggle(String key, BooleanSupplier value, Consumer<Boolean> update, int x, int y, int width) {
        addRenderableWidget(Button.builder(label(key, value.getAsBoolean()), b -> {
            update.accept(!value.getAsBoolean()); FocusOptions.save(); b.setMessage(label(key, value.getAsBoolean()));
        }).bounds(x, y, width, 20).build());
    }
    private Component label(String key, boolean enabled) {
        return Component.translatable("focus.wildcraft." + key, Component.translatable(enabled ? "options.on" : "options.off"));
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        g.text(font, title, (width - font.width(title)) / 2, 15, 0xFFFFFFFF);
        Component hint = Component.translatable("focus.wildcraft.settings.hint");
        g.text(font, hint, (width - font.width(hint)) / 2, 28, 0xFFBBC7D0);
        super.extractRenderState(g, mouseX, mouseY, partial);
    }
}
