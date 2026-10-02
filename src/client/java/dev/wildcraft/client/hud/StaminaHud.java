package dev.wildcraft.client.hud;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.StaminaView;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.client.input.ClimbControls;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class StaminaHud {
    private static LocalPlayer observedPlayer;
    private static StaminaView previous;
    private static int visibleUntil;

    private StaminaHud() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Wildcraft.id("stamina"), StaminaHud::extract);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            observedPlayer = null;
            previous = null;
            visibleUntil = 0;
        });
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            observedPlayer = null;
            previous = null;
            return;
        }
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        StaminaView view = player.getAttached(PlayerStamina.VIEW);
        if (view == null) {
            return;
        }
        if (player != observedPlayer || !view.equals(previous)) {
            observedPlayer = player;
            previous = view;
            visibleUntil = player.tickCount + 60;
        }
        if (view.stamina() >= view.capacity() && player.tickCount > visibleUntil) {
            return;
        }
        int x = 10;
        int y = Math.max(20, graphics.guiHeight() - 62);
        int width = Math.min(112, Math.max(40, graphics.guiWidth() - 20));
        double fraction = Math.clamp(view.stamina() / view.capacity(), 0, 1);
        int fill = (int) Math.round((width - 2) * fraction);
        int color = fraction <= 0.2 ? 0xFFF0B84D : 0xFF72D77E;
        graphics.text(client.font, Component.translatable("hud.wildcraft.stamina",
                number(view.stamina()), number(view.capacity())), x, y - 12, 0xFFFFFFFF);
        graphics.fill(x, y, x + width, y + 7, 0xCC1B251F);
        graphics.fill(x + 1, y + 1, x + 1 + fill, y + 6, color);
        graphics.outline(x, y, width, 7, 0xFFB5C8B7);
        graphics.text(client.font, Component.translatable("hud.wildcraft.peak_level", view.highestLevel()), x, y + 10, 0xFFD5E3D7);
        if (Climbing.active(player)) {
            graphics.text(client.font, Component.translatable("hud.wildcraft.climbing", ClimbControls.CLIMB.getTranslatedKeyMessage()),
                    x, y + 22, 0xFFD5E3D7);
        }
    }

    private static String number(double value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        }
        if (value >= 1000) {
            return String.format(Locale.ROOT, "%.1fk", value / 1000);
        }
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
