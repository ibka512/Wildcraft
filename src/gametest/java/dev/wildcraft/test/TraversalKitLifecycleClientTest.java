package dev.wildcraft.test;

import dev.wildcraft.client.input.ClimbControls;
import dev.wildcraft.client.input.WildcraftBindings;
import dev.wildcraft.combat.Combat;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.network.MeleeView;
import net.minecraft.world.entity.EquipmentSlot;
import dev.wildcraft.cooking.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.player.*;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.*;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Active traversal shutdown uses native saves and new test worlds; never user-save migration. */
public final class TraversalKitLifecycleClientTest implements FabricClientGameTest {
    private record Kit(ItemStack sword, ItemStack battery, int peak, double capacity, int mealMillis, java.util.UUID targetId, float targetHealth) { }
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private static Kit snapshot(ServerPlayer p) {
        var targets=p.level().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class,new net.minecraft.world.phys.AABB(-6,115,-6,6,130,8));
        var target=targets.isEmpty()?null:targets.getFirst();
        return new Kit(p.getInventory().getItem(0).copy(),p.getInventory().getItem(9).copy(),
                PlayerStamina.get(p).highestLevel(),PlayerStamina.get(p).capacity(),CookingEffects.get(p).warmth().millis(),target==null?null:target.getUUID(),target==null?0:target.getHealth());
    }
    private static void retained(ServerPlayer p, Kit before) {
        check(ItemStack.matches(p.getInventory().getItem(0),before.sword()),"Exact fused worn sword survives native shutdown");
        check(ItemStack.matches(p.getInventory().getItem(9),before.battery()),"Exact finite battery survives native shutdown");
        check(GliderEquipment.get(p).is(WildcraftItems.PARAGLIDER),"Real dedicated equipment survives native shutdown");
        check(PlayerStamina.get(p).highestLevel()==before.peak()&&PlayerStamina.get(p).capacity()==before.capacity(),"Historical stamina cap survives alongside equipment");
        if(before.targetId()!=null){var target=p.level().getEntity(before.targetId());check(target instanceof net.minecraft.world.entity.LivingEntity living&&living.getHealth()==before.targetHealth(),"Native target remains unharmed; no late attack after cancellation or reload");}
        int ms=CookingEffects.get(p).warmth().millis();
        check(ms>before.mealMillis()-5000&&ms<=before.mealMillis(),"Meal counts bounded actual active loading time, not offline time");
        check(!Climbing.active(p)&&!Gliding.active(p)&&!FocusTime.active(p.level().getServer())&&!p.isUsingItem(),"No illegal traversal/use/lease is resumed from persistence");
    }
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save; Kit climbed;
        try(TestSingleplayerContext w=c.worldBuilder().create()) {
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.giveExperienceLevels(30);p.giveExperienceLevels(-25);
                GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));
                var battery=new ItemStack(EnergyContent.BATTERY);check(Batteries.charge(battery,1000)==1000&&Batteries.consume(battery,383),"Finite provided battery spends383");p.getInventory().setItem(9,battery);
                var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(7);var material=new ItemStack(Items.DIAMOND);
                check(FusionItems.attach(sword,material,p.registryAccess())&&material.isEmpty(),"Actual attach consumes one provided material");p.getInventory().setItem(0,sword);
                var meal=new ItemStack(CookingContent.MEAL);meal.set(CookingContent.MEAL_DATA,new MealData(1,MealData.WARMTH,1,120));p.getInventory().setItem(1,meal);
                for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                for(int x=2;x<=5;x++)for(int y=120;y<=138;y++)for(int z=-5;z<=5;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
                p.teleportTo(s.overworld(),1.7,120,.5,Set.of(),-90,0,true);p.setDeltaMovement(Vec3.ZERO);p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(v->Math.abs(v.player.getX()-1.7)<.1&&Math.abs(v.player.getY()-120)<.3);
            c.getInput().pressKey(v->v.keyHotbarSlots[1]);c.waitFor(v->v.player.getMainHandItem().is(CookingContent.MEAL));
            c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.getAttached(CookingEffects.VIEW)!=null&&v.player.getAttached(CookingEffects.VIEW).warmth()==1);c.getInput().releaseKey(v->v.keyUse);
            c.waitFor(v->v.player.getMainHandItem().is(Items.BOWL));c.getInput().pressKey(v->v.keyHotbarSlots[0]);c.waitFor(v->v.player.getMainHandItem().is(Items.IRON_SWORD));
            c.getInput().holdKey(ClimbControls.CLIMB);c.getInput().holdKey(v->v.keyUp);c.waitFor(v->Climbing.active(v.player)&&v.player.getY()>125,150);c.getInput().releaseKey(v->v.keyUp);
            climbed=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(Climbing.active(p)&&!Gliding.active(p),"Actual G remains active at native close");check(PlayerStamina.get(p).stamina()<160,"Actual climbing consumes stamina");return snapshot(p);});
            c.takeScreenshot("kit-close-with-real-G-climb-active");save=w.getWorldSave();
        }
        c.getInput().releaseKey(ClimbControls.CLIMB);
        Kit glided;
        try(TestSingleplayerContext w=save.open()) {
            w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->retained(s.getPlayerList().getPlayers().getFirst(),climbed));
            c.waitFor(v->!Climbing.active(v.player)&&!Gliding.active(v.player));c.takeScreenshot("kit-native-reopen-climb-session-cleared");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setHealth(20);PlayerStamina.fill(p);p.teleportTo(s.overworld(),.5,190,.5,Set.of(),180,0,true);p.setDeltaMovement(Vec3.ZERO);});
            c.waitFor(v->v.gui.screen()==null&&v.gui.overlay()==null&&v.player.getY()>185&&!v.player.onGround()&&GliderEquipment.equipped(v.player)&&Gliding.eligible(v.player));c.getInput().releaseKey(v->v.keyJump);c.waitTicks(2);c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(v->Gliding.active(v.player));c.waitTicks(6);
            glided=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(Gliding.active(p)&&!Climbing.active(p),"Actual equipped jump-toggle glide remains active at close");check(p.getY()<190&&p.fallDistance==0,"Existing glide descends and legitimately resets fall distance; Focus contract is different");return snapshot(p);});
            c.takeScreenshot("kit-close-with-real-canopy-active");save=w.getWorldSave();
        }
        c.getInput().releaseKey(v->v.keyJump);
        Kit guarded;
        try(TestSingleplayerContext w=save.open()) {
            w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();retained(p,glided);check(s.tickRateManager().tickrate()==20,"Native reopen retains lawful20 TPS");});
            c.waitFor(v->!Climbing.active(v.player)&&!Gliding.active(v.player));c.takeScreenshot("kit-native-reopen-glider-session-cleared");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);p.setHealth(20);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));var target=net.minecraft.world.entity.EntityTypes.HUSK.create(s.overworld(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);target.setNoAi(true);target.setPos(.5,120,2.7);s.overworld().addFreshEntity(target);p.inventoryMenu.broadcastChanges();});
            c.waitFor(v->v.gui.screen()==null&&v.gui.overlay()==null&&v.player.onGround()&&v.player.getOffhandItem().is(Items.SHIELD));c.getInput().pressKey(v->v.keyHotbarSlots[0]);c.waitFor(v->v.player.getMainHandItem().is(Items.IRON_SWORD));c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);});c.getInput().holdKey(WildcraftBindings.LOCK_TARGET);c.waitFor(v->Combat.locked(v.player));c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->Combat.view(v.player).defending()&&v.player.isUsingItem());
            guarded=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(Combat.locked(p)&&Combat.view(p).defending()&&p.getUseItem().is(Items.SHIELD),"Actual independent shield defense remains active at close");return snapshot(p);});c.takeScreenshot("kit-close-with-native-shield-use-active");save=w.getWorldSave();
        }
        c.getInput().releaseKey(WildcraftBindings.DEFEND);c.getInput().releaseKey(WildcraftBindings.LOCK_TARGET);
        Kit charged;
        try(TestSingleplayerContext w=save.open()) {
            w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();retained(p,guarded);check(!Combat.locked(p)&&!Combat.view(p).defending()&&MeleeActions.view(p).phase()==MeleeView.IDLE,"Reopen clears shield and melee transient state");check(p.getOffhandItem().is(Items.SHIELD)&&p.getOffhandItem().getDamageValue()==0,"True shield remains owned without artificial block wear");});c.waitFor(v->v.gui.screen()==null&&v.gui.overlay()==null&&v.player.onGround());c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.CHARGE&&MeleeActions.view(v.player).age()>=12);
            var beforeGui=w.getServer().computeOnServer(s->snapshot(s.getPlayerList().getPlayers().getFirst()));c.runOnClient(v->v.pauseGame(false));c.waitFor(v->v.isPaused()&&MeleeActions.view(v.player).phase()==MeleeView.IDLE);c.getInput().releaseKey(v->v.keyAttack);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(ItemStack.matches(p.getMainHandItem(),beforeGui.sword()),"GUI cancellation does not debit wear/Fuse");var target=(net.minecraft.world.entity.LivingEntity)p.level().getEntity(beforeGui.targetId());check(target.getHealth()==beforeGui.targetHealth(),"Actual reachable target is unharmed by GUI cancellation");});c.takeScreenshot("kit-real-charge-GUI-cancel-no-hit");c.runOnClient(v->v.gui.setScreen(null));c.waitFor(v->!v.isPaused()&&v.gui.screen()==null);c.waitTicks(2);c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.CHARGE&&MeleeActions.view(v.player).age()>=12);
            charged=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(MeleeActions.view(p).phase()==MeleeView.CHARGE,"Actual native press has charged without release or debit");return snapshot(p);});c.takeScreenshot("kit-close-with-real-charge-held");save=w.getWorldSave();
        }
        c.getInput().releaseKey(v->v.keyAttack);
        try(TestSingleplayerContext w=save.open()) {
            w.getConnection().waitForChunksRender();c.waitFor(v->v.gui.screen()==null&&v.gui.overlay()==null);c.waitTicks(6);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();retained(p,charged);check(MeleeActions.view(p).phase()==MeleeView.IDLE&&!Combat.locked(p)&&!Combat.view(p).defending(),"No late swing/lock/guard after native reopening");check(p.getOffhandItem().is(Items.SHIELD)&&p.getOffhandItem().getDamageValue()==0,"Real shield remains intact alongside unchanged Fuse and battery");});c.takeScreenshot("kit-native-charge-reopen-no-late-hit");
        }
        System.out.println("C7 provided finite kit/eaten meal active-G/glide/shield/charge native disk-reopen PASS; no session replay, no real-save migration or manual survival claim");
    }
}
