package dev.wildcraft.traversal;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.ClimbInput;
import dev.wildcraft.network.ClimbView;
import dev.wildcraft.network.StaminaView;
import dev.wildcraft.player.PlayerStamina;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Client collision prediction, with server-owned eligibility, cost and grip locks. */
public final class Climbing {
    public static final double MOVING_COST = 0.4;
    public static final double HOLDING_COST = 0.1;
    public static final AttachmentType<ClimbInput> INTENT = AttachmentRegistry.create(Wildcraft.id("climb_intent"));
    public static final AttachmentType<ClimbView> VIEW = AttachmentRegistry.create(Wildcraft.id("climb_view"),
            builder -> builder.syncWith(ClimbView.CODEC, AttachmentSyncPredicate.targetOnly()));
    private static final AttachmentType<Session> SESSION = AttachmentRegistry.create(Wildcraft.id("climb_session"));
    private static final int INPUT_TIMEOUT = 20;

    private Climbing() {
    }

    private static final class Session {
        Direction wall;
        long lastInput = Long.MIN_VALUE;
        long cooldownUntil;
        boolean active;
        boolean blocked;
        boolean moved;
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ClimbInput.TYPE, ClimbInput.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ClimbInput.TYPE, (input, context) -> receive(context.player(), input));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(Climbing::tick));
        ServerPlayerEvents.JOIN.register(player -> reset(player, false));
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER, (oldPlayer, newPlayer, alive) -> reset(newPlayer, true));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> reset(player, true));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (entity instanceof ServerPlayer player && taken > 0 && !blocked && active(player)) {
                interrupt(player);
            }
        });
    }

    public static void receive(ServerPlayer player, ClimbInput input) {
        if (!input.valid()) {
            interrupt(player);
            return;
        }
        Session state = session(player);
        state.lastInput = player.level().getGameTime();
        player.setAttached(INTENT, input.held() ? input : ClimbInput.RELEASED);
        if (!input.held()) {
            state.blocked = false;
            state.active = false;
            state.wall = null;
        }
        updateEligibility(player, state);
        publish(player, state);
    }

    public static void tick(ServerPlayer player) {
        Session state = session(player);
        if (state.lastInput != Long.MIN_VALUE && player.level().getGameTime() - state.lastInput > INPUT_TIMEOUT) {
            player.setAttached(INTENT, ClimbInput.RELEASED);
            state.active = false;
            state.wall = null;
        }
        updateEligibility(player, state);
        if (state.active) {
            ClimbInput input = intent(player);
            PlayerStamina.consume(player, state.moved || input.vertical() != 0 || input.sideways() != 0 ? MOVING_COST : HOLDING_COST);
            player.resetFallDistance();
            if (PlayerStamina.get(player).stamina() <= 0) {
                state.active = false;
                state.blocked = true;
            }
        }
        state.moved = false;
        publish(player, state);
    }

    private static void updateEligibility(ServerPlayer player, Session state) {
        ClimbInput input = intent(player);
        if (!input.held() || state.blocked || player.level().getGameTime() < state.cooldownUntil || !eligible(player) || Gliding.active(player)) {
            state.active = false;
            return;
        }
        if (PlayerStamina.get(player).stamina() <= 0) {
            state.active = false;
            state.blocked = true;
            return;
        }
        ClimbSurface.Contact contact = ClimbSurface.find(player, state.wall, state.active, input.vertical() > 0, input.sideways());
        state.active = contact != null;
        state.wall = contact == null ? null : contact.cornerFrom() == null ? contact.face() : contact.cornerFrom();
    }

    public static boolean eligible(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
                && !player.isInLiquid() && !player.isSwimming() && !player.isFallFlying()
                && !player.getAbilities().flying && !player.isInPowderSnow && player.hurtTime == 0;
    }

    public static ClimbInput intent(Player player) {
        ClimbInput input = player.getAttached(INTENT);
        return input == null ? ClimbInput.RELEASED : input;
    }

    public static boolean active(Player player) {
        ClimbView view = player.getAttached(VIEW);
        return view != null && view.active();
    }

    public static boolean movePredicted(Player player) {
        if (!player.level().isClientSide() || !player.isLocalInstanceAuthoritative()) {
            return false;
        }
        Session state = session(player);
        ClimbInput input = intent(player);
        ClimbView view = player.getAttached(VIEW);
        StaminaView stamina = player.getAttached(PlayerStamina.VIEW);
        if (!input.held() || !eligible(player) || Gliding.canPredict(player) || stamina == null || stamina.stamina() <= 0 || view != null && view.blocked()) {
            state.active = false;
            state.wall = null;
            return false;
        }
        ClimbSurface.Contact contact = ClimbSurface.find(player, state.wall, state.active, input.vertical() > 0, input.sideways());
        if (contact == null) {
            state.active = false;
            state.wall = null;
            return false;
        }
        state.active = true;
        state.wall = contact.cornerFrom() == null ? contact.face() : contact.cornerFrom();
        Vec3 movement = movement(input, contact);
        player.setSprinting(false);
        player.resetFallDistance();
        player.setDeltaMovement(movement);
        player.move(MoverType.SELF, movement);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        return true;
    }

    public static Vec3 movement(ClimbInput input, ClimbSurface.Contact contact) {
        Direction face = contact.face();
        if (contact.cornerFrom() != null) {
            Direction from = contact.cornerFrom();
            double length = Math.max(1, Math.hypot(input.vertical(), input.sideways()));
            double side = input.sideways() * 0.08 / length;
            return new Vec3(from.getStepX() * 0.06 + from.getStepZ() * side,
                    input.vertical() * 0.12 / length, from.getStepZ() * 0.06 - from.getStepX() * side);
        }
        if (contact.mantle() && input.vertical() > 0) {
            return new Vec3(face.getStepX() * 0.18, 0.06, face.getStepZ() * 0.18);
        }
        double length = Math.max(1, Math.hypot(input.vertical(), input.sideways()));
        double side = input.sideways() * 0.08 / length;
        return new Vec3(face.getStepX() * 0.025 + face.getStepZ() * side,
                input.vertical() * 0.12 / length, face.getStepZ() * 0.025 - face.getStepX() * side);
    }

    /** Extra bounds while authorized; vanilla collision and movement checks still run. */
    public static boolean acceptMovement(ServerPlayer player, double x, double y, double z) {
        if (!active(player)) {
            return true;
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(y - player.getY()) > 0.16 || Math.hypot(x - player.getX(), z - player.getZ()) > 0.24) {
            interrupt(player);
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            return false;
        }
        if (Math.abs(y - player.getY()) > 0.01 || Math.hypot(x - player.getX(), z - player.getZ()) > 0.01) {
            session(player).moved = true;
        }
        player.resetFallDistance();
        return true;
    }

    public static void interrupt(ServerPlayer player) {
        Session state = session(player);
        state.active = false;
        state.blocked = true;
        state.wall = null;
        state.cooldownUntil = player.level().getGameTime() + 10;
        publish(player, state);
    }

    public static void reset(ServerPlayer player, boolean releaseRequired) {
        player.removeAttached(SESSION);
        player.setAttached(INTENT, ClimbInput.RELEASED);
        Session state = session(player);
        state.blocked = releaseRequired;
        publish(player, state);
    }

    private static Session session(Player player) {
        return player.getAttachedOrCreate(SESSION, Session::new);
    }

    private static void publish(ServerPlayer player, Session state) {
        ClimbView view = new ClimbView(state.active, state.blocked);
        if (!view.equals(player.getAttached(VIEW))) {
            player.setAttached(VIEW, view);
        }
    }
}
