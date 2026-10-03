package dev.wildcraft.test.equipment;

import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.traversal.Gliding;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Ordinary graphic client loop, never the controlled Fabric gametest scheduler. */
public final class BackNativeClientProbe {
    private static int seen = -1, localStage = -1;
    private static long due, started, reconnectAt;
    private static boolean sent, reconnected, actionDone;
    public static boolean holdingUse() { return !System.getProperty("wildcraft.p31.role", "").isEmpty() && (localStage == 1 || localStage == 2) && !sent; }
    public static void frame(Minecraft c) {
        String role = System.getProperty("wildcraft.p31.role", "");
        if (role.isEmpty()) return;
        long now = System.nanoTime();
        if (started == 0) started = now;
        if (now - started > 180_000_000_000L) throw new AssertionError("P3.1 ordinary " + role + " timed out at " + localStage);
        if (reconnectAt != 0 && now >= reconnectAt) {
            reconnectAt = 0;
            ConnectScreen.startConnecting(new TitleScreen(), c, ServerAddress.parseString("127.0.0.1:25631"), new ServerData("P31 loopback", "127.0.0.1:25631", ServerData.Type.OTHER), false, null);
        }
        if (c.player == null || c.level == null || c.gui.screen() != null || c.gui.overlay() != null) return;
        c.getTutorial().stop(); c.gui.toastManager().clear();
        c.options.pauseOnLostFocus = false; c.options.framerateLimit().set(30); c.options.enableVsync().set(false);
        c.options.setCameraType(CameraType.FIRST_PERSON);
        Player actor = role.equals("actor") ? c.player : c.level.players().stream().filter(p -> p.getGameProfile().name().equals("WCActor")).findFirst().orElse(null);
        int stage = actor == null ? localStage == 7 ? 8 : localStage : actor.getAttachedOrElse(BackMultiplayerProbe.STAGE, -1);
        if (stage < 0) return;
        if (stage != localStage) {
            localStage = stage; due = now + 1_600_000_000L; sent = false; actionDone = false;
            if (role.equals("actor")) {
                if (stage <= 4) c.player.getInventory().setSelectedSlot(stage == 1 ? 1 : stage == 2 ? 2 : stage == 3 ? 3 : 0);
                if (stage == 12) { dev.wildcraft.client.input.ClimbControls.CLIMB.setDown(true); c.options.keyUp.setDown(true); }
                if (stage == 13) { dev.wildcraft.client.input.ClimbControls.CLIMB.setDown(false); c.options.keyUp.setDown(false); }
                if (stage == 5) { c.player.setOnGround(false); c.options.keyJump.setDown(true); }
            }
            System.out.println("WILDCRAFT P3.1 native " + role + " stage=" + stage + " fabric.client.gametest=" + System.getProperty("fabric.client.gametest"));
        }
        if (role.equals("actor")) {
            if (stage <= 2 && !actionDone) {
                if (stage == 0 && c.player.getMainHandItem().is(Items.DIAMOND_SWORD)) {
                    var cow = java.util.stream.StreamSupport.stream(c.level.entitiesForRendering().spliterator(), false).filter(e -> e.getType() == EntityTypes.COW && e.distanceTo(c.player) < 4).findFirst().orElse(null);
                    if (cow != null) { c.gameMode.attack(c.player, cow); actionDone = true; due = now + 1_600_000_000L; }
                } else if (stage == 1 && c.player.getMainHandItem().is(Items.SHIELD) || stage == 2 && c.player.getMainHandItem().is(Items.BOW)) {
                    c.options.keyUse.setDown(true); c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND); actionDone = true; due = now + 1_600_000_000L;
                }
            }
            if (stage == 5 && now > due - 1_400_000_000L) c.options.keyJump.setDown(false);
            if (stage == 8) {
                if (c.level.dimension().equals(net.minecraft.world.level.Level.NETHER) && actor.getAttachedOrElse(BackMultiplayerProbe.ACK, -1) == stage && now >= due && !sent) { c.getConnection().sendCommand("p31next"); sent = true; }
            } else if (stage == 14) {
                if (now >= due) c.stop();
            } else if (!sent && now >= due && (stage <= 2 && actionDone || actor.getAttachedOrElse(BackMultiplayerProbe.ACK, -1) == stage)) {
                c.options.keyUse.setDown(false); if (c.player.isUsingItem()) c.gameMode.releaseUsingItem(c.player);
                if (stage >= 3) screenshot(c, "actor-" + stage);
                c.getConnection().sendCommand("p31next"); sent = true;
            }
        } else {
            if (actor != null) {
                double distance = Math.hypot(actor.getX() - c.player.getX(), actor.getZ() - c.player.getZ());
                c.player.setXRot((float)-Math.toDegrees(Math.atan2(actor.getY() + 1.0 - c.player.getEyeY(), distance)));
            }
            if (stage == 10 && !reconnected && now >= due) {
                reconnected = true; reconnectAt = now + 2_000_000_000L;
                c.level.disconnect(net.minecraft.network.chat.Component.literal("P3.1 reconnect fixture"));
                c.disconnect(new TitleScreen(), false); return;
            }
            boolean correct = stage < 3 || actor == null && stage == 8 || actor != null && expected(actor, stage);
            if (stage <= 13 && correct && now >= due && !sent) {
                if (actor != null) check(!actor.hasAttached(BackEquipment.DATA) && !actor.hasAttached(dev.wildcraft.traversal.Climbing.VIEW) && !actor.hasAttached(dev.wildcraft.player.PlayerStamina.VIEW), "Observer never receives persistent private signatures");
                if (stage >= 3 && stage <= 13) screenshot(c, "observer-" + stage);
                c.getConnection().sendCommand("p31observe " + stage); sent = true; seen = stage;
                System.out.println("WILDCRAFT P3.1 observer acknowledged=" + stage + " reconnected=" + reconnected);
            }
            if (stage == 14 && now >= due) { check(seen == 13 && reconnected, "Observer completed reconnect and final tracking"); c.stop(); }
        }
    }
    private static boolean expected(Player p, int s) {
        var v = BackEquipment.view(p);
        var ownership=p.getAttached(BackEquipment.VISUAL);
        if (s==12 && p.getAttached(dev.wildcraft.traversal.Climbing.VISUAL)!=null && p.getAttached(dev.wildcraft.traversal.Climbing.VISUAL).active()) {
            var client=Minecraft.getInstance();
            var state=(net.minecraft.client.renderer.entity.state.AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(p).createRenderState(p,1);
            check(Math.abs(state.yRot)<10,"Looking toward the east wall preserves straight head alignment across -90/270 yaw");
        }
        if (s>=3 && ownership==null) return false;
        if (s==3 && (ownership.melee().owner()!=3 || ownership.shield().owner()!=3 || ownership.ranged().owner()!=3)) return false;
        if (s==4 && ownership.melee().owner()!=1 || s==5 && ownership.melee().owner()!=3 || s>=6 && ownership.melee().owner()!=0) return false;
        return switch (s) {
            case 3 -> v.melee().is(Items.DIAMOND_SWORD) && v.shield().is(Items.SHIELD) && v.ranged().is(Items.BOW);
            case 4 -> v.melee().isEmpty() && !v.shield().isEmpty() && !v.ranged().isEmpty();
            case 5 -> Gliding.active(p) && !v.melee().isEmpty() && !v.shield().isEmpty() && !v.ranged().isEmpty();
            case 6 -> v.melee().isEmpty() && !v.shield().isEmpty() && !v.ranged().isEmpty();
            case 7, 9, 10, 11 -> v.melee().isEmpty() && v.shield().isEmpty() && v.ranged().is(Items.BOW);
            case 12 -> p.getAttached(dev.wildcraft.traversal.Climbing.VISUAL)!=null && p.getAttached(dev.wildcraft.traversal.Climbing.VISUAL).active()
                    && p.getAttached(dev.wildcraft.traversal.Climbing.VISUAL).face()==net.minecraft.core.Direction.EAST.ordinal()
                    && p.getAttached(dev.wildcraft.traversal.Climbing.VISUAL).motion()==1;
            case 13 -> p.getAttached(dev.wildcraft.traversal.Climbing.VISUAL)!=null && !p.getAttached(dev.wildcraft.traversal.Climbing.VISUAL).active();
            default -> true;
        };
    }
    private static void screenshot(Minecraft c, String name) {
        Screenshot.grab(c.gameDirectory, "p31-two-client-" + name + ".png", c.gameRenderer.mainRenderTarget(), 1, msg -> System.out.println("WILDCRAFT P3.1 screenshot " + name + " " + msg.getString()));
    }
    private static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
