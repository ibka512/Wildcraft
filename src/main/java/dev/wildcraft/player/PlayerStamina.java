package dev.wildcraft.player;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.StaminaView;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerStamina {
    public static final AttachmentType<StaminaData> DATA = AttachmentRegistry.create(
            Wildcraft.id("player_stamina"), builder -> builder.persistent(StaminaData.CODEC).copyOnDeath());
    public static final AttachmentType<StaminaView> VIEW = AttachmentRegistry.create(
            Wildcraft.id("stamina_view"), builder -> builder.syncWith(StaminaView.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

    private PlayerStamina() {
    }

    public static void initialize() {
        ServerPlayerEvents.JOIN.register(player -> {
            recordExperience(player);
            publish(player);
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            StaminaData copied = get(oldPlayer).observeLevel(newPlayer.experienceLevel);
            newPlayer.setAttached(DATA, copied);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER, (oldPlayer, newPlayer, alive) -> {
            // Fabric's automatic attachment transfer happens in AFTER_RESPAWN,
            // after COPY_FROM. The explicit event phase runs after that transfer,
            // independently of Fabric/Mod initializer registration order.
            StaminaData copied = get(oldPlayer).observeLevel(newPlayer.experienceLevel);
            newPlayer.setAttached(DATA, alive ? copied : copied.filled());
            publish(newPlayer);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
    }

    public static StaminaData get(ServerPlayer player) {
        return player.getAttachedOrCreate(DATA, () -> StaminaData.initial(player.experienceLevel));
    }

    public static void recordExperience(ServerPlayer player) {
        StaminaData before = get(player);
        StaminaData after = before.observeLevel(player.experienceLevel);
        if (!after.equals(before)) {
            player.setAttached(DATA, after);
            publish(player);
        }
    }

    /** Hooks must not create a fresh attachment while Minecraft loads or copies a player. */
    public static void experienceChanged(ServerPlayer player) {
        if (player.hasAttached(DATA)) {
            recordExperience(player);
        }
    }

    public static void tick(ServerPlayer player) {
        StaminaData before = get(player);
        StaminaData after = before.observeLevel(player.experienceLevel).tick(canRecover(player));
        if (!after.equals(before)) {
            player.setAttached(DATA, after);
        }
        if (player.tickCount % StaminaRules.SYNC_INTERVAL_TICKS == 0
                || after.highestLevel() != before.highestLevel() || !player.hasAttached(VIEW)) {
            publish(player);
        }
    }

    public static boolean canRecover(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && player.onGround() && !Climbing.active(player) && !Gliding.active(player)
                && !player.isInWater() && !player.getAbilities().flying && !player.isFallFlying();
    }

    /** Returns the actual amount consumed, clamped to what the player had left. */
    public static double consume(ServerPlayer player, double amount) {
        StaminaData before = get(player);
        StaminaData after = before.consume(amount);
        if (!after.equals(before)) {
            player.setAttached(DATA, after);
            publish(player);
        }
        return before.stamina() - after.stamina();
    }

    public static void fill(ServerPlayer player) {
        player.setAttached(DATA, get(player).filled());
        publish(player);
    }

    private static void publish(ServerPlayer player) {
        StaminaView view = StaminaView.of(get(player));
        if (!view.equals(player.getAttached(VIEW))) {
            player.setAttached(VIEW, view);
        }
    }
}
