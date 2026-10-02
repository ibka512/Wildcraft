package dev.wildcraft.test;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.ClimbView;
import dev.wildcraft.network.GlideInput;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class GlidingGameTests {
    @GameTest
    public void dedicatedSlotTransfersAndRecipe(GameTestHelper h) {
        ServerPlayer p = fixture(h, false);
        var slot = GliderEquipment.slot(p.inventoryMenu);
        h.assertFalse(slot.mayPlace(new ItemStack(Items.STONE)), "Only a paraglider enters equipment slot");
        h.assertValueEqual(slot.getMaxStackSize(), 1, "Equipment capacity is one");
        p.getInventory().setItem(9, new ItemStack(WildcraftItems.PARAGLIDER));
        p.inventoryMenu.quickMoveStack(p, 9);
        h.assertTrue(GliderEquipment.equipped(p), "Shift move equips to dedicated slot");
        h.assertTrue(p.getInventory().getItem(9).isEmpty(), "Source consumed once");
        p.inventoryMenu.quickMoveStack(p, slot.index);
        h.assertFalse(GliderEquipment.equipped(p), "Shift move unequips");
        h.assertValueEqual(p.getInventory().countItem(WildcraftItems.PARAGLIDER), 1, "Unequip does not duplicate");
        var recipe = h.getLevel().recipeAccess().byKey(ResourceKey.<Recipe<?>>create(Registries.RECIPE, Wildcraft.id("paraglider"))).orElseThrow().value();
        var input = CraftingInput.of(3, 3, List.of(new ItemStack(Items.LEATHER), new ItemStack(Items.LEATHER), new ItemStack(Items.LEATHER),
                new ItemStack(Items.STICK), ItemStack.EMPTY, new ItemStack(Items.STICK), new ItemStack(Items.STICK), ItemStack.EMPTY, new ItemStack(Items.STICK)));
        var shaped = (net.minecraft.world.item.crafting.ShapedRecipe) recipe;
        h.assertTrue(shaped.matches(input, h.getLevel()), "Native resource recipe recognizes ingredients");
        h.assertTrue(shaped.assemble(input).is(WildcraftItems.PARAGLIDER), "Recipe creates the actual item");
        h.assertValueEqual(shaped.assemble(input).getCount(), 1, "One result per recipe");
        p.discard();
        h.succeed();
    }

    @GameTest
    public void equipmentAuthorityCostsAndHands(GameTestHelper h) {
        ServerPlayer p = fixture(h, false);
        p.getInventory().setItem(0, new ItemStack(WildcraftItems.PARAGLIDER));
        Gliding.receive(p, GlideInput.OPEN);
        h.assertFalse(Gliding.active(p), "Inventory glider cannot authorize flight");
        Gliding.receive(p, GlideInput.CLOSED);
        GliderEquipment.set(p, new ItemStack(WildcraftItems.PARAGLIDER));
        p.setAttached(Climbing.VIEW, new ClimbView(true, false));
        double before = PlayerStamina.get(p).stamina();
        for (int i = 0; i < 100; i++) Gliding.receive(p, GlideInput.OPEN);
        h.assertTrue(Gliding.active(p) && !Climbing.active(p), "Opening releases climbing");
        h.assertValueEqual(PlayerStamina.get(p).stamina(), before, "Packets do not multiply fees");
        Gliding.tick(p);
        h.assertTrue(Math.abs(PlayerStamina.get(p).stamina() - (before - Gliding.COST)) < 1e-6, "One fee per game tick");
        h.assertTrue(p.getMainHandItem().is(WildcraftItems.PARAGLIDER), "Holstering preserves the real item");
        p.getInventory().setSelectedSlot(1);
        Gliding.tick(p);
        h.assertFalse(Gliding.active(p), "Taking a different hotbar item closes canopy");
        Gliding.reset(p, false);
        Gliding.receive(p, GlideInput.OPEN);
        GliderEquipment.set(p, ItemStack.EMPTY);
        Gliding.tick(p);
        h.assertFalse(Gliding.active(p), "Removing equipment closes canopy");
        p.discard();
        h.succeed();
    }

    @GameTest
    public void landingDamageExhaustionAndVanillaModes(GameTestHelper h) {
        ServerPlayer p = fixture(h, true);
        Gliding.receive(p, GlideInput.OPEN);
        ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(p, p.damageSources().generic(), 1, 1, false);
        h.assertFalse(Gliding.active(p), "Damage closes canopy");
        Gliding.receive(p, GlideInput.OPEN);
        h.assertFalse(Gliding.active(p), "Heartbeat cannot re-open an interrupted canopy");
        Gliding.reset(p, false);
        PlayerStamina.consume(p, PlayerStamina.get(p).stamina() - 0.1);
        Gliding.receive(p, GlideInput.OPEN);
        Gliding.tick(p);
        h.assertValueEqual(PlayerStamina.get(p).stamina(), 0.0, "Exhaustion clamps to zero");
        h.assertFalse(Gliding.active(p), "Exhaustion ends glide");
        PlayerStamina.fill(p);
        Gliding.reset(p, false);
        p.setOnGround(true);
        Gliding.receive(p, GlideInput.OPEN);
        h.assertFalse(Gliding.active(p), "Ground input cannot open");
        p.setOnGround(false);
        Gliding.reset(p, false);
        Gliding.receive(p, GlideInput.OPEN);
        p.setOnGround(true);
        Gliding.tick(p);
        h.assertFalse(Gliding.active(p), "Landing closes automatically");
        p.setOnGround(false);
        p.getAbilities().mayfly = true;
        h.assertFalse(Gliding.eligible(p), "Creative flight keeps its own input");
        p.getAbilities().mayfly = false;
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        h.assertFalse(Gliding.eligible(p), "Equipped elytra takes priority");
        p.discard();
        h.succeed();
    }

    @GameTest
    public void movementAndNativeSaveLoad(GameTestHelper h) {
        ServerPlayer p = fixture(h, true);
        Gliding.receive(p, GlideInput.OPEN);
        h.assertTrue(Gliding.acceptMovement(p, p.getX() + 0.24, p.getY() - 0.08, p.getZ()), "Normal descent allowed");
        h.assertFalse(Gliding.acceptMovement(p, p.getX() + 3, p.getY(), p.getZ()), "Large movement rejected");
        Gliding.reset(p, false);
        Gliding.receive(p, GlideInput.OPEN);
        var stamina = PlayerStamina.get(p);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        p.saveWithoutId(output);
        h.assertTrue(output.buildResult().toString().contains("wildcraft:glider_equipment"), "Equipment is serialized");
        h.assertFalse(output.buildResult().toString().contains("wildcraft:glide_"), "Glide session is not serialized");
        ServerPlayer restored = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), p.getGameProfile(), p.clientInformation());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(GliderEquipment.equipped(restored), "Native reload preserves equipment");
        h.assertFalse(Gliding.active(restored), "Native reload never opens canopy");
        h.assertValueEqual(PlayerStamina.get(restored), stamina, "Stamina format is preserved");
        Vec3 diagonal = Gliding.movement(-90, new Vec3(1, 0, 1));
        h.assertTrue(diagonal.horizontalDistance() < 0.34 && diagonal.y == Gliding.DESCENT, "Steering is bounded and cannot lift");
        p.discard();
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void staleInputExpires(GameTestHelper h) {
        ServerPlayer p = fixture(h, true);
        Gliding.receive(p, GlideInput.OPEN);
        h.assertTrue(Gliding.active(p), "Fresh input opens canopy");
        h.runAfterDelay(23, () -> {
            h.assertFalse(Gliding.active(p), "Missing heartbeat closes canopy");
            p.discard();
            h.succeed();
        });
    }

    @GameTest
    public void fullInventoryAndDeathDropOnce(GameTestHelper h) {
        ServerPlayer p = fixture(h, true);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        var slot = GliderEquipment.slot(p.inventoryMenu);
        h.assertTrue(p.inventoryMenu.quickMoveStack(p, slot.index).isEmpty(), "Full inventory rejects unequip");
        h.assertTrue(GliderEquipment.equipped(p), "Rejected transfer preserves equipped item");
        ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(p, p.damageSources().generic());
        ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(p, p.damageSources().generic());
        h.assertFalse(GliderEquipment.equipped(p), "Death clears equipment after dropping");
        long count = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(2))
                .stream().filter(entity -> entity.getItem().is(WildcraftItems.PARAGLIDER)).count();
        h.assertValueEqual(count, 1L, "Repeated death notification cannot duplicate the glider");
        p.discard();
        h.succeed();
    }

    private static ServerPlayer fixture(GameTestHelper h, boolean equipped) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        p.setPos(origin.getX(), origin.getY() + 20, origin.getZ());
        p.setOnGround(false);
        p.getAbilities().mayfly = false;
        p.getAbilities().flying = false;
        Gliding.reset(p, false);
        PlayerStamina.fill(p);
        GliderEquipment.set(p, equipped ? new ItemStack(WildcraftItems.PARAGLIDER) : ItemStack.EMPTY);
        return p;
    }
}
