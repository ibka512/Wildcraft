package dev.wildcraft.test.research.time;

import dev.wildcraft.player.PlayerStamina;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Runs WITHOUT Fabric's controlled client-gametest scheduler. */
public final class FocusNativeProbe {
    private record Case(String name, float rate, boolean compensate, int fps, boolean enchanted, boolean stall) {
        Case(String name, float rate, boolean compensate, int fps, boolean enchanted) { this(name, rate, compensate, fps, enchanted, false); }
    }
    private record Snapshot(long time, long sampledAt, int used, double stamina, int cowTicks, int projectileTicks,
                            double projectileX, int bornArrows, int ammunition, int damage, double maxChargeStep) { }
    private static final Case[] CASES = {
        new Case("production-5-120fps", 5, true, 120, false),
        new Case("production-5-30fps", 5, true, 30, false),
        new Case("production-5-infinity-power", 5, true, 120, true),
        new Case("production-5-700ms-server-stall", 5, true, 120, false, true)
    };
    private static String waitingScreen;
    private static int index;
    private static int state;
    private static long deadline;
    private static int frames;
    private static float previousYaw;
    private static int yawChanges;
    private static Cow cow;
    private static AbstractArrow projectile;
    private static Snapshot before;
    private static Snapshot held;
    private static CompletableFuture<Snapshot> pending;
    private static final List<String> RESULTS = new ArrayList<>();
    private static volatile java.util.UUID probeOwner;
    private static volatile long lastShotAt;
    private static long releasedAt;
    private static volatile boolean shotHasEnchantments;
    private static int bornArrows;
    private static final java.util.Set<java.util.UUID> SEEN_ARROWS = new java.util.HashSet<>();

    public static boolean holdingUse() { return Boolean.getBoolean("wildcraft.p32.native") && state >= 3 && state <= 5; }

    public static void arrowSpawned(net.minecraft.world.entity.Entity entity) {
        if (Boolean.getBoolean("wildcraft.p32.native") && entity instanceof AbstractArrow arrow
                && arrow.getOwner() != null && arrow.getOwner().getUUID().equals(probeOwner) && SEEN_ARROWS.add(arrow.getUUID())) {
            bornArrows++;
            lastShotAt = System.nanoTime();
            shotHasEnchantments = arrow.getWeaponItem() != null && !arrow.getWeaponItem().getEnchantments().isEmpty();
        }
    }

