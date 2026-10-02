package dev.wildcraft.test.research.time;

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
public final class TimeResearchClientSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext c) {
        TimeClockCheck.main(new String[0]);
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
                check(WorldTimeResearch.active(server), "Bow session stays active in air");
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
            world.getServer().runOnServer(server -> check(!WorldTimeResearch.active(server) && server.tickRateManager().tickrate() == 12, "External control terminates session without overwrite"));
            release(c);
            world.getServer().runOnServer(server -> server.tickRateManager().setTickRate(20));
            start(c, world);
            save = world.getWorldSave();
            System.out.println("WILDCRAFT R1_WORLD=" + save.getSaveDirectory());
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
            ResearchBackLayer.enabled = true;
            c.waitTicks(3);
            c.takeScreenshot("r1-native-back-anchor");
            ResearchBackLayer.enabled = false;
            c.runOnClient(client -> {
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.player.setYRot(0);
                client.player.setXRot(25);
                client.player.setActivePostEffects(java.util.List.of());
            });
            c.waitTicks(3);
            c.takeScreenshot("r1-color-normal");
            c.runOnClient(client -> client.player.setActivePostEffects(java.util.List.of(ResearchBackLayer.POST_EFFECT)));
            c.waitTicks(4);
            c.runOnClient(client -> check(client.getShaderManager().getPostChain(ResearchBackLayer.POST_EFFECT,
                    net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS) != null, "Actual desaturation post chain compiles"));
            c.takeScreenshot("r1-color-desaturated");
            c.runOnClient(client -> client.player.setActivePostEffects(java.util.List.of()));
            start(c, world);
            world.getServer().runOnServer(server -> {
                check(server.publishServer(MinecraftServer.MultiplayerScope.LAN, true, 0), "Native LAN publication");
                check(server.tickRateManager().tickrate() == 20 && !WorldTimeResearch.active(server), "LAN restores before publication completes");
                check(TimeLease.acquire(server, 5) == null, "Published LAN rejects lease");
                server.unpublishServer();
            });
            release(c);
        }
        System.out.println("WILDCRAFT R1 lifecycle, ownership, real reload, native item anchor and GPU post-effect checks passed");
    }

    public static void prepare(ServerPlayer p) {
        // Reset only the dedicated fixture between cases, never an active focus session.
        p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(64))
                .forEach(net.minecraft.world.entity.Entity::discard);
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
        world.getServer().runOnServer(server -> check(WorldTimeResearch.begin(server.getPlayerList().getPlayers().getFirst(), 5, true), "Session began"));
    }

    private static void release(ClientGameTestContext c) {
        c.getInput().releaseKey(o -> o.keyUse);
        c.waitTicks(2);
    }

    private static void restored(TestSingleplayerContext world, String label) {
        world.getServer().runOnServer(server -> check(server.tickRateManager().tickrate() == 20 && !WorldTimeResearch.active(server), label + " restores rate"));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
