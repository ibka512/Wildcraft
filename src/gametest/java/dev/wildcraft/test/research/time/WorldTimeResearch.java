package dev.wildcraft.test.research.time;

import dev.wildcraft.player.PlayerStamina;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;

/** Explicitly armed by R1 tests. There is no automatic skill in the game mod. */
public final class WorldTimeResearch {
    private static final Map<MinecraftServer, Session> SESSIONS = new IdentityHashMap<>();
    public static volatile UUID clientOwner;
    public static volatile boolean compensated;
    private static double clientElapsed;
    private static final ActivePlayClock CLIENT_CLOCK = new ActivePlayClock();

    private static final class Session {
        final ServerPlayer player;
        final TimeLease lease;
        final ActivePlayClock clock = new ActivePlayClock();
        final boolean compensation;
        double elapsed;
        double maxStep;
        Session(ServerPlayer player, TimeLease lease, boolean compensation) {
            this.player = player;
            this.lease = lease;
            this.compensation = compensation;
            clock.advance(System.nanoTime(), false);
        }
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Session session = SESSIONS.get(server);
            if (session == null) return;
            ServerPlayer p = session.player;
            if (!session.lease.ownsControl() || !p.isAlive() || p.isRemoved() || p.onGround()
                    || !p.isUsingItem() || !(p.getUseItem().getItem() instanceof BowItem)
                    || server.isPublished() || server.getPlayerList().getPlayerCount() != 1) {
                close(server);
                return;
            }
            double seconds = session.clock.advance(System.nanoTime(), server.isPaused());
            session.maxStep = Math.max(session.maxStep, seconds);
            session.elapsed += seconds;
            PlayerStamina.consume(p, seconds * 10);
            if (session.compensation) compensate(p, session.elapsed);
            if (PlayerStamina.get(p).stamina() <= 0) close(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(WorldTimeResearch::close);
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p, origin, destination) -> close(p.level().getServer()));
    }

    public static boolean begin(ServerPlayer p, float rate, boolean compensation) {
        MinecraftServer server = p.level().getServer();
        close(server);
        dev.wildcraft.focus.FocusTime.interrupt(p); // Explicit research takes over this bow use.
        if (p.onGround() || !p.isAlive() || !p.isUsingItem() || !(p.getUseItem().getItem() instanceof BowItem)
                || PlayerStamina.get(p).stamina() <= 0) return false;
        TimeLease lease = TimeLease.acquire(server, rate);
        if (lease == null) return false;
        Session session = new Session(p, lease, compensation);
        session.elapsed = p.getTicksUsingItem() / 20.0;
        SESSIONS.put(server, session);
        clientElapsed = session.elapsed;
        CLIENT_CLOCK.advance(System.nanoTime(), true);
        compensated = compensation;
        clientOwner = p.getUUID();
        return true;
    }

    public static boolean active(MinecraftServer server) { return SESSIONS.containsKey(server); }
    public static double maxStep(MinecraftServer server) {
        Session session = SESSIONS.get(server);
        return session == null ? 0 : session.maxStep;
    }
    public static boolean hasCompensation(MinecraftServer server) {
        Session session = SESSIONS.get(server);
        return session != null && session.compensation && session.lease.ownsControl()
                && !server.isPublished() && server.getPlayerList().getPlayerCount() == 1;
    }
    public static boolean active(Player p) { return p.getUUID().equals(clientOwner); }

    public static void close(MinecraftServer server) {
        Session session = SESSIONS.remove(server);
        if (session != null) {
            session.lease.close();
            clientOwner = null;
            compensated = false;
        }
    }

    public static void beforeRelease(LivingEntity entity) {
        if (entity instanceof ServerPlayer p) {
            Session session = SESSIONS.get(p.level().getServer());
            if (session != null && session.player == p) {
                if (session.compensation) {
                    double seconds = session.clock.advance(System.nanoTime(), false);
                    session.elapsed += seconds;
                    PlayerStamina.consume(p, seconds * 10);
                    compensate(p, session.elapsed);
                }
                close(p.level().getServer());
            }
        }
    }

    public static void clientTick(Player player, boolean paused) {
        double seconds = CLIENT_CLOCK.advance(System.nanoTime(), paused || !active(player));
        if (compensated && active(player) && player.isUsingItem() && player.getUseItem().getItem() instanceof BowItem) {
            clientElapsed += seconds;
            compensate(player, clientElapsed);
        }
    }

    private static void compensate(LivingEntity entity, double elapsed) {
        int used = (int) Math.floor(elapsed * 20);
        ((UsingItemAccess) entity).wildcraftResearch$remaining(72000 - used);
    }
}
