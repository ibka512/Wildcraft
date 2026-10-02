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
import net.minecraft.network.chat.Component;

/** Upper-left hearts and stamina; vanilla XP and mount bars keep their own native area. */
public final class StaminaHud {
    private static LocalPlayer observedPlayer;
    private static StaminaView previous;
    private static int visibleUntil;
    private static int heartBottom = 21;
    public static void setHeartBottom(int bottom) { heartBottom = bottom; }
    private StaminaHud() { }
    public static void initialize() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HEALTH_BAR, Wildcraft.id("stamina"), (graphics, tracker) -> {
            if (showStamina()) extract(graphics);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }
    private static void reset() { observedPlayer = null; previous = null; visibleUntil = 0; heartBottom = 21; }
    public static boolean showStamina() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer p = client.player;
        if (p == null) { reset(); return false; }
        StaminaView view = p.getAttached(PlayerStamina.VIEW);
        if (p != observedPlayer) { observedPlayer = p; previous = view; visibleUntil = 0; }
        if (view != null && !view.equals(previous)) {
            if (view.stamina() < view.capacity() || previous != null && previous.stamina() < previous.capacity()) visibleUntil = p.tickCount + 40;
            previous = view;
        }
        return view != null && p.isAlive() && !p.isSpectator() && !client.gui.hud.isHidden()
                && (Climbing.active(p) || Gliding.active(p)
                || view.stamina() < view.capacity() || p.tickCount < visibleUntil);
    }
    private static void extract(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        StaminaView view = client.player.getAttached(PlayerStamina.VIEW);
        int width = Math.min(112, Math.max(40, graphics.guiWidth() - 24));
        int x = 12, y = Math.min(graphics.guiHeight() - 18, heartBottom + 17);
        double fraction = Math.clamp(view.stamina() / view.capacity(), 0, 1);
        int fill = (int)Math.round((width - 2) * fraction);
        int color = fraction <= 0.2 ? 0xFFF0B84D : 0xFF72D77E;
        Component label = Component.translatable("hud.wildcraft.stamina", number(view.stamina()), number(view.capacity()));
        graphics.text(client.font, label, x, y - 11, color);
        graphics.fill(x, y, x + width, y + 5, 0xCC1B251F);
        graphics.fill(x + 1, y + 1, x + 1 + fill, y + 4, color);
        graphics.outline(x, y, width, 5, 0xFFB5C8B7);
    }
    private static String number(double value) {
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        if (value >= 1000) return String.format(Locale.ROOT, "%.1fk", value / 1000);
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
