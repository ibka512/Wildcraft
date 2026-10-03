package dev.wildcraft.temperature;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.TemperatureView;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/** Transient environment readout. Never modifies health, movement, freezing, stamina or saved progress. */
public final class EnvironmentTemperature {
    public static final AttachmentType<TemperatureView> VIEW = AttachmentRegistry.create(Wildcraft.id("temperature_view"),
            b -> b.syncWith(TemperatureView.CODEC, AttachmentSyncPredicate.targetOnly()));
    private EnvironmentTemperature() { }
    public static void initialize() {
        ServerPlayerEvents.JOIN.register(EnvironmentTemperature::publish);
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER, (old, player, alive) -> publish(player));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> publish(player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var p : server.getPlayerList().getPlayers()) {
                if (!p.isAlive() || p.isSpectator()) { p.removeAttached(VIEW); continue; }
                if (!p.hasAttached(VIEW) || Math.floorMod(p.tickCount + p.getId(), TemperatureRules.SAMPLE_TICKS) == 0) publish(p);
            }
        });
    }
    public static void publish(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.level().hasChunkAt(player.blockPosition())) {
            player.removeAttached(VIEW); return;
        }
        // Quantization keeps ordinary stationary reads silent and packets bounded by the sample frequency.
        float target = Math.round(TemperatureSampler.sample(player).target() * 100) / 100F;
        var view = new TemperatureView(target);
        if (!view.equals(player.getAttached(VIEW))) player.setAttached(VIEW, view);
    }
}
