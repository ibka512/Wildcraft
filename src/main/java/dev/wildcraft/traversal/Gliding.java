package dev.wildcraft.traversal;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.GlideInput;
import dev.wildcraft.network.GlideView;
import dev.wildcraft.network.StaminaView;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.player.GliderEquipment;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Native collision prediction with server-authorized equipment, fees and movement bounds. */
public final class Gliding {
    public static final double COST = 0.2;
    public static final double DESCENT = -0.08;
    public static final AttachmentType<GlideInput> INTENT = AttachmentRegistry.create(Wildcraft.id("glide_intent"));
    public static final AttachmentType<GlideView> VIEW = AttachmentRegistry.create(Wildcraft.id("glide_view"),
            builder -> builder.syncWith(GlideView.CODEC, AttachmentSyncPredicate.all()));
    private static final AttachmentType<Session> SESSION = AttachmentRegistry.create(Wildcraft.id("glide_session"));
    private static final int INPUT_TIMEOUT = 20;

    private static final class Session {
        long lastInput = Long.MIN_VALUE;
        long openedAt;
        long cooldownUntil;
        boolean active;
        boolean blocked;
        int selectedSlot;
        ItemStack mainHand = ItemStack.EMPTY;
        ItemStack offHand = ItemStack.EMPTY;
    }

