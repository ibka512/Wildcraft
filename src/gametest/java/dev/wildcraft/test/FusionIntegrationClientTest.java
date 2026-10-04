package dev.wildcraft.test;

import dev.wildcraft.cooking.*;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.fuse.*;
import dev.wildcraft.player.*;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Native eating, ground melee/back, air focus, Infinity fusion impact and real disk reload together. */
public final class FusionIntegrationClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        FusionData[] retained={null};int[] savedMeal={0};TestWorldSave save;
        try(TestSingleplayerContext world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
                for(int x=-4;x<=4;x++)for(int z=-4;z<=25;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                p.teleportTo(level,.5,120,.5,Set.of(),0,0,false);p.getInventory().clearContent();p.setAttached(PlayerStamina.DATA,new StaminaData(1,20,140,0));p.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE));p.getInventory().setItem(2,new ItemStack(Items.BOW));p.getInventory().setItem(4,CookingContent.meal(new MealData(1,MealData.RECOVERY,2,10)));p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();
                var target=net.minecraft.world.entity.EntityTypes.VILLAGER.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);target.setNoAi(true);target.setPos(.5,120,2.5);level.addFreshEntity(target);
            });
            c.waitFor(client -> client.player.getMainHandItem().is(Items.IRON_SWORD)&&client.player.getOffhandItem().is(Items.STONE));FusionClientSmokeTest.key(c,false);c.waitFor(client -> client.player.getMainHandItem().has(FusionContent.VIEW));FusionClientSmokeTest.aim(c,new Vec3(.5,121,2.5));c.waitFor(client -> client.hitResult instanceof EntityHitResult);c.waitTicks(20);c.getInput().holdKeyFor(o -> o.keyAttack,1);c.waitFor(client -> client.player.getMainHandItem().get(FusionContent.VIEW).uses()==31);
            FusionClientSmokeTest.select(c,4);c.waitFor(client -> client.player.getMainHandItem().is(CookingContent.MEAL));FusionClientSmokeTest.aim(c,new Vec3(.5,128,.5));System.out.println("P9.1 begin actual meal use with empty sky target");c.getInput().holdKey(o -> o.keyUse);c.waitFor(client -> client.player.getAttached(CookingEffects.VIEW)!=null&&client.player.getAttached(CookingEffects.VIEW).recovery()==2);System.out.println("P9.1 native recovery meal effect received");c.getInput().releaseKey(o -> o.keyUse);c.waitFor(client -> client.player.getMainHandItem().is(Items.BOWL));
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();retained[0]=p.getInventory().getItem(0).get(FusionContent.DATA);check(retained[0].remaining()==31&&PlayerStamina.get(p).highestLevel()==20&&PlayerStamina.get(p).capacity()==140,"Native eaten meal leaves historic peak and fused weapon wear unchanged");
                var bow=p.getInventory().getItem(2);net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(bow,e -> e.set(p.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY),1));var arrow=new ItemStack(Items.ARROW);check(FusionItems.attach(arrow,new ItemStack(Items.SNOWBALL),p.registryAccess()),"Generate one paid ice arrow fixture");p.getInventory().setItem(9,arrow);p.teleportTo(server.overworld(),.5,150,.5,Set.of(),0,0,false);p.setNoGravity(true);p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);p.getInventory().setSelectedSlot(2);p.inventoryMenu.broadcastChanges();
                var target=net.minecraft.world.entity.EntityTypes.HUSK.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);target.setNoAi(true);target.setNoGravity(true);target.setPos(.5,150,20.5);server.overworld().addFreshEntity(target);
            });
            FusionClientSmokeTest.select(c,2);c.waitFor(client -> client.player.getY()>149&&client.player.getMainHandItem().is(Items.BOW)&&client.player.getInventory().getItem(9).has(FusionContent.VIEW));c.runOnClient(client -> {client.player.setNoGravity(true);client.player.setDeltaMovement(Vec3.ZERO);});FusionClientSmokeTest.aim(c,new Vec3(.5,151.8,20.5));c.getInput().holdKey(o -> o.keyUse);c.waitFor(client -> FocusTime.active(client.player));
            check(c.computeOnClient(client -> BackEquipment.view(client.player).melee().get(FusionContent.VIEW).uses()==31&&!BackEquipment.view(client.player).melee().has(FusionContent.DATA)),"Fusion back remains bounded while drawing in air");
            double[] before={0};int[] mealBefore={0};
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();check(FocusTime.active(server)&&server.tickRateManager().tickrate()==5,"Actual air draw acquires real 5 TPS lease");before[0]=PlayerStamina.get(p).stamina();mealBefore[0]=CookingEffects.get(p).recovery().millis();check(!FusionActions.receive(p,new FusionIntent(false)),"Held native bow rejects fusion mutation");});
            long began=System.nanoTime();c.waitFor(client -> System.nanoTime()-began>=1_200_000_000L);
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();double paid=before[0]-PlayerStamina.get(p).stamina();int elapsed=mealBefore[0]-CookingEffects.get(p).recovery().millis();check(paid>=8&&paid<18&&elapsed>=500&&elapsed<2500,"Recovery meal does not recover during focus or multiply its own time: paid="+paid+" mealMs="+elapsed);check(PlayerStamina.get(p).highestLevel()==20&&PlayerStamina.get(p).capacity()==140&&p.getInventory().getItem(0).get(FusionContent.DATA).equals(retained[0]),"Focus fees keep peak and dormant fusion intact");});c.takeScreenshot("p91-native-fusion-back-food-air-focus");
            c.getInput().releaseKey(o -> o.keyUse);c.waitFor(client -> !FocusTime.active(client.player));c.waitFor(client -> client.level.getEntitiesOfClass(AbstractArrow.class,new AABB(-3,149,-3,3,155,25)).stream().anyMatch(a -> a.getAttached(FusionCombat.ARROW_VIEW)!=null));
            world.getServer().waitFor(server -> server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class,new AABB(-1,149,18,2,153,23)).stream().anyMatch(m -> m.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)));
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();check(server.tickRateManager().tickrate()==20&&!FocusTime.active(server)&&p.getStats().getValue(Stats.ITEM_USED.get(Items.BOW))==1&&p.getInventory().getItem(9).isEmpty(),"Native release restores rate and shoots/consumes one actual Infinity fusion arrow");check(p.getInventory().getItem(0).get(FusionContent.DATA).equals(retained[0]),"Air arrow never changes stored melee fusion");savedMeal[0]=CookingEffects.get(p).recovery().millis();});c.takeScreenshot("p91-native-ice-arrow-once");save=world.getWorldSave();
        }finally{c.getInput().releaseKey(o -> o.keyUse);}
        try(TestSingleplayerContext world=save.open()){
            world.getConnection().waitForChunksRender();world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();check(!FocusTime.active(server)&&server.tickRateManager().tickrate()==20&&p.getInventory().getItem(0).get(FusionContent.DATA).equals(retained[0])&&p.getInventory().getItem(9).isEmpty()&&PlayerStamina.get(p).highestLevel()==20&&CookingEffects.get(p).recovery().millis()<=savedMeal[0],"Actual disk reload keeps worn fusion, spent ammo and historical peak; does not restore active focus or refill food");});
        }
        System.out.println("WILDCRAFT P9.1 native meal/fused melee/back/air 5 TPS focus/real fees/one Infinity ice impact/disk reload integration passed");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
