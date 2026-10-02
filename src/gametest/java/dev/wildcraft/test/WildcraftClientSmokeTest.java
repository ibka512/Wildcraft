package dev.wildcraft.test;

import dev.wildcraft.registry.WildcraftItems;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Properties;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;

/** Exercises server commands, inventory sync, rendering and a real world save. */
public final class WildcraftClientSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @p");
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("give @p wildcraft:test_core 2");
            context.waitFor(client -> hasTestCores(client, 2));
            context.takeScreenshot("p0-test-core-in-world");
            save = world.getWorldSave();
        }

        try (TestSingleplayerContext world = save.open()) {
            world.getConnection().waitForChunksRender();
            context.waitFor(client -> hasTestCores(client, 2));
            String name = context.computeOnClient(client ->
                    client.player.getInventory().getItem(0).getHoverName().getString());
            if (!name.equals("Wildcraft Test Core")) {
                throw new AssertionError("Test core translation failed: " + name);
            }
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.takeScreenshot("p0-test-core-after-reload");
            context.setScreen(() -> null);
        }

        Properties properties = new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("online-mode", "false");
        properties.setProperty("enforce-secure-profile", "false");
        properties.setProperty("gamemode", "creative");
        // Choose a free local port so another development server can keep running.
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            properties.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot reserve a local test port", exception);
        }

        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
                TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            server.runCommand("give @p wildcraft:test_core 2");
            context.waitFor(client -> hasTestCores(client, 2));
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.takeScreenshot("p0-test-core-on-server");
            context.setScreen(() -> null);
        }
    }

    private static boolean hasTestCores(Minecraft client, int count) {
        if (client.player == null) {
            return false;
        }
        ItemStack stack = client.player.getInventory().getItem(0);
        return stack.is(WildcraftItems.TEST_CORE) && stack.getCount() == count;
    }
}
