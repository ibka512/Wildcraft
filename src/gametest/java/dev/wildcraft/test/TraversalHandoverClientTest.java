package dev.wildcraft.test;

import dev.wildcraft.client.animation.rig.CombatRigLayer;
import dev.wildcraft.client.animation.rig.GlideRig;
import dev.wildcraft.client.input.ClimbControls;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.traversal.Gliding;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real G, movement and jump input: traversal owners cannot retain each other's body or canopy. */
public final class TraversalHandoverClientTest implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static AvatarRenderState state(Minecraft client) {
        return (AvatarRenderState) client.getEntityRenderDispatcher().getRenderer(client.player)
                .createRenderState(client.player, 1);
    }

    private static boolean clip(Minecraft client, String name) {
        var frame = state(client).getData(CombatRigLayer.FRAME);
        return frame != null && frame.clip().equals(name);
    }

    private static PlayerSkin skin(PlayerModelType type) {
        for (int i = 0; i < 100; i++) {
            var skin = DefaultPlayerSkin.get(new UUID(0, i));
            if (skin.model() == type) return skin;
        }
        throw new AssertionError("Native skin fixture");
    }

    private static void release(ClientGameTestContext context) {
        context.getInput().releaseKey(ClimbControls.CLIMB);
        context.getInput().releaseKey(v -> v.keyUp);
        context.getInput().releaseKey(v -> v.keyJump);
    }

    private static Vec3 origin(Direction face) {
        return new Vec3(face.getStepX() == 0 ? .5 : face.getStepX() > 0 ? 4.7 : -3.7,
                250, face.getStepZ() == 0 ? .5 : face.getStepZ() > 0 ? 4.7 : -3.7);
    }

    private static void prepare(ClientGameTestContext context, TestSingleplayerContext world,
                                PlayerModelType type, Direction face) {
        release(context);
        context.setScreen(() -> null);
        context.waitTicks(3);
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            Gliding.reset(player, false);
            Climbing.reset(player, false);
            player.getInventory().clearContent();
            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) player.setItemSlot(slot, ItemStack.EMPTY);
            GliderEquipment.set(player, new ItemStack(WildcraftItems.PARAGLIDER));
            player.setHealth(20);
            PlayerStamina.fill(player);
            var at = origin(face);
            player.teleportTo(player.level(), at.x, at.y, at.z, Set.of(), face.toYRot(), 0, true);
            player.setDeltaMovement(Vec3.ZERO);
            player.inventoryMenu.broadcastChanges();
        });
        context.waitFor(client -> client.player.position().distanceTo(origin(face)) < .35
                && GliderEquipment.equipped(client.player) && client.player.getMainHandItem().isEmpty()
                && client.player.getOffhandItem().isEmpty());
        context.runOnClient(client -> {
            client.player.setYRot(face.toYRot());
            client.player.setXRot(0);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            RigAudit.fixture = skin(type);
            RigAudit.begin();
        });
    }

    private static void cycle(ClientGameTestContext context, TestSingleplayerContext world,
                              PlayerModelType type, Direction face) {
        prepare(context, world, type, face);
        context.getInput().holdKey(ClimbControls.CLIMB);
        context.getInput().holdKey(v -> v.keyUp);
        context.waitFor(client -> Climbing.active(client.player) && clip(client, "climb_up"));
        context.getInput().releaseKey(v -> v.keyUp);
        context.waitFor(client -> clip(client, "climb_idle_up"));
        context.takeScreenshot("traversal-climb-" + type + "-" + face);

        context.getInput().holdKeyFor(v -> v.keyJump, 1);
        context.waitFor(client -> Gliding.active(client.player) && !Climbing.active(client.player)
                && clip(client, "glide_hold"));
        context.waitTicks(3);
        check(context.computeOnClient(client -> Gliding.active(client.player)
                && !Climbing.active(client.player) && state(client).getData(GlideRig.CANOPY) != null
                && RigAudit.bodies > 0 && RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),
                "Held G cannot steal actual glider and native source skin");
        context.takeScreenshot("traversal-glide-held-G-" + type + "-" + face);

        context.getInput().holdKeyFor(v -> v.keyJump, 1);
        context.waitFor(client -> !Gliding.active(client.player));
        context.waitTicks(2);
        check(context.computeOnClient(client -> !Climbing.active(client.player)
                && state(client).getData(GlideRig.CANOPY) == null),
                "Closing canopy with held G preserves the existing blocked re-grip rule");
        context.takeScreenshot("traversal-closed-held-G-" + type + "-" + face);

        context.getInput().releaseKey(ClimbControls.CLIMB);
        context.waitTicks(2);
        context.getInput().holdKey(ClimbControls.CLIMB);
        context.waitFor(client -> Climbing.active(client.player) && clip(client, "climb_idle_up"));
        check(context.computeOnClient(client -> !Gliding.active(client.player)
                && state(client).getData(GlideRig.CANOPY) == null), "Fresh grip owns body without old canopy");
        context.takeScreenshot("traversal-fresh-regrip-" + type + "-" + face);

        context.getInput().holdKeyFor(v -> v.keyJump, 1);
        context.waitFor(client -> Gliding.active(client.player) && clip(client, "glide_hold"));
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitFor(client -> !Gliding.active(client.player) && !Climbing.active(client.player));
        check(context.computeOnClient(client -> state(client).getData(GlideRig.CANOPY) == null
                && state(client).getData(CombatRigLayer.FRAME) == null), "Inventory relinquishes both traversal sources");
        context.takeScreenshot("traversal-inventory-clears-" + type + "-" + face);
        context.setScreen(() -> null);
        release(context);
        context.waitTicks(3);
        check(context.computeOnClient(client -> !Gliding.active(client.player) && !Climbing.active(client.player)),
                "Returning to gameplay cannot replay old traversal input");
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            check(player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()
                    && GliderEquipment.equipped(player), "Traversal handover retains real equipment slot and empty inventory");
        });
    }

    @Override public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("time set day");
            world.getServer().runOnServer(server -> {
                for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++)
                    server.overworld().setBlockAndUpdate(new BlockPos(x, 239, z), Blocks.STONE.defaultBlockState());
                for (int a = -9; a <= 9; a++) for (int y = 240; y <= 267; y++) for (int n : new int[]{-5, 5}) {
                    server.overworld().setBlockAndUpdate(new BlockPos(n, y, a), Blocks.STONE.defaultBlockState());
                    server.overworld().setBlockAndUpdate(new BlockPos(a, y, n), Blocks.STONE.defaultBlockState());
                }
            });
            context.getInput().resizeWindow(1280, 960);
            for (var type : new PlayerModelType[]{PlayerModelType.WIDE, PlayerModelType.SLIM})
                for (var face : new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST})
                    cycle(context, world, type, face);
            System.out.println("C6AK real G-wall movement / glide-held-G / fresh re-grip / GUI exit native source and equipment PASS");
        } finally {
            release(context);
            context.runOnClient(client -> {
                RigAudit.enabled = false;
                RigAudit.fixture = null;
                client.options.setCameraType(CameraType.FIRST_PERSON);
            });
        }
    }
}