    private Gliding() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(GlideInput.TYPE, GlideInput.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GlideInput.TYPE, (input, context) -> receive(context.player(), input));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(Gliding::tick));
        ServerPlayerEvents.JOIN.register(player -> reset(player, false));
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER, (oldPlayer, newPlayer, alive) -> reset(newPlayer, true));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> reset(player, true));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (entity instanceof ServerPlayer player && taken > 0 && !blocked && active(player)) {
                interrupt(player);
            }
        });
        UseItemCallback.EVENT.register((player, level, hand) -> closeForAction(player));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> closeForAction(player));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> closeForAction(player));
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> closeForAction(player));
    }

    public static boolean hasGlider(Player player) {
        return GliderEquipment.equipped(player);
    }

    public static boolean eligible(Player player) {
        if (!Climbing.eligible(player) || player.onGround() || player.getAbilities().mayfly
                || player.isAutoSpinAttack() || player.hasEffect(MobEffects.LEVITATION)) {
            return false;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (LivingEntity.canGlideUsing(player.getItemBySlot(slot), slot)) {
                return false;
            }
        }
        return hasGlider(player);
    }

    public static boolean active(Player player) {
        GlideView view = player.getAttached(VIEW);
        return view != null && view.active();
    }

    public static GlideInput intent(Player player) {
        GlideInput input = player.getAttached(INTENT);
        return input == null ? GlideInput.CLOSED : input;
    }

    public static void receive(ServerPlayer player, GlideInput input) {
        Session state = session(player);
        state.lastInput = player.level().getGameTime();
        player.setAttached(INTENT, input);
        if (!input.open()) {
            state.active = false;
            state.blocked = false;
        } else if (!state.active) {
            if (!state.blocked && player.level().getGameTime() >= state.cooldownUntil
                    && eligible(player) && PlayerStamina.get(player).stamina() > 0) {
                Climbing.reset(player, true);
                state.active = true;
                state.openedAt = player.level().getGameTime();
                state.selectedSlot = player.getInventory().getSelectedSlot();
                state.mainHand = player.getMainHandItem().copy();
                state.offHand = player.getOffhandItem().copy();
                player.stopUsingItem();
                player.resetFallDistance();
            } else {
                state.blocked = true;
            }
        }
        publish(player, state);
    }

    public static void tick(ServerPlayer player) {
        Session state = session(player);
        if (state.active && (player.level().getGameTime() - state.lastInput > INPUT_TIMEOUT
                || !intent(player).open() || !eligible(player) || handsChanged(player, state))) {
            state.active = false;
            state.blocked = true;
        }
        if (state.active) {
            PlayerStamina.consume(player, COST);
            player.resetFallDistance();
            if (PlayerStamina.get(player).stamina() <= 0) {
                state.active = false;
                state.blocked = true;
            }
        }
        publish(player, state);
    }

    public static boolean canPredict(Player player) {
        GlideView view = player.getAttached(VIEW);
        StaminaView stamina = player.getAttached(PlayerStamina.VIEW);
        return intent(player).open() && eligible(player) && stamina != null && stamina.stamina() > 0
                && (view == null || !view.blocked());
    }

    public static boolean movePredicted(Player player, Vec3 input) {
        if (!player.level().isClientSide() || !player.isLocalInstanceAuthoritative() || !canPredict(player)) {
            return false;
        }
        Vec3 movement = movement(player.getYRot(), input, dev.wildcraft.weather.WindSystem.local(player));
        player.setSprinting(false);
        player.resetFallDistance();
        player.setDeltaMovement(movement);
        player.move(MoverType.SELF, movement);
        // Retain modest forward momentum if the next tick closes the canopy.
        player.setDeltaMovement(new Vec3(movement.x * 0.8, 0, movement.z * 0.8));
        player.resetFallDistance();
        return true;
    }

    public static Vec3 movement(float yaw, Vec3 input) {
        double radians = Math.toRadians(yaw);
        double forward = 0.16 + Math.clamp(input.z, -1, 1) * 0.08;
        double side = Math.clamp(input.x, -1, 1) * 0.12;
        return new Vec3(-Math.sin(radians) * forward + Math.cos(radians) * side,
                DESCENT, Math.cos(radians) * forward + Math.sin(radians) * side);
    }

    public static Vec3 movement(float yaw, Vec3 input, dev.wildcraft.network.WindView wind) {
        return movement(yaw, input).add(wind.x(), -dev.wildcraft.weather.WindRules.descentPenalty(wind.precipitation()), wind.z());
    }

    /** Checked before native movement; never disables vanilla collision or flight checks. */
    public static boolean acceptMovement(ServerPlayer player, double x, double y, double z) {
        if (!active(player)) {
            return true;
        }
        Session state = session(player);
        double dy = y - player.getY();
        boolean settling = player.level().getGameTime() <= state.openedAt + 1;
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.hypot(x - player.getX(), z - player.getZ()) > 0.34
                || dy > (settling ? 0.5 : 0.03) || dy < (settling ? -1.5 : -0.18)) {
            interrupt(player);
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            return false;
        }
        player.resetFallDistance();
        return true;
    }

    public static void interrupt(ServerPlayer player) {
        Session state = session(player);
        state.active = false;
        state.blocked = true;
        state.cooldownUntil = player.level().getGameTime() + 10;
        publish(player, state);
    }

    public static void reset(ServerPlayer player, boolean releaseRequired) {
        player.removeAttached(SESSION);
        player.setAttached(INTENT, GlideInput.CLOSED);
        Session state = session(player);
        state.blocked = releaseRequired;
        publish(player, state);
    }

    private static Session session(Player player) {
        return player.getAttachedOrCreate(SESSION, Session::new);
    }

    private static boolean handsChanged(Player player, Session state) {
        return player.getInventory().getSelectedSlot() != state.selectedSlot
                || !ItemStack.matches(player.getMainHandItem(), state.mainHand)
                || !ItemStack.matches(player.getOffhandItem(), state.offHand);
    }

    private static InteractionResult closeForAction(Player player) {
        if (player instanceof ServerPlayer serverPlayer && active(player)) receive(serverPlayer, GlideInput.CLOSED);
        return InteractionResult.PASS;
    }

    private static void publish(ServerPlayer player, Session state) {
        GlideView view = new GlideView(state.active, state.blocked);
        if (!view.equals(player.getAttached(VIEW))) {
            player.setAttached(VIEW, view);
        }
    }
}
