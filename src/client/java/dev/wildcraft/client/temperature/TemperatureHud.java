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
        int[] colours={0xFF7FAECD,0xFFA4C8DD,0xFFBFD6DA,0xFFCBD8BD,0xFFDDC39A,0xFFE5B37B,0xFFEF9A66};
        int color=colours[band];
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
        if (y + 13 > g.guiHeight() - 46 || g.guiWidth() < 100) return;
        Component label=Component.translatable("hud.wildcraft.temperature."+TemperatureRules.STATES[band]);
        int width=Math.max(104,38+c.font.width(label));
        if(10+width>g.guiWidth()-10) return;
        g.fill(10,y-3,10+width,y+13,0x7015231B);
        dev.wildcraft.client.art.ArtGui.icon(g,"ambient-temperature",12,y-3);
        for(int i=0;i<7;i++)g.fill(32,y-2+(6-i)*2,37,y-1+(6-i)*2,colours[i]);
        int markerY=y-2+(6-band)*2;
        g.fill(31,markerY-1,38,markerY+2,0xFFFFFFFF);g.fill(32,markerY,37,markerY+1,color);
        g.text(c.font,label,44,y,color);
    }
}
