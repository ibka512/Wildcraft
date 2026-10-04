package dev.wildcraft.client.mechanics;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.energy.*;
import dev.wildcraft.mechanics.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;

public final class MachinePresentation {
    public static final KeyMapping TOGGLE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.wildcraft.machine_toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_R,
            KeyMapping.Category.register(Wildcraft.id("mechanics"))));
    private MachinePresentation() { }
    public static int selectedNode(MachineEntity body) {
        var c = Minecraft.getInstance();
        if (c.player == null || c.player.isSpectator() || c.gui.screen() != null || c.gui.hud.isHidden()) return -1;
        if (!(c.hitResult instanceof EntityHitResult hit) || hit.getEntity() != body || c.player.distanceToSqr(body) > 25) return -1;
        var relative = hit.getLocation().subtract(body.position());
        if (MachineNodes.panel(relative, body.getYRot())) return -1;
        return MachineNodes.nearest(relative, body.getYRot());
    }
    public static boolean canPreview(MachineEntity body, int node) {
        var p = Minecraft.getInstance().player;
        return p != null && node >= 0 && !body.enabled() && body.kind(node) == 0 && MachineEntity.accepts(p.getMainHandItem())
                && (!p.getMainHandItem().is(EnergyContent.BATTERY) || body.batteryNode() < 0);
    }
    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(c -> {
            while (TOGGLE.consumeClick()) if (c.player != null && c.player.getVehicle() instanceof MachineEntity
                    && c.gui.screen() == null && ClientPlayNetworking.canSend(MachineToggle.TYPE)) ClientPlayNetworking.send(new MachineToggle());
        });
        HudElementRegistry.attachElementAfter(Wildcraft.id("meals"), Wildcraft.id("machine"), (g, t) -> {
            var c = Minecraft.getInstance(); var p = c.player;
            if (p == null || !p.isAlive() || p.isSpectator() || c.gui.hud.isHidden() || c.gui.screen() != null) return;
            MachineEntity body = p.getVehicle() instanceof MachineEntity vehicle ? vehicle
                    : c.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof MachineEntity target ? target : null;
            if (body == null || p.distanceToSqr(body) > 25) return;
            int node = selectedNode(body);
            String help = p.getVehicle() == body ? "drive_hint"
                    : MachineEntity.accepts(p.getMainHandItem()) ? "install_hint" : "ground_hint";
            Component label = Component.translatable("machine.wildcraft.status", body.energy(), body.enabled()
                    ? Component.translatable(body.working() ? "machine.wildcraft.running" : "machine.wildcraft.waiting")
                    : Component.translatable("machine.wildcraft.off"));
            Component hint = Component.translatable("machine.wildcraft." + help, TOGGLE.getTranslatedKeyMessage());
            if (node >= 0) hint = Component.translatable("machine.wildcraft.node_hint", Component.translatable("machine.wildcraft.node." + node), hint);
            int maxWidth = Math.max(40, g.guiWidth() - 24);
            var lines = new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.addAll(c.font.split(label, maxWidth)); lines.addAll(c.font.split(hint, maxWidth));
            int width = Math.min(maxWidth, Math.max(c.font.width(label), c.font.width(hint)));
            int x = (g.guiWidth() - width) / 2, y = Math.max(4, g.guiHeight() - 78 - lines.size() * 10);
            g.fill(x - 5, y - 4, x + width + 5, y + lines.size() * 10, 0xB5223030);
            for (var line : lines) { g.text(c.font, line, x, y, 0xFFE7DEC3); y += 10; }
        });
    }
}
