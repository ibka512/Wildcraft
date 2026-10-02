package dev.wildcraft.test;

import dev.wildcraft.client.input.ClimbControls;
import dev.wildcraft.client.render.ParagliderLayer;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
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
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Real jump presses, native inventory click packets, hand rendering and TCP movement. */
public final class GlidingClientSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext c) {
        release(c);
        TestWorldSave save;
        try (TestSingleplayerContext world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("time set day");
            world.getServer().runOnServer(server -> {
                arena(server.overworld());
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.getInventory().setItem(0, new ItemStack(WildcraftItems.PARAGLIDER));
                p.getInventory().setItem(1, new ItemStack(Items.DIAMOND_SWORD));
                p.getInventory().setItem(2, new ItemStack(Items.BOW));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                p.getInventory().setSelectedSlot(1);
                pose(p, 0.5, 71, 0.5, 0);
            });
            waitPose(c, 71);
            c.waitFor(client -> client.player.getInventory().getItem(0).is(WildcraftItems.PARAGLIDER));
            c.runOnClient(client -> client.player.getInventory().setSelectedSlot(1));
            c.getInput().holdKeyFor(o -> o.keyJump, 12);
            c.waitTicks(12);
            check(!c.computeOnClient(client -> Gliding.active(client.player)), "Ground jump never auto-deploys an unequipped glider");
            c.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            c.runOnClient(client -> client.gameMode.handleContainerInput(0, 36, 0, ContainerInput.QUICK_MOVE, client.player));
            c.waitFor(client -> GliderEquipment.equipped(client.player) && client.player.getInventory().getItem(0).isEmpty());
            c.waitTicks(3);
            world.getServer().runOnServer(server -> check(GliderEquipment.equipped(server.getPlayerList().getPlayers().getFirst()), "Native click packet equips on server"));
            language(c, "zh_cn");
            c.takeScreenshot("p3-paraglider-equipment-slot-zh");
            c.setScreen(() -> null);
            creativeEquipment(c, world);
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 71, 0.5, 0));
            waitPose(c, 71);
            c.getInput().holdKeyFor(o -> o.keyJump, 12);
            c.waitTicks(12);
            check(!c.computeOnClient(client -> Gliding.active(client.player)), "Holding a ground jump does not auto-deploy equipped gear");

            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 95, 0.5, 0));
            waitPose(c, 95);
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            double startY = c.computeOnClient(client -> client.player.getY());
            double startZ = c.computeOnClient(client -> client.player.getZ());
            c.getInput().holdKeyFor(o -> o.keyUp, 20);
            double endY = c.computeOnClient(client -> client.player.getY());
            check(startY - endY > 1 && startY - endY < 2.5, "Canopy descends slowly without upward lift");
            check(c.computeOnClient(client -> client.player.getZ()) > startZ + 3, "Forward key drives real glide");
            double x = c.computeOnClient(client -> client.player.getX());
            c.getInput().holdKeyFor(o -> o.keyLeft, 6);
            check(c.computeOnClient(client -> client.player.getX()) > x + 0.3, "Sideways key steers");
            c.runOnClient(client -> {
                var renderer = client.getEntityRenderDispatcher().getRenderer(client.player);
                var state = (AvatarRenderState) renderer.createRenderState(client.player, 1);
                check(state.getDataOrDefault(ParagliderLayer.OPEN, false), "Third-person canopy state is extracted");
                check(state.rightHandItemState.isEmpty() && state.leftHandItemState.isEmpty(), "Both third-person held items are holstered");
                var hands = new FirstPersonHandsAndItems();
                hands.tick(client.player);
                var handState = new FirstPersonHandsAndItemsRenderState();
                hands.extractRenderState(client.player, 1, handState);
                check(handState.handRenderSelection == null && handState.mainHandRenderState.isEmpty() && handState.offHandRenderState.isEmpty(), "First-person hands cannot show usable items while gripping");
                check(client.player.getMainHandItem().is(Items.DIAMOND_SWORD) && client.player.getOffhandItem().is(Items.SHIELD), "Holstering keeps real stacks intact");
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            });
            c.waitTicks(3);
            c.takeScreenshot("p3-paraglider-gliding-zh");
            jump(c);
            c.waitFor(client -> !Gliding.active(client.player));
            double closedY = c.computeOnClient(client -> client.player.getY());
            c.waitTicks(8);
            check(c.computeOnClient(client -> client.player.getY()) < closedY - 1, "Closing restores gravity");

            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 95, 0.5, 0));
            waitPose(c, 95);
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            c.getInput().pressKey(o -> o.keyHotbarSlots[2]);
            c.waitFor(client -> !Gliding.active(client.player));
            world.getServer().runOnServer(server -> check(!Gliding.active(server.getPlayerList().getPlayers().getFirst())
                    && server.getPlayerList().getPlayers().getFirst().getMainHandItem().is(Items.BOW), "Taking a hotbar item closes on server"));
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 95, 0.5, 0));
            waitPose(c, 95);
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            c.getInput().holdKeyFor(o -> o.keyUse, 1);
            c.waitFor(client -> !Gliding.active(client.player));
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 95, 0.5, 0));
            waitPose(c, 95);
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            c.getInput().holdKeyFor(o -> o.keyAttack, 1);
            c.waitFor(client -> !Gliding.active(client.player));
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 95, 0.5, 0));
            waitPose(c, 95);
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            c.getInput().pressKey(o -> o.keySwapOffhand);
            c.waitFor(client -> !Gliding.active(client.player) && client.player.getMainHandItem().is(Items.SHIELD));
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 95, 0.5, 0));
            waitPose(c, 95);
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            c.getInput().pressKey(o -> o.keyDrop);
            c.waitFor(client -> !Gliding.active(client.player) && client.player.getMainHandItem().isEmpty());

            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 76, 0.5, 0));
            waitPose(c, 76);
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            c.waitFor(client -> client.player.onGround() && !Gliding.active(client.player), 100);
            world.getServer().runOnServer(server -> check(server.getPlayerList().getPlayers().getFirst().getHealth() == 20, "Open-canopy landing has no fall damage"));
            world.getServer().runOnServer(server -> pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 95, 0.5, 0));
            waitPose(c, 95);
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            world.getServer().runCommand("damage @p 1 minecraft:generic");
            c.waitFor(client -> !Gliding.active(client.player));
            c.waitTicks(15);
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                pose(p, 0.5, 95, 0.5, 0);
                PlayerStamina.consume(p, PlayerStamina.get(p).stamina() - 0.4);
            });
            waitPose(c, 95);
            jump(c);
            c.waitFor(client -> !Gliding.active(client.player) && client.player.getAttached(PlayerStamina.VIEW).stamina() == 0);
            double exhaustedY = c.computeOnClient(client -> client.player.getY());
            c.waitTicks(8);
            check(c.computeOnClient(client -> client.player.getY()) < exhaustedY - 1, "Exhaustion returns to falling");

            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                for (int yy = 71; yy <= 86; yy++) for (int zz = -2; zz <= 2; zz++) level.setBlockAndUpdate(new BlockPos(2, yy, zz), BlocksHolder.STONE);
                pose(server.getPlayerList().getPlayers().getFirst(), 1.7, 80, 0.5, -90);
            });
            waitPose(c, 80);
            c.getInput().holdKey(ClimbControls.CLIMB);
            c.getInput().holdKeyFor(o -> o.keyUp, 8);
            c.waitFor(client -> Climbing.active(client.player));
            jump(c);
            c.waitFor(client -> Gliding.active(client.player) && !Climbing.active(client.player));
            c.getInput().releaseKey(ClimbControls.CLIMB);
            c.getInput().lookAt(90, 0);
            c.waitTicks(10);
            c.takeScreenshot("p3-climb-to-glide");
            world.getServer().runOnServer(server -> check(Gliding.active(server.getPlayerList().getPlayers().getFirst()) && !Climbing.active(server.getPlayerList().getPlayers().getFirst()), "Climb-to-glide has one server movement mode"));

            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel nether = server.getLevel(Level.NETHER);
                for (int xx = -2; xx <= 2; xx++) for (int zz = -2; zz <= 2; zz++) {
                    nether.setBlockAndUpdate(new BlockPos(xx, 119, zz), BlocksHolder.STONE);
                    for (int yy = 120; yy < 124; yy++) nether.setBlockAndUpdate(new BlockPos(xx, yy, zz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                check(p.teleportTo(nether, 0, 120, 0, Set.of(), 0, 0, true), "Dimension transfer accepted");
            });
            c.waitFor(client -> client.level.dimension().equals(Level.NETHER));
            c.waitTicks(3);
            check(!c.computeOnClient(client -> Gliding.active(client.player)), "Dimension transfer closes canopy");
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                check(GliderEquipment.equipped(p), "Dimension transfer preserves equipment");
                check(p.teleportTo(server.overworld(), 0.5, 95, 0.5, Set.of(), 0, 0, true), "Return transfer accepted");
            });
            c.waitFor(client -> client.level.dimension().equals(Level.OVERWORLD));
            c.waitFor(client -> client.gui.screen() == null && client.gui.overlay() == null
                    && Gliding.eligible(client.player) && client.player.getAttached(Gliding.VIEW) != null
                    && !client.player.getAttached(Gliding.VIEW).blocked());
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            world.getServer().runOnServer(server -> server.overworld().getGameRules().set(GameRules.KEEP_INVENTORY, true, server));
            world.getServer().runCommand("kill @p");
            c.waitFor(client -> !client.player.isAlive());
            c.runOnClient(client -> client.player.respawn());
            c.waitFor(client -> client.player.isAlive() && GliderEquipment.equipped(client.player));
            world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().connection.hasClientLoaded());
            check(!c.computeOnClient(client -> Gliding.active(client.player)), "Respawn never opens canopy");
            world.getServer().runOnServer(server -> {
                server.overworld().getGameRules().set(GameRules.KEEP_INVENTORY, false, server);
                pose(server.getPlayerList().getPlayers().getFirst(), 0.5, 71, 0.5, 0);
            });
            waitPose(c, 71);
            world.getServer().runCommand("kill @p");
            c.waitFor(client -> !client.player.isAlive());
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                check(!GliderEquipment.equipped(p), "Death removes dropped equipment from player");
                long drops = server.overworld().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8)).stream().filter(e -> e.getItem().is(WildcraftItems.PARAGLIDER)).count();
                check(drops == 1, "Death drops exactly one equipped paraglider");
            });
            c.runOnClient(client -> client.player.respawn());
            c.waitFor(client -> client.player.isAlive());
            world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().connection.hasClientLoaded());
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                GliderEquipment.set(p, new ItemStack(WildcraftItems.PARAGLIDER));
                pose(p, 0.5, 95, 0.5, 0);
            });
            waitPose(c, 95);
            c.waitFor(client -> GliderEquipment.equipped(client.player));
            jump(c);
            c.waitFor(client -> Gliding.active(client.player));
            save = world.getWorldSave();
        }
        try (TestSingleplayerContext world = save.open()) {
            world.getConnection().waitForChunksRender();
            c.waitFor(client -> GliderEquipment.equipped(client.player));
            check(!c.computeOnClient(client -> Gliding.active(client.player)), "World reload keeps gear and discards open state");
        }
        language(c, "en_us");
        try (TestDedicatedServerContext server = c.worldBuilder().createServer(properties())) {
            server.runOnServer(instance -> arena(instance.overworld()));
            try (TestDedicatedServerConnection connection = server.connect()) {
                connection.waitForChunksRender();
                server.runCommand("gamemode survival @p");
                server.runOnServer(instance -> {
                    ServerPlayer p = instance.getPlayerList().getPlayers().getFirst();
                    GliderEquipment.set(p, new ItemStack(WildcraftItems.PARAGLIDER));
                    pose(p, 0.5, 110, 0.5, 0);
                });
                waitPose(c, 110);
                c.waitFor(client -> GliderEquipment.equipped(client.player));
                jump(c);
                c.waitFor(client -> Gliding.active(client.player));
                c.getInput().holdKeyFor(o -> o.keyUp, 90);
                double clientY = c.computeOnClient(client -> client.player.getY());
                server.runOnServer(instance -> {
                    ServerPlayer p = instance.getPlayerList().getPlayers().getFirst();
                    check(Gliding.active(p) && Math.abs(p.getY() - clientY) < 0.3, "TCP positions agree with allow-flight=false");
                    check(PlayerStamina.get(p).stamina() < 90, "Server charges actual network flight");
                });
                c.takeScreenshot("p3-gliding-tcp-en");
            }
            release(c);
            try (TestDedicatedServerConnection connection = server.connect()) {
                connection.waitForChunksRender();
                c.waitFor(client -> GliderEquipment.equipped(client.player));
                check(!c.computeOnClient(client -> Gliding.active(client.player)), "TCP reconnect keeps gear without opening");
            }
        } finally {
            release(c);
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        }
        System.out.println("WILDCRAFT P3 dedicated-slot/native-click/creative-slot-permissions/holstered-hands/jump-toggle/steering/landing/damage/exhaustion/climb-transition/death-drop/keep-inventory/dimension/save-reload/TCP reconnect checks passed");
    }

    private static void creativeEquipment(ClientGameTestContext c, TestSingleplayerContext world) {
        world.getServer().runCommand("gamemode creative @p");
        c.waitFor(client -> client.player.getAbilities().mayfly);
        c.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        c.waitFor(client -> client.gui.screen() instanceof CreativeModeInventoryScreen);
        c.runOnClient(client -> {
            var screen = (CreativeModeInventoryScreen) client.gui.screen();
            CreativeModeTab inventory = BuiltInRegistries.CREATIVE_MODE_TAB.stream()
                    .filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY).findFirst().orElseThrow();
            try {
                var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                select.setAccessible(true);
                select.invoke(screen, inventory);
            } catch (ReflectiveOperationException e) { throw new AssertionError("Cannot select native creative equipment page", e); }
            int index = GliderEquipment.slot(client.player.inventoryMenu).index;
            Slot gear = screen.getMenu().getSlot(index);
            check(gear.x == 152 && gear.y == 20, "Creative equipment slot has a dedicated visible position");
            check(screen.getMenu().slots.stream().noneMatch(slot -> slot != gear && slot.x == gear.x && slot.y == gear.y), "Creative equipment slot does not overlap existing slots");
        });
        c.takeScreenshot("p3-paraglider-creative-slot-zh");
        creativeClick(c);
        world.getServer().waitFor(server -> !GliderEquipment.equipped(server.getPlayerList().getPlayers().getFirst()));
        creativeClick(c);
        world.getServer().waitFor(server -> GliderEquipment.equipped(server.getPlayerList().getPlayers().getFirst()));
        c.runOnClient(client -> client.gameMode.handleCreativeModeItemAdd(new ItemStack(Items.STONE), GliderEquipment.slot(client.player.inventoryMenu).index));
        c.waitTicks(3);
        c.runOnClient(client -> client.gameMode.handleCreativeModeItemAdd(new ItemStack(WildcraftItems.PARAGLIDER, 2), GliderEquipment.slot(client.player.inventoryMenu).index));
        c.waitTicks(3);
        world.getServer().runOnServer(server -> {
            ItemStack gear = GliderEquipment.get(server.getPlayerList().getPlayers().getFirst());
            check(gear.is(WildcraftItems.PARAGLIDER) && gear.getCount() == 1, "Creative packets reject wrong items and excess stacks");
        });
        c.setScreen(() -> null);
        world.getServer().runCommand("gamemode survival @p");
        c.waitFor(client -> !client.player.getAbilities().mayfly);
        c.runOnClient(client -> client.getConnection().send(new ServerboundSetCreativeModeSlotPacket(GliderEquipment.slot(client.player.inventoryMenu).index, ItemStack.EMPTY)));
        c.waitTicks(3);
        world.getServer().runOnServer(server -> check(GliderEquipment.equipped(server.getPlayerList().getPlayers().getFirst()), "Survival cannot spoof a creative equipment edit"));
    }

    private static void creativeClick(ClientGameTestContext c) {
        c.runOnClient(client -> {
            var screen = (CreativeModeInventoryScreen) client.gui.screen();
            int index = GliderEquipment.slot(client.player.inventoryMenu).index;
            try {
                var click = CreativeModeInventoryScreen.class.getDeclaredMethod("slotClicked", Slot.class, int.class, int.class, ContainerInput.class);
                click.setAccessible(true);
                click.invoke(screen, screen.getMenu().getSlot(index), index, 0, ContainerInput.PICKUP);
            } catch (ReflectiveOperationException e) { throw new AssertionError("Cannot click native creative equipment slot", e); }
        });
    }

    private static void arena(ServerLevel level) {
        for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) level.setBlockAndUpdate(new BlockPos(x, 70, z), BlocksHolder.STONE);
    }

    private static void pose(ServerPlayer p, double x, double y, double z, float yaw) {
        p.getAbilities().mayfly = false;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        p.setHealth(p.getMaxHealth());
        p.hurtTime = 0;
        p.stopUsingItem();
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        Climbing.reset(p, false);
        Gliding.reset(p, false);
        PlayerStamina.fill(p);
        check(p.teleportTo(p.level(), x, y, z, Set.of(), yaw, 0, true), "Fixture teleport accepted");
    }

    private static void waitPose(ClientGameTestContext c, double y) {
        c.waitFor(client -> client.player != null && Math.abs(client.player.getY() - y) < 0.3);
        c.waitTicks(1);
    }

    private static void jump(ClientGameTestContext c) {
        c.getInput().releaseKey(o -> o.keyJump);
        c.waitTicks(1);
        c.getInput().holdKeyFor(o -> o.keyJump, 1);
        c.waitTicks(2);
    }

    private static void release(ClientGameTestContext c) {
        c.getInput().releaseKey(ClimbControls.CLIMB);
        c.getInput().releaseKey(o -> o.keyJump);
        c.getInput().releaseKey(o -> o.keyUp);
        c.getInput().releaseKey(o -> o.keyDown);
        c.getInput().releaseKey(o -> o.keyLeft);
        c.getInput().releaseKey(o -> o.keyRight);
        c.getInput().releaseKey(o -> o.keyUse);
        c.getInput().releaseKey(o -> o.keyAttack);
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
        p.setProperty("gamemode", "survival");
        p.setProperty("allow-flight", "false");
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            p.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return p;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class BlocksHolder {
        static final net.minecraft.world.level.block.state.BlockState STONE = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
    }
}