    public static void frame(Minecraft client) {
        if (!Boolean.getBoolean("wildcraft.p32.native")) return;
        if (client.player == null || client.level == null || client.getSingleplayerServer() == null
                || client.gui.screen() != null || client.gui.overlay() != null) {
            String screen = String.valueOf(client.gui.screen()) + "/" + (client.gui.overlay() == null ? "no overlay" : client.gui.overlay().getClass().getSimpleName());
            if (!screen.equals(waitingScreen)) { waitingScreen = screen; System.out.println("WILDCRAFT P3.2 fixture waiting: " + screen); }
            return;
        }
        MinecraftServer server = client.getSingleplayerServer();
        long now = System.nanoTime();
        if (state == 0) {
            client.player.setNoGravity(true);
            client.player.resetFallDistance();
            server.execute(() -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.setNoGravity(true);
                p.resetFallDistance();
                p.setDeltaMovement(Vec3.ZERO);
                p.setHealth(20);
            });
            System.out.println("WILDCRAFT P3.2 native scheduler: no fabric.client.gametest property=" + System.getProperty("fabric.client.gametest"));
            deadline = now + 2_000_000_000L;
            state = 1;
        } else if (state == 1 && now >= deadline) {
            Case sample = CASES[index];
            client.options.framerateLimit().set(sample.fps());
            client.options.enableVsync().set(false);
            client.options.pauseOnLostFocus = false;
            client.options.setCameraType(index == 2 ? net.minecraft.client.CameraType.THIRD_PERSON_BACK : net.minecraft.client.CameraType.FIRST_PERSON);

            pending = submit(server, () -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                for (var entity : java.util.stream.StreamSupport.stream(p.level().getAllEntities().spliterator(), false).toList()) {
                    if (entity instanceof AbstractArrow arrow && arrow.getOwner() == p) arrow.discard();
                }
                TimeResearchClientSmokeTest.prepare(p);
                probeOwner = p.getUUID();
                shotHasEnchantments = false;
                if (sample.enchanted()) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "enchant @p minecraft:infinity 1");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "enchant @p minecraft:power 3");
                }
                p.teleportTo(server.overworld(), 0.5, 105, 0.5, Set.of(), 0, 0, false);
                p.setNoGravity(true);
                p.setOnGround(false);
                p.setDeltaMovement(Vec3.ZERO);
                if (cow != null) cow.discard();
                if (projectile != null) projectile.discard();
                cow = EntityTypes.COW.create(p.level(), EntitySpawnReason.COMMAND);
                cow.setPos(4, 105, 4);
                cow.setNoGravity(true);
                cow.setNoAi(true);
                p.level().addFreshEntity(cow);
                projectile = EntityTypes.ARROW.create(p.level(), EntitySpawnReason.COMMAND);
                projectile.setPos(15, 105, 4);
                projectile.setNoGravity(true);
                projectile.setDeltaMovement(0.2, 0, 0);
                p.level().addFreshEntity(projectile);
                server.tickRateManager().setTickRate(20);
                return snapshot(p);
            });
            state = 2;
        } else if (state == 2 && pending.isDone() && client.player.getY() > 100 && client.player.getMainHandItem().is(Items.BOW)) {
            pending.join();
            client.player.setNoGravity(true);
            client.options.keyUse.setDown(true);
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            Case sample = CASES[index];
            pending = submit(server, () -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.startUsingItem(InteractionHand.MAIN_HAND);
                TimeResearchClientSmokeTest.check(dev.wildcraft.focus.FocusTime.active(server), "Automatic production skill entered");
                return snapshot(p);
            });
            state = 3;
        } else if (state == 3 && pending.isDone()) {
            before = pending.join();
            deadline = now + 1_800_000_000L;
            frames = 0;
            yawChanges = 0;
            previousYaw = client.player.getYRot();
            state = 4;
            if (CASES[index].stall()) server.execute(() -> {
                try { Thread.sleep(700); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            });
        } else if (state == 4) {
            frames++;
            // Send small synthetic relative mouse events through the native frame handler.
            client.mouseHandler.onMove(client.getWindow().handle(), 0, 0, 0.3, 0);
            if (client.player.getYRot() != previousYaw) yawChanges++;
            previousYaw = client.player.getYRot();
            if (now >= deadline) {
                pending = submit(server, () -> snapshot(server.getPlayerList().getPlayers().getFirst()));
                state = 5;
            }
        } else if (state == 5 && pending.isDone()) {
            held = pending.join();
            releasedAt = System.nanoTime();
            client.options.keyUse.setDown(false);
            deadline = now + 600_000_000L;
            state = 6;
        } else if (state == 6 && now >= deadline) {
            pending = submit(server, () -> snapshot(server.getPlayerList().getPlayers().getFirst()));
            state = 7;
        } else if (state == 7 && pending.isDone()) {
            Snapshot after = pending.join();
            Case sample = CASES[index];
            long ticks = held.time() - before.time();
            int used = held.used() - before.used();
            double seconds = (held.sampledAt() - before.sampledAt()) / 1_000_000_000.0;
            String row = String.format(java.util.Locale.ROOT,
                "%s | measuredSampleSeconds=%.3f | worldTicks=%d | cowTicks=%d | projectileTicks=%d | projectileDx=%.3f | bowTicks=%d | power=%.3f | billedAtLastTick=%.3f | frames=%d | mouseFrames=%d | bornArrows=%d | ammoUsed=%d | durability=%d | releaseToServerMs=%.1f | weaponEnchantments=%s | maxChargeStepSeconds=%.3f",
                sample.name(), seconds, ticks, held.cowTicks()-before.cowTicks(), held.projectileTicks()-before.projectileTicks(), held.projectileX()-before.projectileX(), used,
                net.minecraft.world.item.BowItem.getPowerForTime(used), before.stamina()-held.stamina(), frames, yawChanges,
                after.bornArrows()-before.bornArrows(), before.ammunition()-after.ammunition(), after.damage()-before.damage(), (lastShotAt-releasedAt)/1_000_000.0, shotHasEnchantments, held.maxChargeStep());
            System.out.println("WILDCRAFT P3.2 MEASURE " + row);
            RESULTS.add(row);
            TimeResearchClientSmokeTest.check(after.bornArrows()-before.bornArrows() == 1, "Exactly one native arrow spawned per release, even if it later hits a target");
            TimeResearchClientSmokeTest.check(held.bornArrows() == before.bornArrows() && held.used() >= before.used(), "No premature shot during held input");
            TimeResearchClientSmokeTest.check(before.ammunition()-after.ammunition() == (sample.enchanted() ? 0 : 1) && after.damage()-before.damage() == 1, "Native ammo and durability once, including Infinity");
            TimeResearchClientSmokeTest.check(!sample.enchanted() || shotHasEnchantments, "Released arrow retains enchanted weapon context");
            TimeResearchClientSmokeTest.check(sample.rate() == 20 || sample.stall() || Math.abs((before.stamina()-held.stamina()) - 18) < 5, "Real-time stamina is independent of TPS");
            TimeResearchClientSmokeTest.check(sample.rate() != 5 || sample.compensate() || used < 15, "Naive slowdown stretches bow charge");
            TimeResearchClientSmokeTest.check(!sample.compensate() || used >= (sample.stall() ? 20 : 30), "Necessary timer compensation restores vanilla curve");
            TimeResearchClientSmokeTest.check(!sample.stall() || held.maxChargeStep() == 0.25, "Actual 700ms server stall bills at most 250ms in one step");
            TimeResearchClientSmokeTest.check(yawChanges > ticks, "Native aim updates on render frames between slow ticks");
            TimeResearchClientSmokeTest.check(!sample.compensate() || lastShotAt - releasedAt < 100_000_000L, "Frame release avoids slow-tick input delay");
            if (++index == CASES.length) {
                server.execute(() -> {
                    dev.wildcraft.focus.FocusTime.close(server, true, false);
                    var p = server.getPlayerList().getPlayers().getFirst();
                    p.setNoGravity(false);
                    p.stopUsingItem();
                });
                System.out.println("WILDCRAFT P3.2 normal client measurements passed: " + RESULTS.size() + " cases");
                client.stop();
                state = 8;
            } else {
                deadline = now + 500_000_000L;
                state = 1;
            }
        }
    }

    private static Snapshot snapshot(ServerPlayer p) {
        return new Snapshot(p.level().getGameTime(), System.nanoTime(), p.getTicksUsingItem(), PlayerStamina.get(p).stamina(), cow.tickCount,
                projectile.tickCount, projectile.getX(), bornArrows, p.getInventory().getItem(9).getCount(), p.getMainHandItem().getDamageValue(), dev.wildcraft.focus.FocusTime.maxStep(p.level().getServer()));
    }

    private static CompletableFuture<Snapshot> submit(MinecraftServer server, java.util.function.Supplier<Snapshot> task) {
        CompletableFuture<Snapshot> result = new CompletableFuture<>();
        server.execute(() -> {
            try { result.complete(task.get()); } catch (Throwable e) { result.completeExceptionally(e); }
        });
        return result;
    }
}
