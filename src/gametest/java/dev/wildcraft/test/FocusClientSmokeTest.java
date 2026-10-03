package dev.wildcraft.test;

import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.focus.TimeLease;
import dev.wildcraft.test.research.time.TimeResearchClientSmokeTest;

import dev.wildcraft.player.PlayerStamina;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Deterministic lifecycle tests. Wall-clock measurements use a separate normal client. */
public final class FocusClientSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext c) {

        TestWorldSave save;
        try (TestSingleplayerContext world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("time set day");
            world.getServer().runOnServer(server -> {
                prepare(server.getPlayerList().getPlayers().getFirst());
                server.tickRateManager().setTickRate(30);
                TimeLease lease = TimeLease.acquire(server, 5);
                check(lease != null && lease.ownsControl(), "Unpublished single-player lease acquired");
                check(TimeLease.acquire(server, 5) == null, "Nested lease rejected");
                lease.close();
                check(server.tickRateManager().tickrate() == 30, "Restores original 30 instead of hard-coded 20");
                lease.close();
                check(server.tickRateManager().tickrate() == 30, "Repeated close is inert");
                server.tickRateManager().setTickRate(20);
                server.tickRateManager().setFrozen(true);
                check(TimeLease.acquire(server, 5) == null, "Frozen world rejected");
                server.tickRateManager().setFrozen(false);
                TimeLease freezeDuring = TimeLease.acquire(server, 5);
                server.tickRateManager().setFrozen(true);
                freezeDuring.close();
                check(server.tickRateManager().tickrate() == 20 && server.tickRateManager().isFrozen(), "Undo owned rate while preserving external freeze");
                server.tickRateManager().setFrozen(false);
                TimeLease external = TimeLease.acquire(server, 5);
                server.tickRateManager().setTickRate(10);
                external.close();
                check(server.tickRateManager().tickrate() == 10, "External 10 retained");
                server.tickRateManager().setTickRate(20);
                TimeLease sameRate = TimeLease.acquire(server, 5);
                server.tickRateManager().setTickRate(5);
                sameRate.close();
                check(server.tickRateManager().tickrate() == 5, "External same-value write retained");
                server.tickRateManager().setTickRate(20);
            });
            start(c, world);
            c.waitTicks(4);
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                check(FocusTime.active(server), "Bow session stays active in air");
                check(p.fallDistance > 0 && p.getY() < 105, "Finite descent retains accumulated fall distance");
                p.stopUsingItem();
            });
            c.getInput().releaseKey(o -> o.keyUse);
            c.waitTicks(3);
            restored(world, "Stopped bow");
            start(c, world);
            world.getServer().runOnServer(server -> PlayerStamina.consume(server.getPlayerList().getPlayers().getFirst(), 1000));
            c.waitTicks(2);
            restored(world, "Exhaustion");
            release(c);
            start(c, world);
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(server.overworld(), 0.5, 71, 0.5, Set.of(), 0, 0, false);
            });
            c.waitTicks(4);
            restored(world, "Landing");
            release(c);
            start(c, world);
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.hurtServer(p.level(), p.damageSources().genericKill(), 10000);
            });
            c.waitTicks(2);
            restored(world, "Death");
            c.getInput().releaseKey(o -> o.keyUse);
            c.runOnClient(client -> client.player.respawn());
            c.waitFor(client -> client.player.isAlive());
            world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().connection.hasClientLoaded());
            start(c, world);
            c.runOnClient(client -> client.pauseGame(false));
            c.waitTicks(4);
            restored(world, "Pause menu");
            double pausedStamina = world.getServer().computeOnServer(server -> PlayerStamina.get(server.getPlayerList().getPlayers().getFirst()).stamina());
            c.waitTicks(12);
            world.getServer().runOnServer(server -> check(PlayerStamina.get(server.getPlayerList().getPlayers().getFirst()).stamina() == pausedStamina, "Paused time has no cost"));
            c.setScreen(() -> null);
            release(c);
            start(c, world);
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                    nether.setBlockAndUpdate(new BlockPos(x, 119, z), Blocks.STONE.defaultBlockState());
                    for (int y = 120; y <= 124; y++) nether.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                check(p.teleportTo(nether, 0.5, 120, 0.5, Set.of(), 0, 0, false), "Native dimension transfer");
            });
            c.waitFor(client -> client.level.dimension().equals(net.minecraft.world.level.Level.NETHER));
            restored(world, "Dimension change");
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(server.overworld(), 0.5, 71, 0.5, Set.of(), 0, 0, false);
            });
            c.waitFor(client -> client.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD));
            c.waitFor(client -> client.gui.screen() == null && client.gui.overlay() == null);
            release(c);
            start(c, world);
            world.getServer().runOnServer(server -> {
                server.tickRateManager().setTickRate(12);
            });
            c.waitTicks(2);
            world.getServer().runOnServer(server -> check(!FocusTime.active(server) && server.tickRateManager().tickrate() == 12, "External control terminates session without overwrite"));
            release(c);
            world.getServer().runOnServer(server -> server.tickRateManager().setTickRate(20));
            start(c, world);
            save = world.getWorldSave();
            System.out.println("WILDCRAFT P3.2_WORLD=" + save.getSaveDirectory());
        }
        release(c);
        try (TestSingleplayerContext world = save.open()) {
            world.getConnection().waitForChunksRender();
            restored(world, "Real save/reload");
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                prepare(p);
                p.teleportTo(server.overworld(), 0.5, 71, 0.5, Set.of(), 0, 0, false);
            });
            c.waitTicks(4);
            c.runOnClient(client -> {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                client.player.setYRot(150);
            });
            c.waitTicks(3);
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            start(c, world);
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(server.overworld(), 0.5, 85, 0.5, Set.of(), 0, 55, false);
                p.setNoGravity(true); p.setDeltaMovement(Vec3.ZERO);
            });
            c.waitFor(client -> Math.abs(client.player.getY() - 85) < 1);
            c.runOnClient(client -> { client.player.setNoGravity(true); client.player.setDeltaMovement(Vec3.ZERO); client.player.setXRot(55); });
            c.waitFor(client -> FocusTime.active(client.player));
            c.runOnClient(client -> {
                var o = dev.wildcraft.client.focus.FocusOptions.get();
                for (int strength = 1; strength <= 3; strength++) for (boolean vignette : new boolean[]{false, true}) for (boolean pattern : new boolean[]{false, true}) {
                    o.strength = strength; o.vignette = vignette; o.pattern = pattern;
                    check(client.getShaderManager().getPostChain(dev.wildcraft.client.focus.FocusClient.effect(),
                            net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS) != null, "Production GPU focus variant compiles");
                }
                check(client.getShaderManager().getPostChain(dev.wildcraft.Wildcraft.id("focus/low_stamina"), net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS) != null, "Optional low-stamina GPU pulse compiles");
                o.strength = 2; o.vignette = true; o.pattern = false;
                o.reticle = false; o.fov = true; o.pattern = true;
                dev.wildcraft.client.focus.FocusOptions.save();
                o.reticle = true; o.fov = false; o.pattern = false;
                dev.wildcraft.client.focus.FocusOptions.load();
                o = dev.wildcraft.client.focus.FocusOptions.get();
                check(!o.reticle && o.fov && o.pattern && o.strength == 2, "Client presentation settings round-trip without world data");
                o.reticle = true; o.fov = false; o.pattern = false; dev.wildcraft.client.focus.FocusOptions.save();
                check(dev.wildcraft.client.focus.FocusClient.postEffects(java.util.List.of(dev.wildcraft.Wildcraft.id("other_effect"))).getFirst().getPath().equals("other_effect"), "Existing post-effect retained");
            });
            c.runOnClient(client -> dev.wildcraft.client.focus.FocusOptions.get().strength = 0);
            c.waitTicks(3); c.takeScreenshot("p32-color-normal");
            c.runOnClient(client -> dev.wildcraft.client.focus.FocusOptions.get().strength = 2);
            c.waitTicks(3); c.takeScreenshot("p32-focus-first-person");
            c.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            c.waitTicks(3); c.takeScreenshot("p32-focus-third-person");
            c.runOnClient(client -> {
                var o = dev.wildcraft.client.focus.FocusOptions.get(); o.strength = 0;
                check(dev.wildcraft.client.focus.FocusClient.postEffects(java.util.List.of()).isEmpty(), "Off removes filter");
                check(FocusTime.active(client.player), "Off preserves skill");
            });
            c.waitTicks(3); c.takeScreenshot("p32-focus-effects-off");
            c.runOnClient(client -> {
                dev.wildcraft.client.focus.FocusOptions.get().strength = 2;
                client.gui.setScreen(new dev.wildcraft.client.focus.FocusSettingsScreen(null));
            });
            c.waitTicks(3); c.takeScreenshot("p32-focus-settings");
            c.setScreen(() -> null); release(c);
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setNoGravity(false));
            c.runOnClient(client -> client.player.setNoGravity(false));
            fallAndTraversal(c, world);
            start(c, world);
            world.getServer().runOnServer(server -> {
                check(server.publishServer(MinecraftServer.MultiplayerScope.LAN, true, 0), "Native LAN publication");
                check(server.tickRateManager().tickrate() == 20 && !FocusTime.active(server), "LAN restores before publication completes");
                check(TimeLease.acquire(server, 5) == null, "Published LAN rejects lease");
                server.unpublishServer();
            });
            release(c);
        }
        System.out.println("WILDCRAFT P3.2 automatic bow, lifecycle, ownership, reload and production GPU presentation checks passed");
    }

    private static void fallAndTraversal(ClientGameTestContext c, TestSingleplayerContext world) {
        release(c);
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst(); prepare(p);
            p.teleportTo(server.overworld(), 0.5, 71, 0.5, Set.of(), 0, 0, false);
        });
        c.waitFor(client -> client.player.onGround());
        c.getInput().holdKey(o -> o.keyUse); c.waitTicks(4);
        world.getServer().runOnServer(server -> check(!FocusTime.active(server) && server.tickRateManager().tickrate() == 20, "Ground bow has no focus"));
        release(c);
        start(c, world);
        c.waitTicks(6);
        double distance = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().fallDistance);
        System.out.println("WILDCRAFT P3.2 initial fall distance=" + distance);
        check(distance > 0, "Actual descent accumulates distance");
        for (int i = 0; i < 3; i++) {
            release(c); c.getInput().holdKey(o -> o.keyUse);
            c.waitFor(client -> client.player.isUsingItem());
            world.getServer().waitFor(FocusTime::active); c.waitTicks(4);
            double next = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().fallDistance);
            System.out.println("WILDCRAFT P3.2 toggled fall distance=" + next);
            check(next >= distance, "Repeated bow toggles never reset fall distance"); distance = next;
        }
        release(c);
        c.waitFor(client -> client.player.onGround() || !client.player.isAlive());
        c.waitTicks(4); // A ground-only packet can precede the final landing-position packet.
        world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().onGround() || !server.getPlayerList().getPlayers().getFirst().isAlive());
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            System.out.println("WILDCRAFT P3.2 landing health=" + p.getHealth() + " ground=" + p.onGround() + " loaded=" + p.connection.hasClientLoaded() + " dimensionChanging=" + p.isChangingDimension() + " mayfly=" + p.getAbilities().mayfly + " noGravity=" + p.isNoGravity() + " remainingFall=" + p.fallDistance + " y=" + p.getY() + " block=" + p.level().getBlockState(p.getOnPos()).getBlock() + " fallDamageRule=" + p.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE));
            check(p.getHealth() < 20, "Real native landing causes accumulated fall damage");
            check(!FocusTime.active(server) && server.tickRateManager().tickrate() == 20, "Landing clears time ownership");
        });
        if (!world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().isAlive())) {
            c.waitFor(client -> !client.player.isAlive());
            c.runOnClient(client -> client.player.respawn()); c.waitFor(client -> client.player.isAlive());
            world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().connection.hasClientLoaded());
        }
        start(c, world);
        // Equip/open the existing glider using the same authority path as jump input.
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            p.stopUsingItem();
            dev.wildcraft.player.GliderEquipment.set(p, new ItemStack(dev.wildcraft.registry.WildcraftItems.PARAGLIDER));
            dev.wildcraft.traversal.Gliding.reset(p, false);
            dev.wildcraft.traversal.Gliding.receive(p, dev.wildcraft.network.GlideInput.OPEN);
            check(dev.wildcraft.traversal.Gliding.active(p) && !FocusTime.active(server), "Focus to glide restores world before glider activation");
        });
        release(c);
        world.getServer().runOnServer(server -> dev.wildcraft.traversal.Gliding.reset(server.getPlayerList().getPlayers().getFirst(), false));
        c.getInput().holdKey(o -> o.keyUse); c.waitFor(client -> client.player.isUsingItem());
        world.getServer().waitFor(FocusTime::active);
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            dev.wildcraft.traversal.Climbing.receive(p, new dev.wildcraft.network.ClimbInput(true, (byte)0, (byte)0));
            FocusTime.pulse(server);
            check(!FocusTime.active(server) && server.tickRateManager().tickrate() == 20, "Climb input exits focus");
            dev.wildcraft.traversal.Climbing.reset(p, false);
            dev.wildcraft.player.GliderEquipment.set(p, ItemStack.EMPTY);
        });
        release(c);
    }

    public static void prepare(ServerPlayer p) {
        // Reset only the dedicated fixture between cases, never an active focus session.
        p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(64))
                .forEach(net.minecraft.world.entity.Entity::discard);
        p.stopUsingItem();
        p.setNoGravity(false);
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtTime = 0;
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) p.level().setBlockAndUpdate(new BlockPos(x, 70, z), Blocks.STONE.defaultBlockState());
        for (int x = -4; x <= 4; x++) for (int y = 71; y <= 75; y++) {
            p.level().setBlockAndUpdate(new BlockPos(x, y, 8), Blocks.WOOL.pick(x < 0 ? net.minecraft.world.item.DyeColor.RED : net.minecraft.world.item.DyeColor.BLUE).defaultBlockState());
        }
        p.getInventory().clearContent();
        p.getInventory().setItem(0, new ItemStack(Items.BOW));
        p.getInventory().setItem(1, new ItemStack(Items.DIAMOND_SWORD));
        p.getInventory().setItem(2, new ItemStack(Items.SHIELD));
        p.getInventory().setItem(3, new ItemStack(Items.BOW));
        p.getInventory().setItem(9, new ItemStack(Items.ARROW, 64));
        p.getInventory().setSelectedSlot(0);
        p.setHealth(20);
        PlayerStamina.fill(p);
    }

    private static void start(ClientGameTestContext c, TestSingleplayerContext world) {
        release(c);
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            prepare(p);
            p.teleportTo(server.overworld(), 0.5, 105, 0.5, Set.of(), 0, 0, false);
            p.setDeltaMovement(Vec3.ZERO);
            p.setOnGround(false);
        });
        c.waitFor(client -> client.player.getY() > 100 && client.player.getMainHandItem().is(Items.BOW));
        c.getInput().holdKey(o -> o.keyUse);
        c.waitFor(client -> client.player.isUsingItem());
        world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().isUsingItem());
        world.getServer().runOnServer(server -> check(FocusTime.active(server) || FocusTime.tryBegin(server.getPlayerList().getPlayers().getFirst()), "Automatic production session began"));
    }

    private static void release(ClientGameTestContext c) {
        c.getInput().releaseKey(o -> o.keyUse);
        c.waitTicks(2);
    }

    private static void restored(TestSingleplayerContext world, String label) {
        world.getServer().runOnServer(server -> check(server.tickRateManager().tickrate() == 20 && !FocusTime.active(server), label + " restores rate"));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
