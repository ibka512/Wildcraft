package dev.wildcraft.client.focus;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.focus.ActivePlayClock;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.network.FocusView;
import dev.wildcraft.player.PlayerStamina;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BowItem;

public final class FocusClient {
    public static final KeyMapping SETTINGS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.wildcraft.focus_settings", InputConstants.Type.KEYBOARD, InputConstants.KEY_F8,
            KeyMapping.Category.register(Wildcraft.id("focus_settings"))));
    private static ActivePlayClock clock = new ActivePlayClock();
    private static FocusView previous = FocusView.OFF;
    private static double bowSeconds;
    private static boolean audibleActive;
    private FocusClient() { }
    public static void initialize() {
        FocusOptions.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SETTINGS.consumeClick()) if (client.gui.screen() == null) client.gui.setScreen(new FocusSettingsScreen(null));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Wildcraft.id("focus_reticle"), (g, tracker) -> {
            var client = Minecraft.getInstance(); var options = FocusOptions.get();
            if (!visible() || options.strength == 0 || !options.reticle || client.gui.hud.isHidden()) return;
            int x = g.guiWidth() / 2, y = g.guiHeight() / 2;
            int color = lowStamina() ? 0xDDF0B84D : 0xCCAFE5DC;
            g.fill(x - 10, y - 1, x - 7, y, color); g.fill(x + 7, y - 1, x + 10, y, color);
            g.fill(x - 1, y - 10, x, y - 7, color); g.fill(x - 1, y + 7, x, y + 10, color);
        });
    }
    private static void reset() { previous = FocusView.OFF; audibleActive = false; bowSeconds = 0; clock = new ActivePlayClock(); }
    public static boolean visible() {
        var c = Minecraft.getInstance();
        return c.player != null && c.gui.screen() == null && !c.isPaused() && FocusTime.active(c.player) && FocusTime.eligible(c.player);
    }
    public static boolean lowStamina() {
        var p = Minecraft.getInstance().player; var v = p == null ? null : p.getAttached(PlayerStamina.VIEW);
        return v != null && v.stamina() / v.capacity() <= 0.2;
    }
    /** Render-frame input path invokes vanilla release once, without ticking the player. */
    public static void frame(Minecraft c) {
        if (c.player == null) { reset(); return; }
        var view = c.player.getAttached(FocusTime.VIEW);
        if (view == null) view = FocusView.OFF;
        double seconds = clock.advance(System.nanoTime(), c.isPaused() || !view.active());
        if (view.active() && (!previous.active() || view.session() != previous.session())) bowSeconds = view.bowSeconds();
        else if (view.active()) {
            bowSeconds += seconds;
            // Only reconcile newly received server samples; the remaining time is presentation prediction.
            if (!view.equals(previous)) bowSeconds = view.bowSeconds();
        }
        previous = view;
        boolean active = visible();
        if (active != audibleActive) {
            if (!c.isPaused()) c.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, active ? 1.35F : 0.85F, 0.22F));
            audibleActive = active;
        }
        if (active && c.player.isUsingItem() && c.player.getUseItem().getItem() instanceof BowItem) {
            FocusTime.compensate(c.player, bowSeconds);
            if (c.gameMode != null && !c.options.keyUse.isDown()) c.gameMode.releaseUsingItem(c.player);
        }
    }
    public static Identifier effect() {
        var o = FocusOptions.get();
        return Wildcraft.id("focus/" + o.strength + "_" + (o.vignette ? 1 : 0) + "_" + (o.pattern ? 1 : 0));
    }
    public static List<Identifier> postEffects(List<Identifier> existing) {
        if (!visible() || FocusOptions.get().strength == 0) return existing;
        var result = new ArrayList<>(existing);
        result.add(effect());
        // Only an optional, gentle low-stamina pulse. Default is steady, with no flash.
        if (FocusOptions.get().lowStaminaPulse && lowStamina() && System.nanoTime() / 625_000_000L % 2 == 0)
            result.add(Wildcraft.id("focus/low_stamina"));
        return result;
    }
    public static float fovMultiplier() { return visible() && FocusOptions.get().strength > 0 && FocusOptions.get().fov ? 0.97F : 1; }
}
