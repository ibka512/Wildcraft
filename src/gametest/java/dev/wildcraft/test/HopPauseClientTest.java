package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
/** Real X/direction input and actual GUI intents; never grants pause immunity or extends the six-tick hop. */
public final class HopPauseClientTest implements FabricClientGameTest {
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin fixture");}
 private static boolean source(HopPauseAudit.Sample a){return a!=null&&a.frame()!=null&&a.frame().clip().startsWith("dodge_")&&!a.frame().clip().equals("dodge_native_base");}
 private static void release(ClientGameTestContext c){c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyDown);c.getInput().releaseKey(WildcraftBindings.DEFEND);c.getInput().releaseKey(WildcraftBindings.DODGE);}
 @Override public void runTest(ClientGameTestContext c){var opts=CombatOptions.get();boolean jump=opts.jumpDodge,hold=opts.hold;int follow=opts.follow;boolean matrix=Boolean.getBoolean("wildcraft.test.hopPauseMatrix");int cases=0,cancelled=0,retained=0;var id=new java.util.concurrent.atomic.AtomicInteger();
  try(TestSingleplayerContext w=c.worldBuilder().create()){
   w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
   w.getServer().runOnServer(s->{for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.inventoryMenu.broadcastChanges();var mob=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);mob.setNoAi(true);mob.setNoGravity(true);mob.setPos(.5,120,8);s.overworld().addFreshEntity(mob);id.set(mob.getId());});
   c.waitFor(v->v.level.getEntity(id.get())!=null&&v.player.getY()>119&&v.player.getMainHandItem().is(Items.IRON_SWORD)&&ActionInput.tickFrame().gameplay());c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.jumpDodge=false;opts.hold=false;opts.follow=0;v.options.guiScale().set(2);v.player.setYRot(0);v.player.setXRot(0);v.options.setCameraType(CameraType.THIRD_PERSON_BACK);});
   c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(v->Combat.locked(v.player));
   for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values())for(int mode=0;mode<3;mode++)for(String direction:new String[]{"left","right","back"})for(String menu:new String[]{"pause","inventory"}){
    if(!matrix&&cases>0)continue;int equipment=mode;release(c);c.setScreen(()->null);w.getServer().runCommand("tick rate 20");c.runOnClient(v->v.options.framerateLimit().set(120));c.waitTicks(16);
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();MeleeActions.reset(p);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);p.setMainArm(arm);p.setHealth(20);p.setInvulnerableTime(0);var sword=new ItemStack(Items.IRON_SWORD);check(FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Actual Fuse fixture");p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setItemInHand(InteractionHand.OFF_HAND,equipment==0?ItemStack.EMPTY:new ItemStack(Items.SHIELD));PlayerStamina.fill(p);CounterAttackAudit.begin(p);MeleeLifecycleAudit.begin(p);p.inventoryMenu.broadcastChanges();});
    c.getInput().pressKey(v->v.keyHotbarSlots[0]);c.waitFor(v->v.player.onGround()&&v.player.getMainArm()==arm&&v.player.getOffhandItem().isEmpty()==(equipment==0));c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);RigAudit.fixture=skin(type);RigAudit.begin();HopPauseAudit.begin();});c.waitTicks(4);
    if(!c.computeOnClient(v->Combat.locked(v.player))){c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(v->Combat.locked(v.player));}
    if(mode==2){c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->Combat.view(v.player).defending()&&v.player.isUsingItem());}
    w.getServer().runCommand("tick rate 5");c.runOnClient(v->v.options.framerateLimit().set(10));if(direction.equals("left"))c.getInput().holdKey(v->v.keyLeft);else if(direction.equals("right"))c.getInput().holdKey(v->v.keyRight);else c.getInput().holdKey(v->v.keyDown);
    c.getInput().holdKeyFor(WildcraftBindings.DODGE,1);c.waitFor(v->MeleeActions.hopping(v.player)&&source(HopPauseAudit.last)&&RigAudit.bodies>0&&RigAudit.hands>0);c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyDown);
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(PlayerStamina.get(p).stamina()==92&&MeleeLifecycleAudit.dodges==1&&!p.isUsingItem(),"Actual single native hop pays eight and releases shield");});
    String label=type+"-"+arm+"-mode-"+mode+"-"+direction+"-"+menu;int frames=HopPauseAudit.frames;if(menu.equals("pause")){c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);}else c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));c.waitTicks(2);
    var a=c.computeOnClient(v->HopPauseAudit.last);check(HopPauseAudit.frames>frames,"GUI state is actually submitted again");boolean alive=a.view().phase()==MeleeView.HOP;
    System.out.println("C6AS_HOP_GUI "+label+" phase="+a.view().phase()+" source="+source(a)+" cancels="+MeleeLifecycleAudit.cancels+" cancelFromHop="+MeleeLifecycleAudit.cancelFromHop+" partial="+a.partial());c.takeScreenshot("hop-pause-"+label);
    if(menu.equals("pause")&&alive){
     check(source(a),"A genuinely retained native hop owns its qualified source");float expected=Math.clamp((a.view().age()+a.partial())/Math.max(1,a.view().duration()),0,1);System.out.println("C6AS_HOP_RETAINED "+label+" actual="+a.frame().time()+" expected="+expected+" partial="+a.partial());check(Math.abs(a.frame().time()-expected)<1e-5F,"Live hop source preserves native frozen fraction");
     var pos=c.computeOnClient(v->v.player.position());var vel=c.computeOnClient(v->v.player.getDeltaMovement());long until=System.nanoTime()+350_000_000L;c.waitFor(v->System.nanoTime()>until);var b=c.computeOnClient(v->HopPauseAudit.last);check(source(b)&&b.tick()==a.tick()&&b.view().equals(a.view()),"Retained native hop truly freezes");for(int i=0;i<a.frame().global().length;i++)check(a.frame().global()[i].equals(b.frame().global()[i],2e-5F),"Retained paused palette is frozen");check(c.computeOnClient(v->v.player.position().distanceToSqr(pos)<1e-10&&v.player.getDeltaMovement().distanceToSqr(vel)<1e-10),"Only native pause freezes movement");retained++;
    }else{check(!source(a),"Native GUI cancellation or inventory cannot render a late hop source");cancelled++;}
    c.setScreen(()->null);w.getServer().runCommand("tick rate 20");c.runOnClient(v->v.options.framerateLimit().set(120));c.waitFor(v->!MeleeActions.hopping(v.player)&&v.player.onGround());c.waitTicks(3);check(c.computeOnClient(v->!source(HopPauseAudit.last)&&v.player.getHealth()==20),"GUI resume cannot replay old source or invent damage");
    boolean mustCancel=!alive||menu.equals("inventory");w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(MeleeLifecycleAudit.dodges==1&&(!mustCancel||MeleeLifecycleAudit.cancels>=1),"Actual server receives one hop and original GUI cancel where required");check(CounterAttackAudit.calls==0&&p.getMainHandItem().getDamageValue()==0&&p.getMainHandItem().get(FusionContent.DATA).remaining()==32&&p.getOffhandItem().getDamageValue()==0,"GUI creates no late hit/wear/Fuse transaction");double dx=p.getX()-.5,dz=p.getZ()-.5;check(Math.hypot(dx,dz)<3.5&&Math.abs(p.getY()-120)<.01&&(direction.equals("left")?dx>=-.01:direction.equals("right")?dx<=.01:dz<=.01),"Native cancelled movement remains bounded, grounded and correctly directed");});cases++;
   }
   System.out.println("C6AS_HOP_COMPLETE actualGuiCases="+cases+" nativeCancelled="+cancelled+" nativeRetained="+retained+" originalSixTickEightCost=true");
  }finally{release(c);MeleeLifecycleAudit.player=null;c.runOnClient(v->{HopPauseAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;opts.jumpDodge=jump;opts.hold=hold;opts.follow=follow;v.options.framerateLimit().set(120);v.options.setCameraType(CameraType.FIRST_PERSON);});}
 }
}
