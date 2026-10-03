package dev.wildcraft.test;

import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.focus.TimeLease;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.network.ClimbInput;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class FocusGameTests {
    @GameTest public void dedicatedNeverSlowsOrBills(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        p.getInventory().setItem(0, new ItemStack(Items.BOW));
        p.getInventory().setItem(9, new ItemStack(Items.ARROW, 64));
        p.setOnGround(false); p.startUsingItem(InteractionHand.MAIN_HAND);
        double before = PlayerStamina.get(p).stamina();
        float rate = p.level().getServer().tickRateManager().tickrate();
        h.assertTrue(!FocusTime.allowedServer(p.level().getServer()), "Dedicated/test server excluded");
        h.assertTrue(!FocusTime.tryBegin(p) && TimeLease.acquire(p.level().getServer(), 5) == null, "Cannot acquire global control");
        FocusTime.pulse(p.level().getServer());
        h.assertTrue(PlayerStamina.get(p).stamina() == before && p.level().getServer().tickRateManager().tickrate() == rate, "No focus cost or global rate change");
        p.discard(); h.succeed();
    }
    @GameTest public void qualificationAndNoSyntheticFlight(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); p.getAbilities().mayfly = false; p.setOnGround(false);
        p.getInventory().setItem(0, new ItemStack(Items.BOW)); p.startUsingItem(InteractionHand.MAIN_HAND);
        h.assertTrue(FocusTime.eligible(p), "Actual airborne bow qualifies structurally");
        p.setOnGround(true); h.assertTrue(!FocusTime.eligible(p), "Ground excluded"); p.setOnGround(false);
        p.getAbilities().flying = true; h.assertTrue(!FocusTime.eligible(p), "Creative flight excluded"); p.getAbilities().flying = false;
        Climbing.receive(p, new ClimbInput(true, (byte)0, (byte)0));
        h.assertTrue(!FocusTime.eligible(p), "Climb intent blocks focus before movement activates"); Climbing.receive(p, ClimbInput.RELEASED);
        p.getInventory().setItem(0, new ItemStack(Items.BOW));
        h.assertTrue(!FocusTime.eligible(p), "Replacing bow with identical spare terminates ownership");
        p.stopUsingItem(); p.getInventory().setItem(0, new ItemStack(Items.CROSSBOW)); p.startUsingItem(InteractionHand.MAIN_HAND);
        h.assertTrue(!FocusTime.eligible(p), "Crossbow remains vanilla"); p.stopUsingItem();
        p.setDeltaMovement(new Vec3(0.2, -1, 0.3)); p.fallDistance = 12;
        FocusTime.limitDescent(p);
        h.assertTrue(p.getDeltaMovement().y == -1 && p.fallDistance == 12 && !p.getAbilities().flying, "Inactive path never grants flight or resets fall distance");
        p.discard(); h.succeed();
    }
}
