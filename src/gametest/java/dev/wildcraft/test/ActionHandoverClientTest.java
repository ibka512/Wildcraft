package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.*;
import dev.wildcraft.client.render.ParagliderLayer;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.*;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
/** Real jump, item-use and glider handover; original inventory and cancelled attack remain authoritative. */
public final class ActionHandoverClientTest implements FabricClientGameTest {
 private static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
 private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Actual native skin");}
 private static void release(ClientGameTestContext c){c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);c.getInput().releaseKey(WildcraftBindings.DEFEND);}
 private static void prepare(ClientGameTestContext c,TestSingleplayerContext w,PlayerModelType type,HumanoidArm arm){
  release(c);c.waitTicks(12);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();Gliding.reset(p,false);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})p.setItemSlot(slot,ItemStack.EMPTY);p.setMainArm(arm);var sword=new ItemStack(Items.IRON_SWORD);var shield=new ItemStack(Items.SHIELD);check(FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),s.registryAccess())&&FusionItems.attach(shield,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Actual native Fuse fixtures");p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setItemInHand(InteractionHand.OFF_HAND,shield);p.getInventory().setItem(1,new ItemStack(Items.TORCH));GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));p.teleportTo(p.level(),.5,250,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);p.setHealth(20);PlayerStamina.fill(p);CounterAttackAudit.begin(p);p.inventoryMenu.broadcastChanges();});c.getInput().pressKey(v->v.keyHotbarSlots[0]);c.waitFor(v->v.player.onGround()&&v.player.getY()==250&&v.player.getMainArm()==arm&&v.player.getMainHandItem().is(Items.IRON_SWORD)&&v.player.getOffhandItem().is(Items.SHIELD)&&GliderEquipment.equipped(v.player));c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);v.options.setCameraType(CameraType.THIRD_PERSON_BACK);RigAudit.fixture=skin(type);RigAudit.begin();GlideRigAudit.begin();});c.waitTicks(25);c.waitFor(v->ActionInput.tickFrame().gameplay());
 }
 private static void jump(ClientGameTestContext c){c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(v->!v.player.onGround()&&v.player.getDeltaMovement().y>0);}
 private static void open(ClientGameTestContext c){
  c.waitTicks(2);c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(v->Gliding.active(v.player));
  // A normal jump may legitimately land before the .30-second presentation entry finishes.
  // Check the actual active enter/hold canopy now; sustained hold remains covered at height by GlideRig/TraversalHandover.
  c.waitFor(v->{var f=state(v).getData(CombatRigLayer.FRAME);return Gliding.active(v.player)&&f!=null&&(f.clip().equals("glide_enter")||f.clip().equals("glide_hold"))&&GlideRigAudit.worldCanopies>0;});
  check(c.computeOnClient(v->{var s=state(v);return s.rightHandItemState.isEmpty()&&s.leftHandItemState.isEmpty()&&s.getData(GlideRig.CANOPY)!=null&&v.player.getMainHandItem().is(Items.IRON_SWORD)&&v.player.getOffhandItem().is(Items.SHIELD)&&MeleeActions.view(v.player).phase()==MeleeView.IDLE&&!v.player.isUsingItem();}),"Open glider owns both visual hands and cancels old attack/use while retaining true stacks");
 }
 private static void heldItemCancels(ClientGameTestContext c,String label){
  c.waitTicks(2);c.getInput().holdKeyFor(v->v.keyJump,1);c.waitTicks(3);check(c.computeOnClient(v->!Gliding.active(v.player)&&!Gliding.intent(v.player).open()&&MeleeActions.view(v.player).phase()==MeleeView.IDLE),"Held item input cancels canopy and old charge instead of hiding conflicting input");c.takeScreenshot(label+"-held-item-cancels-canopy");
  c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(WildcraftBindings.DEFEND);c.waitTicks(1);c.waitFor(v->v.player.onGround());jump(c);open(c);
 }
 private static void takeItem(ClientGameTestContext c,TestSingleplayerContext w,String name){
  c.getInput().pressKey(v->v.keyHotbarSlots[1]);c.waitFor(v->!Gliding.active(v.player)&&v.player.getMainHandItem().is(Items.TORCH));check(c.computeOnClient(v->{var s=state(v);var f=s.getData(CombatRigLayer.FRAME);return s.getData(GlideRig.CANOPY)==null&&!s.getDataOrDefault(ParagliderLayer.OPEN,false)&&(f==null||!f.clip().startsWith("jump_")&&!f.clip().startsWith("glide_"));}),"Actual item takeover closes canopy and never revives cancelled jump/attack");c.takeScreenshot(name+"-take-torch-native-owner");w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var sword=p.getInventory().getItem(0);var shield=p.getOffhandItem();check(CounterAttackAudit.calls==0&&sword.is(Items.IRON_SWORD)&&sword.getDamageValue()==0&&sword.get(FusionContent.DATA).remaining()==32&&shield.is(Items.SHIELD)&&shield.getDamageValue()==0&&shield.get(FusionContent.DATA).remaining()==32&&GliderEquipment.equipped(p),"Mode transition adds no attack-wear-Fuse or equipment transfer");});
 }
 @Override public void runTest(ClientGameTestContext c){var opts=CombatOptions.get();int follow=opts.follow;boolean jumpDodge=opts.jumpDodge;
  try(TestSingleplayerContext w=c.worldBuilder().create()){
   w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runOnServer(s->{for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,249,z),Blocks.STONE.defaultBlockState());s.getPlayerList().getPlayers().getFirst().teleportTo(.5,250,.5);});c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.follow=0;opts.jumpDodge=false;v.options.guiScale().set(2);});
   if(Boolean.getBoolean("wildcraft.test.handoverChargeProbe")){prepare(c,w,PlayerModelType.WIDE,HumanoidArm.LEFT);c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.CHARGE);c.waitTicks(14);jump(c);heldItemCancels(c,"handover-charge-probe");takeItem(c,w,"handover-charge-probe");return;}
   for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values()){
    prepare(c,w,type,arm);jump(c);c.waitFor(v->{var f=state(v).getData(CombatRigLayer.FRAME);return f!=null&&f.clip().startsWith("jump_");});open(c);c.takeScreenshot("handover-jump-glide-"+type+"-"+arm);takeItem(c,w,"handover-jump-glide-"+type+"-"+arm);if(Boolean.getBoolean("wildcraft.test.handoverFirstOnly"))return;
    prepare(c,w,type,arm);c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.CHARGE);c.waitTicks(14);jump(c);heldItemCancels(c,"handover-charge-"+type+"-"+arm);c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(2);check(c.computeOnClient(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE),"Release of cancelled charge cannot dispatch delayed strong hit");c.takeScreenshot("handover-charge-glide-"+type+"-"+arm);takeItem(c,w,"handover-charge-glide-"+type+"-"+arm);
    prepare(c,w,type,arm);c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->v.player.isUsingItem()&&Combat.view(v.player).defending());jump(c);heldItemCancels(c,"handover-shield-"+type+"-"+arm);c.getInput().releaseKey(WildcraftBindings.DEFEND);c.takeScreenshot("handover-shield-glide-"+type+"-"+arm);takeItem(c,w,"handover-shield-glide-"+type+"-"+arm);
   }
   System.out.println("C6AJ actual jump/charge/defend-to-glide-item input, real inventory and dual visual ownership, zero delayed hit-wear-Fuse, native skin and canopy GPU PASS");
  }finally{release(c);c.runOnClient(v->{RigAudit.enabled=false;RigAudit.fixture=null;GlideRigAudit.enabled=false;opts.follow=follow;opts.jumpDodge=jumpDodge;v.options.setCameraType(CameraType.FIRST_PERSON);});}
 }
}
