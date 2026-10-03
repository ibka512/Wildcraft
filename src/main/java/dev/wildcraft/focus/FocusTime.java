package dev.wildcraft.focus;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.FocusView;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** Airborne bow skill. All time ownership, duration and fees belong to the server thread. */
public final class FocusTime {
    public static final float WORLD_RATE = 5;
    public static final double COST_PER_SECOND = 10;
    public static final double DESCENT_LIMIT = -0.15;
    public static final AttachmentType<FocusView> VIEW = AttachmentRegistry.create(Wildcraft.id("focus_view"),
            b -> b.syncWith(FocusView.CODEC, AttachmentSyncPredicate.targetOnly()));
    private static final AttachmentType<ItemStack> BLOCKED_USE = AttachmentRegistry.create(Wildcraft.id("focus_blocked_use"));
    private static final Map<MinecraftServer, Session> SESSIONS = new IdentityHashMap<>();
    private static long nextSession;
    private FocusTime() { }
    private static final class Session {
        final ServerPlayer player;
        final ItemStack bow;
        final TimeLease lease;
        final ActivePlayClock clock = new ActivePlayClock();
        final long id = ++nextSession;
        double bowSeconds;
        double maxStep;
        Session(ServerPlayer player, TimeLease lease) {
            this.player = player; this.bow = player.getUseItem(); this.lease = lease;
            bowSeconds = player.getTicksUsingItem() / 20.0;
            clock.advance(System.nanoTime(), false);
        }
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            pulse(server);
            if (!SESSIONS.containsKey(server)) {
                for (var player : server.getPlayerList().getPlayers()) tryBegin(player);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> close(server, false, false));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> close(server, false, false));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p, from, to) -> interrupt(p));
    }
    public static boolean allowedServer(MinecraftServer server) {
        return server.isSingleplayer() && !server.isDedicatedServer() && !server.isPublished()
                && server.getPlayerList().getPlayerCount() == 1;
    }
    public static boolean paused(MinecraftServer server) {
        return server.isPaused() || server instanceof FocusPauseSource source && source.wildcraft$focusPaused();
    }
    public static boolean eligible(Player p) {
        return p.isAlive() && !p.isRemoved() && !p.isSpectator() && !p.onGround()
                && !p.isPassenger() && !p.isSleeping() && !p.isInLiquid() && !p.isSwimming()
                && !p.getAbilities().mayfly && !p.getAbilities().flying && !p.isFallFlying()
                && !p.isAutoSpinAttack() && !p.isInPowderSnow && !p.hasEffect(MobEffects.LEVITATION)
                && !Climbing.active(p) && !Climbing.intent(p).held() && !Gliding.active(p)
                && p.isUsingItem() && p.getUseItem().getItem() instanceof BowItem
                && p.getUseItem() == p.getItemInHand(p.getUsedItemHand());
    }
    public static boolean active(Player p) {
        var view = p.getAttached(VIEW);
        return view != null && view.active();
    }
    public static boolean active(MinecraftServer server) { return SESSIONS.containsKey(server); }
    public static double maxStep(MinecraftServer server) {
        Session s = SESSIONS.get(server); return s == null ? 0 : s.maxStep;
    }
    public static boolean tryBegin(ServerPlayer p) {
        var server = p.level().getServer();
        if (!p.isUsingItem()) p.removeAttached(BLOCKED_USE);
        if (SESSIONS.containsKey(server) || !allowedServer(server) || paused(server) || !eligible(p)
                || PlayerStamina.get(p).stamina() <= 0 || p.getAttached(BLOCKED_USE) == p.getUseItem()) return false;
        TimeLease lease = TimeLease.acquire(server, WORLD_RATE);
        if (lease == null) return false;
        Session session = new Session(p, lease);
        SESSIONS.put(server, session);
        publish(session);
        return true;
    }
    /** Called between world ticks. Processes no entity/world/player tick. */
    public static void pulse(MinecraftServer server) {
        Session s = SESSIONS.get(server);
        if (s == null) return;
        if (paused(server)) { close(server, false, false); return; }
        if (!allowedServer(server) || !s.lease.ownsControl() || !eligible(s.player)
                || s.player.getUseItem() != s.bow || PlayerStamina.get(s.player).stamina() <= 0) {
            close(server, false, true); return;
        }
        charge(s);
        if (PlayerStamina.get(s.player).stamina() <= 0) close(server, false, true);
    }
    private static void charge(Session s) {
        double seconds = s.clock.advance(System.nanoTime(), false);
        s.maxStep = Math.max(s.maxStep, seconds);
        // The final partial interval cannot grant free charge once stamina is exhausted.
        double paid = PlayerStamina.consume(s.player, seconds * COST_PER_SECOND);
        s.bowSeconds += paid / COST_PER_SECOND;
        compensate(s.player, s.bowSeconds);
        publish(s);
    }
    public static void compensate(LivingEntity p, double seconds) {
        int duration = p.getUseItem().getUseDuration(p);
        int ticks = (int)Math.min(duration, Math.floor(Math.max(0, seconds) * 20));
        ((UsingItemAccess)p).wildcraft$bowRemaining(duration - ticks);
    }
    private static void publish(Session s) {
        s.player.setAttached(VIEW, new FocusView(true, s.id, s.bowSeconds));
    }
    public static void beforeRelease(LivingEntity entity) {
        if (entity instanceof ServerPlayer p) close(p.level().getServer(), true, false);
    }
    public static void stopped(LivingEntity entity) {
        if (entity instanceof ServerPlayer p) {
            close(p.level().getServer(), true, false);
            p.removeAttached(BLOCKED_USE);
        }
    }
    public static void interrupt(ServerPlayer p) { close(p.level().getServer(), true, true); }
    public static void close(MinecraftServer server, boolean billLastInterval, boolean blockCurrentUse) {
        Session s = SESSIONS.remove(server);
        if (s == null) return;
        if (billLastInterval && !paused(server) && s.player.isAlive() && s.player.getUseItem() == s.bow) charge(s);
        s.lease.close();
        if (blockCurrentUse && s.player.isUsingItem()) s.player.setAttached(BLOCKED_USE, s.player.getUseItem());
        s.player.setAttached(VIEW, FocusView.OFF);
    }
    public static void limitDescent(Player p) {
        if (active(p) && eligible(p) && p.getDeltaMovement().y < DESCENT_LIMIT) {
            var v = p.getDeltaMovement(); p.setDeltaMovement(v.x, DESCENT_LIMIT, v.z);
        }
    }
}
