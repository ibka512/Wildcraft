package dev.wildcraft.test;

import dev.wildcraft.client.animation.rig.CombatRigLayer;
import dev.wildcraft.client.input.ActionInput;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.level.block.Blocks;

/** Actual RIGHT bow use and native arrow settlement, with the original left-bow audit tolerances. */
public final class RightGroundBowRigClientTest implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static boolean clip(net.minecraft.client.Minecraft client, String suffix) {
        var frame = GroundBowRigClientTest.frame(client);
        return frame != null && frame.clip().equals("bow_ground_" + suffix + "_empty_R");
    }

    private static void cycle(ClientGameTestContext context, TestSingleplayerContext world,
                              PlayerModelType type, InteractionHand hand) {
        GroundBowRigClientTest.prepare(context, world, type, hand);
        HumanoidArm main = hand == InteractionHand.MAIN_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setMainArm(main));
        context.waitFor(client -> client.player.getMainArm() == main);
        context.runOnClient(client -> {
            GroundBowAudit.begin();
            GroundBowAudit.expectActive = true;
            client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        context.getInput().holdKey(v -> v.keyUse);
        context.waitFor(client -> client.player.isUsingItem() && client.player.getUsedItemHand() == hand);
        context.waitFor(client -> clip(client, "draw") && GroundBowRigClientTest.frame(client).weight() > .999);
        context.takeScreenshot("right-ground-bow-draw-" + type + "-" + hand);
        context.waitFor(client -> clip(client, "hold") && RigAudit.hands > 0);
        context.takeScreenshot("right-ground-bow-hold-" + type + "-" + hand);
        context.runOnClient(client -> {
            check(GroundBowAudit.stages.equals(Set.of(0, 1, 2)) && GroundBowAudit.firstHold == 20
                    && GroundBowAudit.draws > 0 && GroundBowAudit.holds > 0,
                    "Native three-stage bow sprite and source hold begin at actual 20-tick use");
            check(RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString()),
                    "Right bow uses the real standard/slim skin texture");
            client.player.setXRot(18);
            client.player.setYRot(25);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        context.waitTicks(2);
        check(context.computeOnClient(client -> clip(client, "hold") && client.player.getXRot() == 18
                && client.player.getYRot() == 25), "Native head and camera remain independently aimed");
        context.takeScreenshot("right-ground-bow-independent-look-" + type + "-" + hand);
        context.runOnClient(client -> GroundBowAudit.expectActive = false);
        context.getInput().releaseKey(v -> v.keyUse);
        context.waitFor(client -> !client.player.isUsingItem() && client.player.getItemInHand(hand).getDamageValue() == 1);
        check(context.computeOnClient(client -> {
            var frame = GroundBowRigClientTest.state(client).getData(CombatRigLayer.FRAME);
            return frame == null || !frame.clip().startsWith("bow_ground_");
        }), "Release immediately relinquishes ground draw/hold; no long source release lock");
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            check(GroundBowAudit.shots.get() == 1 && player.getInventory().getItem(10).getCount() == 31
                    && player.getItemInHand(hand).getDamageValue() == 1 && player.getMainArm() == main,
                    "One real RIGHT arrow, one ammo and one wear with true hand ownership");
        });
        context.takeScreenshot("right-ground-bow-release-native-owner-" + type + "-" + hand);
    }

    private static void boundaries(ClientGameTestContext context, TestSingleplayerContext world) {
        for (String event : new String[]{"slot", "armor", "inventory", "movement"}) {
            GroundBowRigClientTest.prepare(context, world, PlayerModelType.WIDE, InteractionHand.MAIN_HAND);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setMainArm(HumanoidArm.RIGHT);
                player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH));
                player.inventoryMenu.broadcastChanges();
            });
            context.waitFor(client -> client.player.getMainArm() == HumanoidArm.RIGHT);
            context.getInput().holdKey(v -> v.keyUse);
            context.waitFor(client -> clip(client, "hold"));
            if (event.equals("slot")) {
                context.getInput().pressKey(v -> v.keyHotbarSlots[1]);
                context.waitFor(client -> client.player.getMainHandItem().is(net.minecraft.world.item.Items.TORCH));
            } else if (event.equals("armor")) {
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
                    player.inventoryMenu.broadcastChanges();
                });
                context.waitFor(client -> !client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty());
            } else if (event.equals("inventory")) {
                context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
                context.waitTicks(2);
            } else {
                context.getInput().holdKey(v -> v.keyUp);
                context.waitTicks(4);
            }
            check(context.computeOnClient(client -> {
                var frame = GroundBowRigClientTest.frame(client);
                return frame == null || !frame.clip().startsWith("bow_ground_");
            }), "Actual RIGHT " + event + " retains native ownership and removes the source");
            context.takeScreenshot("right-ground-bow-native-exit-" + event);
            context.setScreen(() -> null);
            context.getInput().releaseKey(v -> v.keyUse);
            context.getInput().releaseKey(v -> v.keyUp);
            context.waitTicks(3);
            if (event.equals("slot")) world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                check(GroundBowAudit.shots.get() == 0 && player.getInventory().getItem(10).getCount() == 32
                        && player.getInventory().getItem(0).getDamageValue() == 0,
                        "Native RIGHT slot cancellation adds no shot, ammo or wear");
            });
        }
    }

    @Override public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("time set day");
            world.getServer().runOnServer(server -> {
                for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
                    server.overworld().setBlockAndUpdate(new BlockPos(x, 119, z), Blocks.STONE.defaultBlockState());
                server.getPlayerList().getPlayers().getFirst().teleportTo(.5, 120, .5);
            });
            context.getInput().resizeWindow(1280, 960);
            context.waitFor(client -> ActionInput.tickFrame().gameplay());
            if (!Boolean.getBoolean("wildcraft.test.rightGroundBowBoundaryOnly")) {
                for (var type : new PlayerModelType[]{PlayerModelType.WIDE, PlayerModelType.SLIM})
                    for (var hand : InteractionHand.values()) cycle(context, world, type, hand);
            }
            boundaries(context, world);
            System.out.println("C6AL actual RIGHT mainRIGHT/off-mainLEFT ground native draw20/three sprites/wood grip-nock-fixed segments/head/skin GPU/one arrow-ammo-wear/native slot-armor-inventory-movement exits PASS");
        } finally {
            context.getInput().releaseKey(v -> v.keyUse);
            context.runOnClient(client -> {
                GroundBowAudit.enabled = false;
                RigAudit.enabled = false;
                RigAudit.fixture = null;
                DodgeFeedbackAudit.checkingNativeItem = false;
                client.options.setCameraType(CameraType.FIRST_PERSON);
            });
        }
    }
}
