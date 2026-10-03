package dev.wildcraft.client.hud;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.StaminaView;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Upper-left hearts and stamina; vanilla XP and mount bars keep their own native area. */
public final class StaminaHud {
    private static LocalPlayer observedPlayer;
    private static StaminaView previous;
    private static int visibleUntil;
    private static int heartBottom = 21;
    private static boolean recovering;
    public static void setHeartBottom(int bottom) { heartBottom = bottom; }
    private StaminaHud() { }
    public static void initialize() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HEALTH_BAR, Wildcraft.id("stamina"), (graphics, tracker) -> {
            if (showStamina()) extract(graphics);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }
    private static void reset() { observedPlayer = null; previous = null; visibleUntil = 0; heartBottom = 21; recovering = false; }
    public static boolean showStamina() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer p = client.player;
        if (p == null) { reset(); return false; }
        StaminaView view = p.getAttached(PlayerStamina.VIEW);
        if (p != observedPlayer) { observedPlayer = p; previous = view; visibleUntil = 0; recovering = false; }
        if (view != null && !view.equals(previous)) {
            recovering = previous != null && view.stamina() > previous.stamina() && view.capacity() == previous.capacity();
            if (view.stamina() < view.capacity() || previous != null && previous.stamina() < previous.capacity()) visibleUntil = p.tickCount + 40;
            previous = view;
        }
        return view != null && p.isAlive() && !p.isSpectator() && !client.gui.hud.isHidden()
                && (Climbing.active(p) || Gliding.active(p) || dev.wildcraft.focus.FocusTime.active(p)
                || view.stamina() < view.capacity() || p.tickCount < visibleUntil);
    }
    private static void extract(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        StaminaView view = client.player.getAttached(PlayerStamina.VIEW);
        int width = Math.min(112, Math.max(40, graphics.guiWidth() - 24));
        int x = 12, y = heartBottom + 17;
        // Never move the block back over the hearts in a short viewport.
        if (y + 7 > graphics.guiHeight() - 2) return;
        double fraction = Math.clamp(view.stamina() / view.capacity(), 0, 1);
        int fill = (int)Math.round((width - 4) * fraction);
        int color = fraction <= 0.2 ? 0xFFF0B84D : 0xFFDCE8D0;
        Component label = Component.translatable("hud.wildcraft.stamina", number(view.stamina()), number(view.capacity()));
        graphics.text(client.font, label, x, y - 11, color);
        var atlas = Wildcraft.id("textures/gui/stamina-hud-atlas.png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, atlas, x, y, 0, 0, width, 7, 112, 7, 112, 32);
        if (fill > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, atlas, x + 2, y + 2, 0, fraction <= 0.2 ? 16 : 8, fill, 3, 112, 32);
            if (recovering && fraction > 0.2 && fraction < 1) {
                int tip = Math.min(2, fill);
                graphics.blit(RenderPipelines.GUI_TEXTURED, atlas, x + 2 + fill - tip, y + 2, 0, 24, tip, 3, 112, 32);
            }
        }
    }
    private static String number(double value) {
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        if (value >= 1000) return String.format(Locale.ROOT, "%.1fk", value / 1000);
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
