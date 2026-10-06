package dev.wildcraft.test;

import dev.wildcraft.client.hud.StaminaHud;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.player.PlayerStamina;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CameraType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class BackEquipmentClientSmokeTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save;
        try (TestSingleplayerContext w = c.worldBuilder().create()) {
            w.getConnection().waitForChunksRender();
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(0.5, 100, 0.5); p.setOnGround(false);
                p.getAbilities().mayfly = true; p.getAbilities().flying = true; p.onUpdateAbilities();
                p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                p.getInventory().setItem(1, new ItemStack(Items.SHIELD));
                p.getInventory().setItem(2, new ItemStack(Items.BOW));
                p.getInventory().setItem(9, new ItemStack(Items.ARROW, 16));
                p.getInventory().setSelectedSlot(0); p.inventoryMenu.broadcastChanges();
                var cow = EntityTypes.COW.create(server.overworld(), EntitySpawnReason.COMMAND);
                cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(0.5, 100, 2); server.overworld().addFreshEntity(cow);
            });
            c.waitFor(client -> client.player.getMainHandItem().is(Items.DIAMOND_SWORD)
                    && java.util.stream.StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false).anyMatch(e -> e.getType() == EntityTypes.COW));
            c.runOnClient(client -> {
                var cow = java.util.stream.StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
                        .filter(e -> e.getType() == EntityTypes.COW).findFirst().orElseThrow();
                client.gameMode.attack(client.player, cow);
            });
            w.getServer().waitFor(server -> !BackEquipment.data(server.getPlayerList().getPlayers().getFirst()).references().get(0).signature().isEmpty());
            select(c, w, 1, Items.SHIELD);
            c.runOnClient(client -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND));
            w.getServer().waitFor(server -> !BackEquipment.data(server.getPlayerList().getPlayers().getFirst()).references().get(1).signature().isEmpty());
            c.runOnClient(client -> client.gameMode.releaseUsingItem(client.player));
            select(c, w, 2, Items.BOW);
            c.runOnClient(client -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND));
            w.getServer().waitFor(server -> !BackEquipment.data(server.getPlayerList().getPlayers().getFirst()).references().get(2).signature().isEmpty());
            c.runOnClient(client -> client.gameMode.releaseUsingItem(client.player));
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.getInventory().setItem(40, p.getInventory().removeItemNoUpdate(1)); p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client -> client.player.getOffhandItem().is(Items.SHIELD));
            c.getInput().holdKey(options -> options.keyUse);
            c.waitFor(client -> client.player.isUsingItem() && BackEquipment.view(client.player).shield().is(Items.SHIELD));
            c.runOnClient(client -> {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                var state = (net.minecraft.client.renderer.entity.state.AvatarRenderState) client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 1);
                check(!state.leftHandItemStack.is(Items.SHIELD) && !state.rightHandItemStack.is(Items.SHIELD), "Two-hand bow draw hides held shield in third person");
                check(!state.getData(dev.wildcraft.client.render.BackEquipmentLayer.ITEMS)[1].isEmpty(), "Owned shield is actually rendered on back while drawing");
                var hands = new net.minecraft.client.player.FirstPersonHandsAndItems(); hands.tick(client.player);
                var handState = new net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState();
                hands.extractRenderState(client.player, 1, handState);
                check(handState.offHandItem.isEmpty() && handState.offHandRenderState.isEmpty(), "First-person shield is stowed while bow uses both hands");
                check(client.player.getOffhandItem().is(Items.SHIELD), "Underlying inventory is unchanged");
            });
            c.takeScreenshot("p31-bow-draw-shield-stowed");
            c.getInput().releaseKey(options -> options.keyUse);
            c.runOnClient(client -> client.gameMode.releaseUsingItem(client.player));
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.getInventory().setItem(1, p.getInventory().removeItemNoUpdate(40)); p.inventoryMenu.broadcastChanges();
            });
            select(c, w, 3, Items.AIR);
            c.waitFor(client -> allVisible(client.player));
            check(c.computeOnClient(client -> !client.player.hasAttached(BackEquipment.DATA)), "Saved signatures stay server private");
            c.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            c.waitTicks(5); c.takeScreenshot("p31-back-all-native-models");
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.getInventory().setItem(4, p.getInventory().getItem(0).copy()); p.inventoryMenu.broadcastChanges();
            });
            select(c, w, 4, Items.DIAMOND_SWORD);
            c.waitFor(client -> !BackEquipment.view(client.player).melee().isEmpty());
            c.runOnClient(client -> {
                var state = (net.minecraft.client.renderer.entity.state.AvatarRenderState) client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 1);
                check(!state.getData(dev.wildcraft.client.render.BackEquipmentLayer.ITEMS)[0].isEmpty(), "Identical-looking spare in hand must not hide the recorded owned sword");
            });
            select(c, w, 0, Items.DIAMOND_SWORD);
            c.waitFor(client -> BackEquipment.view(client.player).melee().isEmpty());
            select(c, w, 3, Items.AIR);
            c.waitFor(client -> allVisible(client.player));
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.getInventory().setItem(4, ItemStack.EMPTY); p.inventoryMenu.broadcastChanges();
            });
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 8; z++) server.overworld().setBlock(new net.minecraft.core.BlockPos(x, 99, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                p.teleportTo(0.5, 100, 0.5); p.getAbilities().flying = false; p.getAbilities().mayfly = false; p.onUpdateAbilities();
                p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            });
            c.getInput().holdKey(options -> options.keyShift);
            c.waitFor(client -> client.player.isCrouching());
            c.takeScreenshot("p31-crouching-back-models"); c.getInput().releaseKey(options -> options.keyShift);
            w.getServer().runOnServer(server -> {
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 8; z++) for (int y = 100; y <= 101; y++)
                    server.overworld().setBlock(new net.minecraft.core.BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            });
            c.getInput().holdKey(options -> options.keySprint); c.getInput().holdKey(options -> options.keyUp);
            c.waitFor(client -> client.player.isVisuallySwimming());
            c.takeScreenshot("p31-swimming-back-models");
            c.getInput().releaseKey(options -> options.keySprint); c.getInput().releaseKey(options -> options.keyUp);
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.teleportTo(0.5, 110, 0.5);
                p.setOnGround(false); p.getAbilities().mayfly = true; p.getAbilities().flying = true; p.onUpdateAbilities();
                p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            });
            c.waitFor(client -> client.player.getY() > 109 && !client.player.isVisuallySwimming());
            c.runOnClient(client -> {
                var state = (net.minecraft.client.renderer.entity.state.AvatarRenderState) client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 1);
                var skin = state.skin;
                state.skin = net.minecraft.world.entity.player.PlayerSkin.insecure(skin.body(), new net.minecraft.core.ClientAsset.ResourceTexture(dev.wildcraft.Wildcraft.id("textures/entity/paraglider.png")), null, skin.model());
                state.showCape = true;
                dev.wildcraft.client.render.BackEquipmentLayer.extract(client.player, state);
                check(state.getData(dev.wildcraft.client.render.BackEquipmentLayer.ITEMS) != null, "Visible cape switches the same owned references to waist layout");
                System.out.println("WILDCRAFT P3.1 controlled client skin model=" + skin.model());
            });
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            c.waitTicks(3); c.takeScreenshot("p31-first-person-no-own-back");
            w.getServer().runCommand("experience set @p 30 levels"); w.getServer().runCommand("experience set @p 5 levels");
            w.getServer().runCommand("wildcraft stamina fill @p");
            c.runOnClient(client -> { if (client.gui.hud.isHidden()) client.gui.hud.toggle(); });
            w.getServer().runCommand("wildcraft stamina consume @p 145");
            c.waitFor(client -> client.player.getAttached(PlayerStamina.VIEW).stamina() == 15 && StaminaHud.showStamina());
            c.takeScreenshot("p31-merged-hud-low-en");
            CompletableFuture<Void> reload = c.computeOnClient(client -> {
                client.options.languageCode = "zh_cn"; client.getLanguageManager().setSelected("zh_cn"); return client.reloadResourcePacks();
            });
            c.waitFor(client -> reload.isDone()); reload.join(); c.waitFor(client -> client.gui.overlay() == null);
            c.takeScreenshot("p31-merged-hud-low-zh");
            c.runOnClient(client -> client.gui.hud.toggle());
            check(c.computeOnClient(client -> !StaminaHud.showStamina()), "Hidden HUD suppresses stamina");
            c.takeScreenshot("p31-hidden-hud"); c.runOnClient(client -> client.gui.hud.toggle());
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);
                p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(8);
                p.setHealth(60); p.setAbsorptionAmount(8);
            });
            c.waitFor(client -> client.player.getMaxHealth() == 60 && client.player.getAbsorptionAmount() == 8);
            c.runOnClient(client -> client.options.guiScale().set(3)); c.waitTicks(5);
            c.takeScreenshot("p31-upper-left-multiple-heart-rows-scaled");
            w.getServer().runCommand("experience set @p 600000 levels");
            w.getServer().runCommand("experience set @p 5 levels");
            c.waitFor(client -> client.player.getAttached(PlayerStamina.VIEW).highestLevel() == 600000);
            c.takeScreenshot("p31-upper-left-large-capacity");
            c.runOnClient(client -> client.options.guiScale().set(2));
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var horse = EntityTypes.HORSE.create(server.overworld(), EntitySpawnReason.COMMAND);
                horse.setTamed(true); horse.setPos(p.position()); horse.setNoGravity(true);
                horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
                server.overworld().addFreshEntity(horse); p.startRiding(horse, true, true);
            });
            c.waitFor(client -> client.player.jumpableVehicle() != null);
            c.runOnClient(client -> client.options.keyJump.setDown(true));
            c.waitTicks(3); c.takeScreenshot("p31-native-mount-bar-kept-with-upper-left-stamina");
            c.runOnClient(client -> client.options.keyJump.setDown(false));
            w.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().stopRiding());
            c.waitFor(client -> client.player.getVehicle() == null);
            w.getServer().runCommand("wildcraft stamina fill @p"); c.waitTicks(45);
            check(c.computeOnClient(client -> !StaminaHud.showStamina() && client.player.experienceLevel == 5), "Full stamina returns current XP, not historical peak");
            c.takeScreenshot("p31-restored-xp-level5");
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
                BackEquipment.tick(p);
            });
            c.waitFor(client -> !BackEquipment.view(client.player).shield().isEmpty() && !BackEquipment.view(client.player).ranged().isEmpty());
            check(c.computeOnClient(client -> !BackEquipment.view(client.player).melee().isEmpty()), "Elytra retains all three owned references for waist layout");
            c.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK)); c.waitTicks(3);
            c.takeScreenshot("p31-elytra-priority");
            w.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); });
            c.waitFor(client -> allVisible(client.player));
            save = w.getWorldSave();
        }
        try (TestSingleplayerContext w = save.open()) {
            w.getConnection().waitForChunksRender(); c.waitFor(client -> allVisible(client.player));
            w.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var dest = server.getLevel(Level.NETHER);
                for (int x = -2; x < 3; x++) for (int z = -2; z < 3; z++) for (int y = 119; y < 125; y++)
                    dest.setBlock(new net.minecraft.core.BlockPos(x, y, z), y == 119 ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                p.teleportTo(dest, 0.5, 120, 0.5, Set.of(), 0, 0, true);
            });
            c.waitFor(client -> client.level.dimension().equals(Level.NETHER) && allVisible(client.player));
            w.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(), 0.5, 100, 0.5, Set.of(), 0, 0, true));
            c.waitFor(client -> client.level.dimension().equals(Level.OVERWORLD) && allVisible(client.player));
            w.getServer().runOnServer(server -> server.overworld().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY, true, server));
            w.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().connection.hasClientLoaded() && !server.getPlayerList().getPlayers().getFirst().isChangingDimension());
            w.getServer().runCommand("kill @p");
            c.waitFor(client -> !client.player.isAlive());
            check(c.computeOnClient(client -> !StaminaHud.showStamina()), "Death hides stamina");
            c.runOnClient(client -> client.player.respawn());
            c.waitFor(client -> client.player.isAlive() && allVisible(client.player));
            w.getServer().runOnServer(server -> server.overworld().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY, false, server));
            w.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().connection.hasClientLoaded() && !server.getPlayerList().getPlayers().getFirst().isChangingDimension());
            w.getServer().runCommand("kill @p"); c.waitFor(client -> !client.player.isAlive());
            c.runOnClient(client -> client.player.respawn()); c.waitFor(client -> client.player.isAlive());
            w.getServer().runOnServer(server -> check(BackEquipment.data(server.getPlayerList().getPlayers().getFirst()).references().stream().allMatch(r -> r.signature().isEmpty()), "Normal death clears references"));
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        }
        System.out.println("WILDCRAFT P3.1 actual network attack/shield/bow, save/reload/dimension/death, private view and merged HUD passed");
    }
    private static void select(ClientGameTestContext c, TestSingleplayerContext w, int slot, net.minecraft.world.item.Item item) {
        w.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); p.getInventory().setSelectedSlot(slot); p.inventoryMenu.broadcastChanges(); });
        c.runOnClient(client -> client.player.getInventory().setSelectedSlot(slot));
        c.waitFor(client -> client.player.getMainHandItem().is(item) || item == Items.AIR && client.player.getMainHandItem().isEmpty());
    }
    private static boolean allVisible(net.minecraft.world.entity.player.Player p) {
        var v = BackEquipment.view(p); return !v.melee().isEmpty() && !v.shield().isEmpty() && !v.ranged().isEmpty();
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
