package dev.wildcraft.test;

import dev.wildcraft.network.ClimbInput;
import dev.wildcraft.network.ClimbView;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.ClimbSurface;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.block.Blocks;

public final class ClimbingGameTests {
    private static final ClimbInput UP = new ClimbInput(true, (byte) 1, (byte) 0);

    @GameTest
    public void collisionShapesLedgesAndCorners(GameTestHelper h) {
        ServerPlayer p = fixture(h);
        h.assertTrue(ClimbSurface.hasWall(p, Direction.EAST), "Stone face reachable");
        place(p, h, 1.5, 1, 1.5);
        h.assertFalse(ClimbSurface.hasWall(p, Direction.EAST), "Gap cannot become a grip");
        for (int y = 1; y <= 8; y++) h.setBlock(2, y, 1, Blocks.AIR);
        h.setBlock(2, 1, 1, Blocks.STONE_SLAB);
        place(p, h, 1.7, 1, 1.5);
        h.assertTrue(ClimbSurface.hasWall(p, Direction.EAST), "Slab side has collision");
        place(p, h, 1.7, 1.55, 1.5);
        h.assertFalse(ClimbSurface.hasWall(p, Direction.EAST), "Air above slab is not a wall");
        h.setBlock(2, 1, 1, Blocks.SHORT_GRASS);
        place(p, h, 1.7, 1, 1.5);
        h.assertFalse(ClimbSurface.hasWall(p, Direction.EAST), "Plant has no wall collision");
        h.setBlock(2, 1, 1, Blocks.STONE);
        place(p, h, 1.7, 1.96, 1.5);
        h.assertTrue(ClimbSurface.find(p, Direction.EAST, true, true).mantle(), "Clear ledge offers mantle");
        h.setBlock(2, 3, 1, Blocks.STONE);
        var blocked = ClimbSurface.find(p, Direction.EAST, true, true);
        h.assertTrue(blocked == null || !blocked.mantle(), "Overhang prevents mantle");
        h.setBlock(2, 3, 1, Blocks.AIR);
        h.setBlock(2, 1, 1, Blocks.AIR);
        h.setBlock(1, 1, 2, Blocks.STONE);
        place(p, h, 1.7, 1, 1.7);
        h.assertValueEqual(ClimbSurface.find(p, Direction.EAST, true, false).face(), Direction.SOUTH, "Inner corner changes face");
        h.setBlock(1, 1, 2, Blocks.AIR);
        h.setBlock(2, 1, 1, Blocks.STONE);
        place(p, h, 2.01, 1, 2.3);
        h.assertValueEqual(ClimbSurface.find(p, Direction.EAST, true, false).face(), Direction.NORTH, "Outer corner has real adjacent face");
        p.discard();
        h.succeed();
    }

    @GameTest
    public void packetsCannotMultiplyCostOrRecovery(GameTestHelper h) {
        ServerPlayer p = fixture(h);
        p.setOnGround(true);
        double before = PlayerStamina.get(p).stamina();
        for (int i = 0; i < 100; i++) Climbing.receive(p, UP);
        h.assertValueEqual(PlayerStamina.get(p).stamina(), before, "Input does not charge per packet");
        Climbing.tick(p);
        h.assertTrue(Math.abs(PlayerStamina.get(p).stamina() - (before - 0.4)) < 1e-6, "One cost per server tick");
        h.assertFalse(PlayerStamina.canRecover(p), "Ground contact cannot restore while gripping");
        Climbing.receive(p, new ClimbInput(true, (byte) 0, (byte) 0));
        Climbing.tick(p);
        h.assertTrue(Math.abs(PlayerStamina.get(p).stamina() - (before - 0.5)) < 1e-6, "Holding cost");
        Climbing.acceptMovement(p, p.getX(), p.getY() + 0.12, p.getZ());
        Climbing.tick(p);
        h.assertTrue(Math.abs(PlayerStamina.get(p).stamina() - (before - 0.9)) < 1e-6, "Moving cannot claim cheaper idle cost");
        h.assertTrue(p.getAttached(Climbing.VISUAL).motion()==1,"Accepted movement direction is published before clearing the per-tick sample");
        Climbing.tick(p);
        h.assertTrue(p.getAttached(Climbing.VISUAL).motion()==0,"No additional displacement returns to hold");
        Climbing.receive(p, ClimbInput.RELEASED);
        h.assertFalse(Climbing.active(p), "Release ends authorization");
        h.assertTrue(PlayerStamina.canRecover(p), "Ground recovery resumes");
        p.discard();
        h.succeed();
    }

