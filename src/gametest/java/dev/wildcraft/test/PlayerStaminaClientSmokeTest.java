package dev.wildcraft.test;

import dev.wildcraft.network.StaminaView;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.player.StaminaData;
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
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Checks actual player saves, lifecycle packets, owner sync and rendered HUD. */
public final class PlayerStaminaClientSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("experience set @p 15 levels");
            world.getServer().runCommand("experience set @p 30 levels");
            world.getServer().runCommand("experience set @p 5 levels");
            world.getServer().runOnServer(server -> holdAirborne(server.getPlayerList().getPlayers().getFirst()));
            world.getServer().runCommand("wildcraft stamina fill @p");
            world.getServer().runCommand("wildcraft stamina consume @p 118");
            context.waitFor(client -> viewMatches(client, 30, 42));
            check(context.computeOnClient(client -> client.player.experienceLevel == 5), "Vanilla XP still decreases normally");
            check(context.computeOnClient(client -> !client.player.hasAttached(PlayerStamina.DATA)), "Private saved progress is not sent to client");
            context.runOnClient(client -> {
                if (client.gui.hud.isHidden()) {
                    client.gui.hud.toggle();
                }
            });
            context.takeScreenshot("p1-stamina-peak30-after-xp-spend-en");
            changeLanguage(context, "zh_cn");
            check(context.computeOnClient(client -> Component.translatable("hud.wildcraft.stamina", "42", "160")
                    .getString().startsWith("精力")), "Chinese HUD translation loads");
            context.takeScreenshot("p1-stamina-peak30-after-xp-spend-zh");
            changeLanguage(context, "en_us");
            context.waitTicks(25);
            world.getServer().runOnServer(server -> checkData(server.getPlayerList().getPlayers().getFirst(), 30, 42));
            save = world.getWorldSave();
        }

        try (TestSingleplayerContext world = save.open()) {
            world.getConnection().waitForChunksRender();
            context.waitFor(client -> viewMatches(client, 30, 42));
            world.getServer().runOnServer(server -> checkData(server.getPlayerList().getPlayers().getFirst(), 30, 42));
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                check(player.teleportTo(server.getLevel(Level.NETHER), 0, 120, 0, Set.of(), 0, 0, true), "Dimension transfer accepted");
            });
            context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.NETHER));
            world.getServer().runOnServer(server -> checkData(server.getPlayerList().getPlayers().getFirst(), 30, 42));
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                check(player.teleportTo(server.overworld(), 0, 100, 0, Set.of(), 0, 0, true), "Return transfer accepted");
            });
            context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.OVERWORLD));
            context.waitFor(client -> viewMatches(client, 30, 42));
            world.getServer().runCommand("kill @p");
            context.waitFor(client -> client.player != null && !client.player.isAlive());
            context.runOnClient(client -> client.player.respawn());
            world.getServer().waitFor(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                return player.isAlive() && PlayerStamina.get(player).stamina() == 160;
            });
            context.waitFor(client -> client.player != null && client.player.isAlive() && viewMatches(client, 30, 160));
            world.getServer().runOnServer(server -> checkData(server.getPlayerList().getPlayers().getFirst(), 30, 160));
            world.getServer().runCommand("wildcraft stamina consume @p 40");
            context.waitFor(client -> client.player != null && client.player.getAttached(PlayerStamina.VIEW) != null
                    && client.player.getAttached(PlayerStamina.VIEW).stamina() < 160);
            world.getServer().waitFor(server -> PlayerStamina.get(server.getPlayerList().getPlayers().getFirst()).stamina() == 160);
            context.waitFor(client -> viewMatches(client, 30, 160));
        }

        Properties properties = localServerProperties();
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties)) {
            try (TestDedicatedServerConnection connection = server.connect()) {
                connection.waitForChunksRender();
                server.runCommand("experience set @p 30 levels");
                server.runCommand("experience set @p 5 levels");
                server.runOnServer(instance -> holdAirborne(instance.getPlayerList().getPlayers().getFirst()));
                server.runCommand("wildcraft stamina fill @p");
                server.runCommand("wildcraft stamina consume @p 118");
                context.waitFor(client -> viewMatches(client, 30, 42));
            }
            try (TestDedicatedServerConnection connection = server.connect()) {
                connection.waitForChunksRender();
                context.waitFor(client -> viewMatches(client, 30, 42));
                server.runOnServer(instance -> checkData(instance.getPlayerList().getPlayers().getFirst(), 30, 42));
                context.takeScreenshot("p1-stamina-after-server-reconnect");
            }
        }
        System.out.println("WILDCRAFT P1 real HUD/XP/save/restart/dimension/death/TCP reconnect checks passed");
    }

    private static void holdAirborne(ServerPlayer player) {
        // Flying is automatically cancelled on contact with the ground. Move the
        // fixture into actual air before granting flight, so reload waits cannot
        // accidentally exercise normal ground recovery instead of persistence.
        player.teleportTo(player.getX(), player.getY() + 12, player.getZ());
        player.setOnGround(false);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    private static void checkData(ServerPlayer player, int peak, double stamina) {
        StaminaData data = PlayerStamina.get(player);
        check(data.highestLevel() == peak && data.stamina() == stamina, "Server peak and stamina remain correct: " + data);
    }

    private static boolean viewMatches(Minecraft client, int peak, double stamina) {
        if (client.player == null) {
            return false;
        }
        StaminaView view = client.player.getAttached(PlayerStamina.VIEW);
        return view != null && view.highestLevel() == peak && view.stamina() == stamina;
    }

    private static void changeLanguage(ClientGameTestContext context, String language) {
        CompletableFuture<Void> reload = context.computeOnClient(client -> {
            client.options.languageCode = language;
            client.getLanguageManager().setSelected(language);
            return client.reloadResourcePacks();
        });
        context.waitFor(client -> reload.isDone());
        reload.join();
        context.waitFor(client -> client.gui.overlay() == null);
    }

    private static Properties localServerProperties() {
        Properties properties = new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("online-mode", "false");
        properties.setProperty("enforce-secure-profile", "false");
        properties.setProperty("allow-flight", "true");
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            properties.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return properties;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
