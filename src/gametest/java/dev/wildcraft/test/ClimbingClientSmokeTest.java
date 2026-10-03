package dev.wildcraft.test;

import dev.wildcraft.client.input.ClimbControls;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.traversal.Climbing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;

/** Uses real key bindings, native movement packets and collision, without teleporting climb steps. */
public final class ClimbingClientSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext c) {
        release(c);
        try (TestSingleplayerContext world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("time set day");
            world.getServer().runOnServer(server -> {
                arena(server.overworld());
                pose(server.getPlayerList().getPlayers().getFirst(), 71);
            });
            waitPose(c, 71);
            language(c, "zh_cn");
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.getInput().holdKey(options -> options.keyUp);
            c.waitTicks(20);
            check(c.computeOnClient(client -> Climbing.active(client.player) && client.player.getY() > 72.5), "Keys climb the actual wall");
            c.waitFor(client -> client.player.getAttached(Climbing.VISUAL)!=null && client.player.getAttached(Climbing.VISUAL).motion()==1);
            c.getInput().releaseKey(options -> options.keyUp);
            double heldY = c.computeOnClient(client -> client.player.getY());
            double heldCost = world.getServer().computeOnServer(server -> PlayerStamina.get(server.getPlayerList().getPlayers().getFirst()).stamina());
            c.waitTicks(12);
            check(Math.abs(c.computeOnClient(client -> client.player.getY()) - heldY) < 0.15, "Holding does not slide down");
            world.getServer().runOnServer(server -> check(PlayerStamina.get(server.getPlayerList().getPlayers().getFirst()).stamina() < heldCost, "Holding consumes server stamina"));
            double z = c.computeOnClient(client -> client.player.getZ());
            c.getInput().holdKeyFor(options -> options.keyLeft, 8);
            check(c.computeOnClient(client -> client.player.getZ()) < z - 0.3, "Horizontal climb moves along the wall");
            double y = c.computeOnClient(client -> client.player.getY());
            c.getInput().holdKeyFor(options -> options.keyDown, 6);
            check(c.computeOnClient(client -> client.player.getY()) < y - 0.4, "Backward key descends");
            c.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            c.waitTicks(3);
            c.takeScreenshot("p2-climbing-wall-zh");
            double dropY = c.computeOnClient(client -> client.player.getY());
            release(c);
            c.waitTicks(6);
            check(c.computeOnClient(client -> client.player.getY()) < dropY - 0.3, "Releasing grip restores gravity");

            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 78.8));
            waitPose(c, 78.8);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.getInput().holdKey(options -> options.keyUp);
            c.waitFor(client -> client.player.getY() >= 83 && client.player.getX() > 2.35 && client.player.onGround(), 100);
            c.takeScreenshot("p2-climbing-over-ledge");
            release(c);
            c.waitTicks(3);

            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 75, 5.6));
            waitPose(c, 75);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.getInput().holdKeyFor(options -> options.keyRight, 25);
            check(c.computeOnClient(client -> Climbing.active(client.player) && client.player.getX() > 2.25
                    && client.player.getY() > 74.8), "Actual sideways movement rounds the outside corner");
            c.takeScreenshot("p2-climbing-outer-corner");
            release(c);
            c.waitTicks(3);
            world.getServer().runOnServer(server -> {
                for (int x = -3; x < 2; x++) for (int y0 = 71; y0 <= 82; y0++) {
                    server.overworld().setBlockAndUpdate(new BlockPos(x, y0, 6), Blocks.STONE.defaultBlockState());
                }
                pose(server.getPlayerList().getPlayers().getFirst(), 75, 5.6);
            });
            waitPose(c, 75);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.getInput().holdKeyFor(options -> options.keyRight, 25);
            check(c.computeOnClient(client -> Climbing.active(client.player) && client.player.getX() < 1.2
                    && client.player.getZ() < 5.72 && client.player.getY() > 74.8), "Actual sideways movement turns inside corner without penetration");
            release(c);
            c.waitTicks(3);

            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                for (int x = 0; x < 2; x++) for (int z0 = -2; z0 <= 2; z0++) level.setBlockAndUpdate(new BlockPos(x, 77, z0), Blocks.STONE.defaultBlockState());
                pose(server.getPlayerList().getPlayers().getFirst(), 74.5);
            });
            waitPose(c, 74.5);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.getInput().holdKeyFor(options -> options.keyUp, 20);
            check(c.computeOnClient(client -> client.player.getBoundingBox().maxY) <= 77.001, "Ceiling collision stops upward motion");
            release(c);
            c.waitTicks(3);
            world.getServer().runOnServer(server -> {
                for (int x = 0; x < 2; x++) for (int z0 = -2; z0 <= 2; z0++) server.overworld().setBlockAndUpdate(new BlockPos(x, 77, z0), Blocks.AIR.defaultBlockState());
                pose(server.getPlayerList().getPlayers().getFirst(), 75);
            });
            waitPose(c, 75);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.waitFor(client -> Climbing.active(client.player));
            world.getServer().runCommand("damage @p 1 minecraft:generic");
            c.waitFor(client -> client.player.getAttached(Climbing.VIEW).blocked());
            double hurtY = c.computeOnClient(client -> client.player.getY());
            c.waitTicks(6);
            check(c.computeOnClient(client -> client.player.getY()) < hurtY - 0.3, "Actual damage drops the player");
            world.getServer().runOnServer(server -> check(!Climbing.active(server.getPlayerList().getPlayers().getFirst()), "Held key cannot instantly re-grab after damage"));
            release(c);
            c.waitTicks(15);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                pose(player, 75);
                PlayerStamina.consume(player, PlayerStamina.get(player).stamina() - 0.8);
            });
            waitPose(c, 75);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.getInput().holdKey(options -> options.keyUp);
            c.waitFor(client -> client.player.getAttached(Climbing.VIEW).blocked());
            world.getServer().runOnServer(server -> check(PlayerStamina.get(server.getPlayerList().getPlayers().getFirst()).stamina() == 0, "Actual climb exhausts server stamina"));
            double exhaustedY = c.computeOnClient(client -> client.player.getY());
            c.waitTicks(6);
            check(c.computeOnClient(client -> client.player.getY()) < exhaustedY - 0.3, "Exhaustion restores gravity");
            release(c);
            c.waitTicks(3);
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 75));
            waitPose(c, 75);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.waitFor(client -> Climbing.active(client.player));
            world.getServer().runCommand("kill @p");
            c.waitFor(client -> !client.player.isAlive());
            release(c);
            c.runOnClient(client -> client.player.respawn());
            c.waitFor(client -> client.player.isAlive() && !Climbing.active(client.player));
            world.getServer().runOnServer(server -> check(!Climbing.active(server.getPlayerList().getPlayers().getFirst()), "Death/respawn discards grip"));
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 75));
            waitPose(c, 75);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.waitFor(client -> Climbing.active(client.player));
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                check(p.teleportTo(server.getLevel(Level.NETHER), 0, 120, 0, Set.of(), 0, 0, true), "Dimension transfer accepted");
            });
            c.waitFor(client -> client.level.dimension().equals(Level.NETHER));
            world.getServer().runOnServer(server -> check(!Climbing.active(server.getPlayerList().getPlayers().getFirst()), "Dimension transfer ends grip"));
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                check(p.teleportTo(server.overworld(), 1.7, 75, 0.5, Set.of(), -90, 0, true), "Return transfer accepted");
            });
            c.waitFor(client -> client.level.dimension().equals(Level.OVERWORLD) && Math.abs(client.player.getX() - 1.7) < 0.1);
            c.waitTicks(3);
            world.getServer().runOnServer(server -> check(!Climbing.active(server.getPlayerList().getPlayers().getFirst()), "Held grip cannot carry through dimension change"));
            release(c);
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 71));
            language(c, "en_us");
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        }

        try (TestDedicatedServerContext server = c.worldBuilder().createServer(properties())) {
            server.runOnServer(instance -> arena(instance.overworld()));
            try (TestDedicatedServerConnection connection = server.connect()) {
                connection.waitForChunksRender();
                server.runOnServer(instance -> pose(instance.getPlayerList().getPlayers().getFirst(), 71));
                waitPose(c, 71);
                c.getInput().holdKey(ClimbControls.CLIMB);
                c.getInput().holdKeyFor(options -> options.keyUp, 20);
                c.waitFor(client -> Climbing.active(client.player));
                double y = c.computeOnClient(client -> client.player.getY());
                c.waitTicks(90);
                check(Math.abs(c.computeOnClient(client -> client.player.getY()) - y) < 0.15, "TCP grip stays stable with allow-flight=false");
                double clientY = c.computeOnClient(client -> client.player.getY());
                server.runOnServer(instance -> {
                    ServerPlayer player = instance.getPlayerList().getPlayers().getFirst();
                    check(Climbing.active(player) && Math.abs(player.getY() - clientY) < 0.15, "Client and server positions agree");
                    check(PlayerStamina.get(player).stamina() < 90, "Server charges real network climb and hold");
                });
                c.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
                c.waitTicks(3);
                c.takeScreenshot("p2-climbing-on-tcp-server");
            }
            release(c);
            try (TestDedicatedServerConnection connection = server.connect()) {
                connection.waitForChunksRender();
                c.waitTicks(3);
                server.runOnServer(instance -> check(!Climbing.active(instance.getPlayerList().getPlayers().getFirst()), "Reconnect never restores a grip"));
            }
        } finally {
            release(c);
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        }
        System.out.println("WILDCRAFT P2 real keys/hold/sideways/descent/ledge/inner-outer-corners/ceiling/damage/exhaustion/death/dimension/TCP reconnect checks passed");
    }

    private static void arena(ServerLevel level) {
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) level.setBlockAndUpdate(new BlockPos(x, 70, z), Blocks.STONE.defaultBlockState());
        for (int x = 2; x <= 5; x++) for (int y = 71; y <= 82; y++) for (int z = -5; z <= 5; z++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
    }

    private static void pose(ServerPlayer player, double y) {
        pose(player, y, 0.5);
    }

    private static void pose(ServerPlayer player, double y, double z) {
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.setHealth(player.getMaxHealth());
        player.hurtTime = 0;
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.resetFallDistance();
        Climbing.reset(player, false);
        PlayerStamina.fill(player);
        check(player.teleportTo(player.level(), 1.7, y, z, Set.of(), -90, 0, true), "Fixture teleport accepted");
    }

    private static void waitPose(ClientGameTestContext c, double y) {
        c.waitFor(client -> client.player != null && Math.abs(client.player.getX() - 1.7) < 0.1 && Math.abs(client.player.getY() - y) < 0.3);
    }

    private static void release(ClientGameTestContext c) {
        c.getInput().releaseKey(ClimbControls.CLIMB);
        c.getInput().releaseKey(options -> options.keyUp);
        c.getInput().releaseKey(options -> options.keyDown);
        c.getInput().releaseKey(options -> options.keyLeft);
        c.getInput().releaseKey(options -> options.keyRight);
    }

    private static void language(ClientGameTestContext c, String code) {
        CompletableFuture<Void> reload = c.computeOnClient(client -> {
            client.options.languageCode = code;
            client.getLanguageManager().setSelected(code);
            return client.reloadResourcePacks();
        });
        c.waitFor(client -> reload.isDone());
        reload.join();
        c.waitFor(client -> client.gui.overlay() == null);
    }

    private static Properties properties() {
        Properties p = new Properties();
        p.setProperty("server-ip", "127.0.0.1");
        p.setProperty("online-mode", "false");
        p.setProperty("enforce-secure-profile", "false");
        p.setProperty("allow-flight", "false");
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            p.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return p;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