    @GameTest
    public void exhaustionDamageAndInvalidInputLockGrip(GameTestHelper h) {
        ServerPlayer p = fixture(h);
        PlayerStamina.consume(p, PlayerStamina.get(p).stamina() - 0.1);
        Climbing.receive(p, UP);
        Climbing.tick(p);
        h.assertValueEqual(PlayerStamina.get(p).stamina(), 0.0, "Exhaustion clamps to zero");
        h.assertValueEqual(p.getAttached(Climbing.VIEW), new ClimbView(false, true), "Exhaustion locks grip");
        PlayerStamina.fill(p);
        Climbing.receive(p, UP);
        h.assertFalse(Climbing.active(p), "Held input cannot bypass lock");
        Climbing.receive(p, ClimbInput.RELEASED);
        Climbing.receive(p, UP);
        h.assertTrue(Climbing.active(p), "Release and re-press can grab again");
        ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(p, p.damageSources().generic(), 1, 1, false);
        h.assertFalse(Climbing.active(p), "Damage event interrupts grip");
        Climbing.reset(p, false);
        Climbing.receive(p, new ClimbInput(true, (byte) 127, (byte) 0));
        h.assertValueEqual(p.getAttached(Climbing.VIEW), new ClimbView(false, true), "Invalid axis rejected");
        p.discard();
        h.succeed();
    }

    @GameTest
    public void authorizedMovementIsBoundedAndTransient(GameTestHelper h) {
        ServerPlayer p = fixture(h);
        Climbing.receive(p, UP);
        h.assertTrue(Climbing.acceptMovement(p, p.getX(), p.getY() + 0.12, p.getZ()), "Normal step allowed");
        h.assertFalse(Climbing.acceptMovement(p, p.getX(), p.getY() + 3, p.getZ()), "Large step rejected");
        Climbing.reset(p, false);
        Climbing.receive(p, UP);
        var saved = PlayerStamina.get(p);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        p.saveWithoutId(output);
        h.assertFalse(output.buildResult().toString().contains("wildcraft:climb_"), "Active climbing state is not serialized");
        ServerPlayer restored = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), p.getGameProfile(), p.clientInformation());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertFalse(Climbing.active(restored), "Native player load does not restore grip");
        h.assertValueEqual(PlayerStamina.get(restored), saved, "Native player load preserves stamina");
        Climbing.reset(p, true);
        h.assertFalse(Climbing.active(p), "Lifecycle reset discards grip");
        h.assertValueEqual(Climbing.intent(p), ClimbInput.RELEASED, "Lifecycle reset discards input");
        h.assertValueEqual(PlayerStamina.get(p), saved, "Saved stamina preserved");
        p.discard();
        h.succeed();
    }

    private static ServerPlayer fixture(GameTestHelper h) {
        for (int y = 1; y <= 8; y++) h.setBlock(2, y, 1, Blocks.STONE);
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        place(p, h, 1.7, 1, 1.5);
        p.setYRot(-90);
        Climbing.reset(p, false);
        PlayerStamina.fill(p);
        return p;
    }

    @GameTest(maxTicks = 40)
    public void staleInputExpires(GameTestHelper h) {
        ServerPlayer p = fixture(h);
        Climbing.receive(p, UP);
        h.assertTrue(Climbing.active(p), "Fresh input authorizes grip");
        h.runAfterDelay(23, () -> {
            h.assertFalse(Climbing.active(p), "Missing heartbeat releases grip");
            h.assertValueEqual(Climbing.intent(p), ClimbInput.RELEASED, "Stale controls discarded");
            p.discard();
            h.succeed();
        });
    }

    private static void place(ServerPlayer p, GameTestHelper h, double x, double y, double z) {
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        p.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
    }
}
