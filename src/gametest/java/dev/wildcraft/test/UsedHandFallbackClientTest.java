package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.combat.SkillPreferences;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.player.PlayerStamina;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
/** Unregistered next-batch draft. Observe real native ownership/collector without alternate rendering. */
public final class UsedHandFallbackClientTest implements FabricClientGameTest {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static void fallback(ClientGameTestContext c){check(c.computeOnClient(v->{var f=AirBowRigClientTest.frame(v);return f==null||!f.clip().startsWith("bow_air_")&&!f.clip().startsWith("bow_ground_");}),"Occupied other hand must not draw empty-hand source");}
 private static void lateChanges(ClientGameTestContext c,TestSingleplayerContext w){
  c.runOnClient(v->{NativeFallbackAudit.lateContinued=NativeFallbackAudit.lateCancelled=0;});
  for(var skin:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var main:HumanoidArm.values())for(var hand:InteractionHand.values()){
   c.runOnClient(v->NativeFallbackAudit.enabled=false);AirBowRigClientTest.prepare(c,w,skin,hand);w.getServer().runOnServer(v->v.getPlayerList().getPlayers().getFirst().setMainArm(main));c.waitFor(v->v.player.getMainArm()==main);AirBowRigClientTest.jumpUse(c);c.waitFor(v->{var f=AirBowRigClientTest.frame(v);return f!=null&&f.clip().startsWith("bow_air_hold_")&&f.weight()>.999&&RigAudit.hands>0;});
   c.runOnClient(v->{AirBowAudit.expectActive=false;NativeFallbackAudit.begin();});var other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
   w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();q.setItemInHand(other,new ItemStack(Items.TORCH));q.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemInHand(other).is(Items.TORCH));fallback(c);c.waitFor(v->NativeFallbackAudit.bodies>0&&NativeFallbackAudit.bowQuads>0&&NativeFallbackAudit.otherQuads>0);c.takeScreenshot("late-other-native-fallback-"+skin+"-"+main+"-"+hand);
   w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();check(AirBowAudit.shots.get()==0&&q.getItemInHand(hand).getDamageValue()==0&&q.getInventory().getItem(10).getCount()==32,"Synced other-hand update cannot invent arrow/ammo/wear");q.setItemInHand(other,ItemStack.EMPTY);q.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemInHand(other).isEmpty());boolean continued=c.computeOnClient(v->v.player.isUsingItem()&&v.player.getUseItem().is(Items.BOW));
   c.runOnClient(v->{if(continued)NativeFallbackAudit.lateContinued++;else NativeFallbackAudit.lateCancelled++;});
   if(continued){c.runOnClient(v->{RigAudit.hands=RigAudit.bodies=0;});c.waitFor(v->{var f=AirBowRigClientTest.frame(v);return f!=null&&(f.clip().startsWith("bow_air_")||f.clip().startsWith("bow_ground_"))&&f.weight()>.999&&RigAudit.hands>0&&RigAudit.bodies>0;});}
   else fallback(c);
   c.takeScreenshot("late-other-source-or-native-cancel-"+skin+"-"+main+"-"+hand);c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->!v.player.isUsingItem());c.waitTicks(3);w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();check(AirBowAudit.shots.get()==(continued?1:0)&&q.getItemInHand(hand).getDamageValue()==(continued?1:0)&&q.getInventory().getItem(10).getCount()==(continued?31:32),"Only actual surviving native use may settle one original arrow");check(!FocusTime.active(q)&&v.tickRateManager().tickrate()==20&&q.getMainArm()==main,"Same actual arm and complete original lease restoration");});c.waitFor(v->v.player.onGround());
  }
  System.out.println("C6AN late branches continued="+NativeFallbackAudit.lateContinued+" nativeCancelled="+NativeFallbackAudit.lateCancelled);
  System.out.println("C6AN late8 actual synced other-hand update/source relinquish/native model-item GPU/true use continuation or native cancel/original one arrow-lease PASS");
 }
 private static void equipmentCases(ClientGameTestContext c,TestSingleplayerContext w,boolean wings){
  var slots=wings?new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.CHEST}:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};var items=wings?new Item[]{Items.ELYTRA}:new Item[]{Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS};var prefix=wings?"elytra":"armor";
  for(var skin:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var main:HumanoidArm.values())for(var hand:InteractionHand.values())for(int i=0;i<slots.length;i++){
   var slot=slots[i];var armor=items[i];c.runOnClient(v->NativeFallbackAudit.enabled=false);AirBowRigClientTest.prepare(c,w,skin,hand);w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();q.setMainArm(main);q.setItemSlot(slot,new ItemStack(armor));q.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getMainArm()==main&&v.player.getItemBySlot(slot).is(armor));c.runOnClient(v->{CombatOptions.get().bowSkill=2;NativeFallbackAudit.begin();NativeFallbackAudit.expectedArmorTexture=net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft","textures/entity/equipment/"+(wings?"wings/elytra":(slot==net.minecraft.world.entity.EquipmentSlot.LEGS?"humanoid_leggings/iron":"humanoid/iron"))+".png");});c.waitTicks(3);c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.isUsingItem()&&v.player.getTicksUsingItem()>=18);fallback(c);c.waitFor(v->NativeFallbackAudit.bodies>0&&NativeFallbackAudit.bowQuads>0&&NativeFallbackAudit.armorModels>0);c.takeScreenshot(prefix+"-ground-native-"+skin+"-"+main+"-"+hand+"-"+slot);
   c.runOnClient(v->{NativeFallbackAudit.bodies=NativeFallbackAudit.bowQuads=NativeFallbackAudit.armorModels=0;});c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround());c.getInput().releaseKey(v->v.keyJump);c.waitTicks(2);fallback(c);c.waitFor(v->NativeFallbackAudit.bodies>0&&NativeFallbackAudit.bowQuads>0&&NativeFallbackAudit.armorModels>0);check(c.computeOnClient(v->NativeFallbackAudit.skin.equals(RigAudit.fixture.body().texturePath().toString())&&!FocusTime.active(v.player)),"Actual native armor/skin/item and disabled skill unchanged");c.takeScreenshot(prefix+"-air-native-"+skin+"-"+main+"-"+hand+"-"+slot);c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->!v.player.isUsingItem());c.waitTicks(3);w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();check(AirBowAudit.shots.get()==1&&q.getItemInHand(hand).getDamageValue()==1&&q.getInventory().getItem(10).getCount()==31&&q.getItemBySlot(slot).is(armor)&&q.getItemBySlot(slot).getDamageValue()==0,"Actual armor fallback native one arrow/ammo/bowwear and unchanged armor");check(PlayerStamina.get(q).stamina()==100&&v.tickRateManager().tickrate()==20&&!q.isNoGravity()&&!q.isFallFlying(),"Original costs/time/physics");});c.waitFor(v->v.player.onGround());
  }
  System.out.println("C6AN equipped "+prefix+" actual native skin-equipment-bow collector/original one-arrow-ammo-wear-cost-physics PASS; no source armor/wings retargeting or controlled flight proof");
 }
 @Override public void runTest(ClientGameTestContext c){int previous=CombatOptions.get().bowSkill;
  try(TestSingleplayerContext w=c.worldBuilder().create()){
   w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runOnServer(s->{for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.getPlayerList().getPlayers().getFirst().teleportTo(.5,120,.5);});c.getInput().resizeWindow(1280,960);c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
   if(Boolean.getBoolean("wildcraft.test.usedHandElytraOnly")){equipmentCases(c,w,true);return;}
   if(Boolean.getBoolean("wildcraft.test.usedHandArmorOnly")){equipmentCases(c,w,false);return;}
   if(!Boolean.getBoolean("wildcraft.test.usedHandLateOnly"))for(var skin:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var main:HumanoidArm.values())for(var bowHand:InteractionHand.values())for(var otherItem:new Item[]{Items.TORCH,Items.SHIELD}){
    AirBowRigClientTest.prepare(c,w,skin,bowHand);var otherHand=bowHand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
    w.getServer().runOnServer(s->{var q=s.getPlayerList().getPlayers().getFirst();q.setMainArm(main);q.setItemInHand(otherHand,new ItemStack(otherItem));q.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getMainArm()==main&&v.player.getItemInHand(otherHand).is(otherItem));c.runOnClient(v->{AirBowAudit.expectActive=false;CombatOptions.get().bowSkill=2;});c.waitTicks(3);w.getServer().runOnServer(s->check(SkillPreferences.bow(s.getPlayerList().getPlayers().getFirst())==2,"True transmitted disabled mode"));
    if(otherItem==Items.SHIELD&&bowHand==InteractionHand.MAIN_HAND){
     c.getInput().pressKey(v->v.keyHotbarSlots[8]);c.waitFor(v->v.player.getMainHandItem().isEmpty());c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.isUsingItem()&&v.player.getUsedItemHand()==InteractionHand.OFF_HAND&&v.player.getUseItem().is(Items.SHIELD));c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->!v.player.isUsingItem());c.waitFor(v->v.player.getAttached(dev.wildcraft.equipment.BackEquipment.VISUAL).shield().reference()!=0);c.getInput().pressKey(v->v.keyHotbarSlots[0]);c.waitFor(v->v.player.getMainHandItem().is(Items.BOW));
    }
    c.runOnClient(v->NativeFallbackAudit.begin());c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.isUsingItem());boolean shieldPriority=bowHand==InteractionHand.OFF_HAND&&otherItem==Items.SHIELD;
    check(c.computeOnClient(v->v.player.getUsedItemHand()==(shieldPriority?InteractionHand.MAIN_HAND:bowHand)&&v.player.getUseItem().is(shieldPriority?Items.SHIELD:Items.BOW)),"Actual native accepted use; do not force off-bow through main-shield priority");c.waitFor(v->v.player.getTicksUsingItem()>=18);fallback(c);
    c.waitFor(v->NativeFallbackAudit.bodies>0&&NativeFallbackAudit.bowQuads>0&&(otherItem==Items.TORCH?NativeFallbackAudit.otherQuads>0:NativeFallbackAudit.shieldModels>0));check(c.computeOnClient(v->NativeFallbackAudit.skin.equals(RigAudit.fixture.body().texturePath().toString())),"Actual ground native skin and native item/back submissions");c.takeScreenshot("occupied-ground-native-"+skin+"-"+main+"-"+bowHand+"-"+otherItem);c.runOnClient(v->{NativeFallbackAudit.bodies=NativeFallbackAudit.bowQuads=NativeFallbackAudit.otherQuads=NativeFallbackAudit.shieldModels=0;});
    c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround());c.getInput().releaseKey(v->v.keyJump);c.waitTicks(2);fallback(c);
    c.waitFor(v->NativeFallbackAudit.bodies>0&&NativeFallbackAudit.bowQuads>0&&(otherItem==Items.TORCH?NativeFallbackAudit.otherQuads>0:NativeFallbackAudit.shieldModels>0));check(c.computeOnClient(v->NativeFallbackAudit.skin.equals(RigAudit.fixture.body().texturePath().toString())),"True native fallback skin and native bow/torch or actually-used hand-back shield collector submissions");
    check(c.computeOnClient(v->!FocusTime.active(v.player)&&!v.player.isNoGravity()&&!v.player.getAbilities().flying),"Fallback does not activate disabled skill or change physics");c.takeScreenshot("occupied-native-"+skin+"-"+main+"-"+bowHand+"-"+otherItem);
    c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->!v.player.isUsingItem());c.waitTicks(3);
    w.getServer().runOnServer(s->{var q=s.getPlayerList().getPlayers().getFirst();check(q.getItemInHand(bowHand).is(Items.BOW)&&q.getItemInHand(otherHand).is(otherItem)&&q.getMainArm()==main,"Original hands/inventory survive fallback");check(q.getItemInHand(bowHand).getDamageValue()==(shieldPriority?0:1)&&q.getInventory().getItem(10).getCount()==(shieldPriority?32:31)&&AirBowAudit.shots.get()==(shieldPriority?0:1),"Native accepted bow shoots once, native shield-priority shoots none");check(q.getItemInHand(otherHand).getDamageValue()==0&&PlayerStamina.get(q).stamina()==100&&s.tickRateManager().tickrate()==20,"No renderer cost/wear/lease");});c.waitFor(v->v.player.onGround());
   }
   lateChanges(c,w);
   if(!Boolean.getBoolean("wildcraft.test.usedHandLateOnly")){equipmentCases(c,w,false);equipmentCases(c,w,true);}
   if(!Boolean.getBoolean("wildcraft.test.usedHandLateOnly"))System.out.println("C6AN16 actual use cases ground/air32 native model-skin/item/back collector, registered shield and main-shield priority,12 native arrows/4 block-only, original inventory-cost-physics PASS");
  }finally{c.getInput().releaseKey(v->v.keyUse);c.getInput().releaseKey(v->v.keyJump);c.runOnClient(v->{CombatOptions.get().bowSkill=previous;NativeFallbackAudit.enabled=false;NativeFallbackAudit.kind=0;NativeFallbackAudit.expectedBackShield=null;AirBowAudit.enabled=false;AirBowAudit.expectActive=false;RigAudit.enabled=false;RigAudit.fixture=null;v.options.setCameraType(CameraType.FIRST_PERSON);});}
 }
}
