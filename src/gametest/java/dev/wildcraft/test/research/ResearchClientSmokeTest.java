package dev.wildcraft.test.research;

import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Real chunk/player saves supplement the in-memory R0 codec tests. */
public final class ResearchClientSmokeTest implements FabricClientGameTest {
    private record SavedSample(UUID machine, UUID item, int startingChunk, ItemStack material) { }
    private record MachineSnapshot(int charge, double x) { }

    @Override
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        SavedSample sample;
        MachineSnapshot snapshot;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            sample = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                double x = Math.floor((player.getX() + 8) / 16) * 16 + 15;
                double z = player.getZ() + 6;
                level.getChunk(BlockPos.containing(x + 4, player.getY() + 4, z));
                MachineSample machine = ResearchFixtures.MACHINE.create(level, EntitySpawnReason.COMMAND);
                machine.setPos(x, player.getY() + 4, z);
                machine.tryInstall(0, ResearchFixtures.battery(80));
                machine.tryInstall(1, new ItemStack(Items.FEATHER));
                machine.setPowered(true);
                check(level.addFreshEntity(machine), "Research body spawned");

                ItemStack material = new ItemStack(Items.SHULKER_BOX);
                ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
                tool.setDamageValue(71);
                material.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(tool)));
                ItemStack original = material.copy();
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                check(FuseSample.tryFuse(sword, material, level.registryAccess()), "Complex material fused");
                player.getInventory().setItem(1, sword);
                BlockPos chestPos = player.blockPosition().offset(3, 0, 3);
                level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
                ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
                chest.setItem(0, player.getInventory().removeItemNoUpdate(1));
                check(chest.getItem(0).has(ResearchFixtures.FUSE), "Fuse survives inventory to chest transfer");
                ItemEntity drop = new ItemEntity(level, player.getX() + 8, player.getY() + 3, player.getZ() + 3,
                        chest.removeItemNoUpdate(0));
                drop.setDeltaMovement(Vec3.ZERO);
                drop.setNeverPickUp();
                check(level.addFreshEntity(drop), "Fuse transferred from chest to dropped entity");
                return new SavedSample(machine.getUUID(), drop.getUUID(), machine.chunkPosition().x(), original);
            });
            world.getServer().waitFor(server -> {
                Entity entity = find(server.overworld(), sample.machine());
                return entity instanceof MachineSample body && body.chunkPosition().x() > sample.startingChunk();
            });
            snapshot = world.getServer().computeOnServer(server -> {
                MachineSample body = (MachineSample) find(server.overworld(), sample.machine());
                body.setPowered(false);
                check(body.charge() < 80 && body.charge() > 0, "Crossing consumes finite energy");
                return new MachineSnapshot(body.charge(), body.getX());
            });
            save = world.getWorldSave();
        }

        try (TestSingleplayerContext world = save.open()) {
            world.getConnection().waitForChunksRender();
            world.getServer().waitFor(server -> find(server.overworld(), sample.machine()) != null
                    && find(server.overworld(), sample.item()) != null);
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                MachineSample body = (MachineSample) find(level, sample.machine());
                check(body.charge() == snapshot.charge(), "Actual chunk reload preserves remaining energy");
                check(Math.abs(body.getX() - snapshot.x()) < 0.001, "Actual chunk reload preserves position");
                check(body.part(1).is(Items.FEATHER), "Actual chunk reload preserves installed fan");
                ItemEntity drop = (ItemEntity) find(level, sample.item());
                check(ItemStack.matches(sample.material(), drop.getItem().get(ResearchFixtures.FUSE).material()),
                        "Actual dropped-item reload preserves nested container and tool damage");
                drop.setNoPickUpDelay();
                var player = server.getPlayerList().getPlayers().getFirst();
                drop.playerTouch(player);
                check(drop.isRemoved(), "Reloaded fuse picked up once");
                check(player.getInventory().getItem(0).has(ResearchFixtures.FUSE), "Fuse returns to player inventory");
            });
        }
        System.out.println("WILDCRAFT R0 real cross-chunk/save/reload/container/drop checks passed");
    }

    private static Entity find(ServerLevel level, UUID uuid) {
        for (Entity entity : level.getAllEntities()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
