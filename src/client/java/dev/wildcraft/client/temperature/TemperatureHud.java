package dev.wildcraft.client.temperature;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.client.hud.StaminaHud;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.temperature.EnvironmentTemperature;
import dev.wildcraft.temperature.TemperatureRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/** A small seven-step indicator with an optional perimeter tint; the aiming centre is never tinted. */
public final class TemperatureHud {
    private static LocalPlayer observedPlayer;
    private static ClientLevel observedLevel;
    private static long lastFrame;
    private static double displayed;
    private static int band = 3;
    private TemperatureHud() { }
    public static void initialize() {
        TemperatureOptions.load();
        HudElementRegistry.attachElementAfter(Wildcraft.id("stamina"), Wildcraft.id("temperature"), (g, tracker) -> extract(g));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }
    private static void reset() { observedPlayer = null; observedLevel = null; lastFrame = 0; displayed = 0; band = 3; }
    public static double displayed() { return displayed; }
    public static int displayedBand() { return band; }
    public static int rowY() { return StaminaHud.heartBottom() + (StaminaHud.showStamina() ? 29 : 5); }
    public static boolean edgeTintVisible() {
        var c = Minecraft.getInstance();
        return c.player != null && TemperatureOptions.get().edgeTint && !FocusTime.active(c.player) && Math.abs(displayed) > .5;
    }
    private static void extract(GuiGraphicsExtractor g) {
        var c = Minecraft.getInstance();
        var p = c.player;
        if (p == null || c.level == null) { reset(); return; }
        var view = p.getAttached(EnvironmentTemperature.VIEW);
        if (view == null || !p.isAlive() || p.isSpectator() || c.gui.hud.isHidden()) { lastFrame = 0; return; }
        long now = System.nanoTime();
        if (observedPlayer != p || observedLevel != c.level || lastFrame == 0) {
            observedPlayer = p; observedLevel = c.level; displayed = view.target(); band = TemperatureRules.initialBand(displayed); lastFrame = now;
        }
        double seconds = Math.max(0, now - lastFrame) / 1_000_000_000.0; lastFrame = now;
        if (!c.isPaused()) displayed = TemperatureRules.smooth(displayed, view.target(), seconds);
        band = TemperatureRules.band(displayed, band);
        int color = band < 3 ? 0xFFAACFE0 : band > 3 ? 0xFFE9BF85 : 0xFFCCD8C2;
        if (edgeTintVisible()) {
            int rgb = displayed < 0 ? 0x78ACD0 : 0xD49A62;
            var food=p.getAttached(dev.wildcraft.cooking.CookingEffects.VIEW);
            int level=food==null?0:displayed<0?food.warmth():food.cooling();
            int alpha = (int)Math.round(Math.min(14, Math.abs(displayed) * 5)/(level+1.0));
            // Only four pixels along the outside edge; no shader, FOV change, flash or centre overlay.
            g.fill(0, 0, 4, g.guiHeight(), alpha << 24 | rgb);
            g.fill(g.guiWidth() - 4, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | rgb);
        }
        int y = rowY();
        if (y + 10 > g.guiHeight() - 2 || g.guiWidth() < 100) return;
        Component label = Component.translatable("hud.wildcraft.temperature." + TemperatureRules.STATES[band]);
        g.fill(10, y - 2, Math.min(g.guiWidth() - 10, 18 + c.font.width(label) + 37), y + 10, 0x7015231B);
        for (int i = 0; i < 7; i++) g.fill(12 + i * 5, y + 2, 15 + i * 5, y + 6, i == band ? color : 0xFF4D5F51);
        g.text(c.font, label, 51, y, color);
    }
}
