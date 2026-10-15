package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.block.Blocks;
/** Actual RIGHT AIR use, original physics/Focus and one native arrow settlement. */
public final class RightAirBowRigClientTest implements FabricClientGameTest {
 private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 private static void prepareRight(ClientGameTestContext c,TestSingleplayerContext w){
  AirBowRigClientTest.prepare(c,w,PlayerModelType.WIDE,InteractionHand.MAIN_HAND);w.getServer().runOnServer(v->v.getPlayerList().getPlayers().getFirst().setMainArm(HumanoidArm.RIGHT));c.waitFor(v->v.player.getMainArm()==HumanoidArm.RIGHT);
 }
 private static boolean hold(Minecraft v){var f=AirBowRigClientTest.frame(v);return f!=null&&f.clip().equals("bow_air_hold_empty_R")&&f.weight()>.999;}
 private static void ready(ClientGameTestContext c){AirBowRigClientTest.jumpUse(c);c.waitFor(RightAirBowRigClientTest::hold);c.runOnClient(v->AirBowAudit.expectActive=false);}
 private static void mode(ClientGameTestContext c,TestSingleplayerContext w,int mode){c.runOnClient(v->CombatOptions.get().bowSkill=mode);c.waitTicks(3);w.getServer().runOnServer(v->check(dev.wildcraft.combat.SkillPreferences.bow(v.getPlayerList().getPlayers().getFirst())==mode,"Actual transmitted bow mode"));}
 private static void noAir(ClientGameTestContext c,String reason){check(c.computeOnClient(v->{var f=AirBowRigClientTest.frame(v);return f==null||!f.clip().startsWith("bow_air_");}),reason);}
 private static void clockChecks(ClientGameTestContext c,TestSingleplayerContext w){
  prepareRight(c,w);c.runOnClient(v->v.options.framerateLimit().set(10));ready(c);check(c.computeOnClient(v->AirBowAudit.draws>0&&AirBowAudit.holds>0),"Actual RIGHT10FPS native effective draw/hold");c.takeScreenshot("right-air-native10fps");c.runOnClient(v->v.options.framerateLimit().set(120));c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->v.player.onGround());
  prepareRight(c,w);ready(c);c.setScreen(()->new net.minecraft.client.gui.screens.PauseScreen(true));c.waitFor(Minecraft::isPaused);boolean using=c.computeOnClient(v->v.player.isUsingItem());
  if(using){float time=c.computeOnClient(v->AirBowRigClientTest.frame(v).time());c.waitTicks(12);check(c.computeOnClient(v->{var f=AirBowRigClientTest.frame(v);return f!=null&&Math.abs(f.time()-time)<.000001;}),"Pause freezes surviving same RIGHT source clock");}
  else {c.waitTicks(12);noAir(c,"Native pause ended actual use; no retained fake RIGHT AIR hold");}
  c.takeScreenshot("right-air-native-pause");c.setScreen(()->null);c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->v.player.onGround());
 }
 private static void boundaries(ClientGameTestContext c,TestSingleplayerContext w){
  prepareRight(c,w);mode(c,w,1);c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround());c.getInput().releaseKey(v->v.keyJump);c.runOnClient(v->AirBowAudit.expectActive=true);c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.isUsingItem());c.waitTicks(2);check(c.computeOnClient(v->!FocusTime.active(v.player)),"RIGHT source cannot start manual Focus");c.getInput().holdKeyFor(dev.wildcraft.client.input.WildcraftBindings.CONFIRM_SKILL,1);c.waitFor(v->FocusTime.active(v.player));c.waitFor(RightAirBowRigClientTest::hold);c.runOnClient(v->AirBowAudit.expectActive=false);c.takeScreenshot("right-air-manual-confirm");c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->v.player.onGround());
  prepareRight(c,w);mode(c,w,2);c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.isUsingItem()&&v.player.getTicksUsingItem()>=16);c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround());c.getInput().releaseKey(v->v.keyJump);c.runOnClient(v->AirBowAudit.expectActive=true);c.waitFor(RightAirBowRigClientTest::hold);c.runOnClient(v->AirBowAudit.expectActive=false);check(c.computeOnClient(v->!FocusTime.active(v.player)),"Disabled RIGHT skill stays disabled");w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();check(v.tickRateManager().tickrate()==20&&PlayerStamina.get(q).stamina()==100,"Disabled ordinary RIGHT AIR retains20TPS/no skill fee");});c.takeScreenshot("right-air-disabled-native20");c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->v.player.onGround());
  for(String event:new String[]{"slot","inventory","armor","hurt","land"}){
   prepareRight(c,w);w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();q.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH));q.inventoryMenu.broadcastChanges();});ready(c);
   if(event.equals("slot")){c.getInput().pressKey(v->v.keyHotbarSlots[1]);c.waitFor(v->v.player.getMainHandItem().is(net.minecraft.world.item.Items.TORCH));}
   else if(event.equals("inventory")){c.setScreen(()->new net.minecraft.client.gui.screens.inventory.InventoryScreen(Minecraft.getInstance().player));c.waitTicks(2);}
   else if(event.equals("armor")){w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();q.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));q.inventoryMenu.broadcastChanges();});c.waitFor(v->!v.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty());}
   else if(event.equals("hurt")){w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();q.hurtServer(v.overworld(),q.damageSources().generic(),1);check(q.getHealth()<20,"No RIGHT animation immunity");});c.waitFor(v->v.player.getHealth()<20&&v.player.hurtTime>0);}
   else {w.getServer().runOnServer(v->PlayerStamina.consume(v.getPlayerList().getPlayers().getFirst(),10000));c.waitFor(v->!FocusTime.active(v.player));c.waitFor(v->v.player.onGround());}
   noAir(c,"Actual RIGHT "+event+" relinquishes airborne source");c.takeScreenshot("right-air-native-exit-"+event);c.setScreen(()->null);c.getInput().releaseKey(v->v.keyUse);c.waitTicks(3);
   if(event.equals("slot"))w.getServer().runOnServer(v->{var q=v.getPlayerList().getPlayers().getFirst();check(AirBowAudit.shots.get()==0&&q.getInventory().getItem(10).getCount()==32&&q.getInventory().getItem(0).getDamageValue()==0,"RIGHT slot cancellation adds no arrow-ammo-wear");});c.waitFor(v->v.player.onGround());
  }
 }
 @Override public void runTest(ClientGameTestContext c){int old=CombatOptions.get().bowSkill;
  try(TestSingleplayerContext w=c.worldBuilder().create()){
   w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runOnServer(s->{for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.getPlayerList().getPlayers().getFirst().teleportTo(.5,120,.5);});c.getInput().resizeWindow(1280,960);c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
   if(!Boolean.getBoolean("wildcraft.test.rightAirBoundaryOnly")&&!Boolean.getBoolean("wildcraft.test.rightAirClockOnly"))for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var hand:InteractionHand.values()){
    AirBowRigClientTest.prepare(c,w,type,hand);var main=hand==InteractionHand.MAIN_HAND?HumanoidArm.RIGHT:HumanoidArm.LEFT;w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setMainArm(main));c.waitFor(v->v.player.getMainArm()==main);AirBowRigClientTest.jumpUse(c);
    c.waitFor(v->{var f=AirBowRigClientTest.frame(v);return f!=null&&f.clip().equals("bow_air_draw_empty_R")&&f.weight()>.999;});c.takeScreenshot("right-air-bow-draw-"+type+"-"+hand);
    c.waitFor(v->{var f=AirBowRigClientTest.frame(v);return f!=null&&f.clip().equals("bow_air_hold_empty_R")&&RigAudit.hands>0;});c.takeScreenshot("right-air-bow-hold-"+type+"-"+hand);
    c.runOnClient(v->{check(!v.player.onGround()&&v.player.getUsedItemHand()==hand&&AirBowAudit.frames>0&&AirBowAudit.draws>0&&AirBowAudit.holds>0&&AirBowAudit.stages.equals(Set.of(0,1,2)),"Actual RIGHT source and native three-stage air ownership");check(RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString()),"Original skin GPU");check(AirBowRigClientTest.frame(v).global()[0].getTranslation(new org.joml.Vector3f()).length()<.00002,"No preview lift on real player");AirBowAudit.expectActive=false;});
    w.getServer().runOnServer(s->{var q=s.getPlayerList().getPlayers().getFirst();check(FocusTime.active(q)&&s.tickRateManager().tickrate()==5&&PlayerStamina.get(q).stamina()<100&&PlayerStamina.get(q).stamina()>70,"Native lease and original actual-time cost");check(!q.isNoGravity()&&!q.getAbilities().flying&&q.getHealth()==20&&AirBowAudit.shots.get()==0&&q.getInventory().getItem(10).getCount()==32&&q.getItemInHand(hand).getDamageValue()==0,"Original physics and no animation shot");});
    c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->!v.player.isUsingItem()&&v.player.getItemInHand(hand).getDamageValue()==1);check(c.computeOnClient(v->{var f=AirBowRigClientTest.frame(v);return f==null||!f.clip().startsWith("bow_air_");}),"Stop-use returns actual hands immediately");w.getServer().runOnServer(s->{var q=s.getPlayerList().getPlayers().getFirst();check(AirBowAudit.shots.get()==1&&q.getInventory().getItem(10).getCount()==31&&q.getItemInHand(hand).getDamageValue()==1&&!FocusTime.active(q)&&s.tickRateManager().tickrate()==20&&q.getMainArm()==main,"One native arrow/ammo/wear and full original lease exit");});c.takeScreenshot("right-air-bow-release-native-owner-"+type+"-"+hand);
   }
   clockChecks(c,w);
   if(!Boolean.getBoolean("wildcraft.test.rightAirClockOnly"))boundaries(c,w);
   System.out.println(Boolean.getBoolean("wildcraft.test.rightAirClockOnly")?"C6AM actual RIGHT AIR10FPS/pause clock-only PASS":Boolean.getBoolean("wildcraft.test.rightAirBoundaryOnly")?"C6AM actual RIGHT AIR10FPS/pause/manual-disabled-native exits PASS":"C6AM actual RIGHT AIR4/native20/three sprites/source-root-head/skin-item GPU/native Focus/one arrow-ammo-wear/10FPS-pause-manual-disabled-native exits PASS");
  }finally{c.getInput().releaseKey(v->v.keyUse);c.getInput().releaseKey(v->v.keyJump);c.runOnClient(v->{v.options.framerateLimit().set(120);AirBowAudit.enabled=false;GroundBowAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;FocusLegAudit.enabled=false;CombatOptions.get().bowSkill=old;v.options.setCameraType(CameraType.FIRST_PERSON);});}
 }
}
