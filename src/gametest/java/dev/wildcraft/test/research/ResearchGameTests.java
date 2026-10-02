package dev.wildcraft.test.research;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class ResearchGameTests {
    @GameTest
    public void fusePreservesMaterialsAndConsumesOnce(GameTestHelper helper) {
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.setDamageValue(123);
        tool.set(DataComponents.CUSTOM_NAME, Component.literal("R0 durability sample"));
        ItemStack container = new ItemStack(Items.SHULKER_BOX);
        container.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(tool.copy(), new ItemStack(Items.DIAMOND, 3))));
        for (ItemStack material : List.of(new ItemStack(Items.COBBLESTONE, 3), tool, container)) {
            ItemStack original = material.copyWithCount(1);
            int count = material.getCount();
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            helper.assertTrue(FuseSample.tryFuse(sword, material, helper.getLevel().registryAccess()), "Material accepted");
            helper.assertValueEqual(material.getCount(), count - 1, "Consume exactly one material");
            helper.assertFalse(FuseSample.tryFuse(sword, new ItemStack(Items.STONE), helper.getLevel().registryAccess()), "Repeated fuse rejected");
            var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
            var encoded = ItemStack.CODEC.encodeStart(ops, sword).getOrThrow();
            ItemStack restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            helper.assertTrue(ItemStack.matches(sword, restored), "Encoded host retains component value equality");
            helper.assertTrue(ItemStack.matches(original, restored.get(ResearchFixtures.FUSE).material()), "All material components restored");
            ItemStack detachedSnapshot = restored.get(ResearchFixtures.FUSE).material();
            detachedSnapshot.setCount(9);
            helper.assertValueEqual(restored.get(ResearchFixtures.FUSE).material().getCount(), 1, "Snapshot cannot be mutated through accessor");
        }
        helper.succeed();
    }

    @GameTest
    public void fuseRejectsOversizeAndMissingTypes(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack huge = new ItemStack(Items.STONE, 2);
        huge.set(DataComponents.CUSTOM_NAME, Component.literal("x".repeat(12000)));
        helper.assertFalse(FuseSample.tryFuse(sword, huge, helper.getLevel().registryAccess()), "Oversize rejected");
        helper.assertValueEqual(huge.getCount(), 2, "Rejected material untouched");
        helper.assertFalse(sword.has(ResearchFixtures.FUSE), "Rejected host untouched");
        JsonObject absent = new JsonObject();
        absent.addProperty("id", "missing-mod:unavailable_item");
        absent.addProperty("count", 1);
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.CODEC.parse(ops, absent).error().isPresent(), "Missing item requires explicit fallback policy");
        helper.succeed();
    }

    @GameTest
    public void machineOwnershipRecoveryAndSave(GameTestHelper helper) {
        MachineSample body = ResearchFixtures.MACHINE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        ItemStack battery = ResearchFixtures.battery(37);
        ItemStack fan = new ItemStack(Items.FEATHER, 2);
        helper.assertTrue(body.tryInstall(0, battery), "Battery installed");
        helper.assertTrue(body.tryInstall(1, fan), "Fan installed");
        helper.assertValueEqual(fan.getCount(), 1, "Install consumes one");
        helper.assertFalse(body.tryInstall(1, fan), "Occupied node rejects duplicate");
        helper.assertFalse(body.tryInstall(4, fan), "Node bound enforced");
        var player = helper.makeMockServerPlayerInLevel();
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        helper.assertFalse(body.tryRecover(0, inventory), "Full inventory leaves part installed");
        helper.assertValueEqual(body.charge(), 37, "Failed recovery preserves charge");
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        body.saveWithoutId(output);
        MachineSample restored = ResearchFixtures.MACHINE.create(helper.getLevel(), EntitySpawnReason.LOAD);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertValueEqual(restored.charge(), 37, "Entity serialization preserves charge");
        helper.assertTrue(restored.part(1).is(Items.FEATHER), "Entity serialization preserves nodes");
        inventory.setItem(0, ItemStack.EMPTY);
        helper.assertTrue(restored.tryRecover(0, inventory), "Recovered to inventory");
        helper.assertValueEqual(inventory.getItem(0).get(ResearchFixtures.BATTERY_CHARGE), 37, "Recovered battery keeps remaining charge");
        helper.assertFalse(restored.tryRecover(0, inventory), "Repeated recovery does not duplicate");
        player.discard();
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void machineMovesCollidesAndCarriesPlayer(GameTestHelper helper) {
        MachineSample body = helper.spawn(ResearchFixtures.MACHINE, new Vec3(2, 2, 2), EntitySpawnReason.COMMAND);
        body.tryInstall(0, ResearchFixtures.battery(100));
        body.tryInstall(1, new ItemStack(Items.FEATHER));
        var rider = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(rider.startRiding(body), "Player can ride body");
        helper.setBlock(new BlockPos(5, 2, 2), Blocks.STONE);
        body.setPowered(true);
        double start = body.getX();
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(body.getX() > start + 1, "Fan moves body");
            helper.assertTrue(body.getBoundingBox().maxX <= helper.absolutePos(new BlockPos(5, 2, 2)).getX() + 0.01, "Body stops at wall");
            helper.assertTrue(rider.getVehicle() == body, "Ride survives movement");
            helper.assertTrue(rider.position().distanceTo(body.position()) < 2, "Rider moves with body");
            helper.assertTrue(body.charge() < 100, "Movement consumes finite charge");
            body.setPowered(false);
            double stopped = body.getX();
            helper.runAfterDelay(5, () -> {
                helper.assertValueEqual(body.getX(), stopped, "Stopped body remains still");
                rider.discard();
                helper.succeed();
            });
        });
    }
}
