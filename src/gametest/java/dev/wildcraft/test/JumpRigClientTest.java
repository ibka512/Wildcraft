package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
/** Real jump key and original physics with native skin/item GPU; no fabricated jump or animation phase. */
public final class JumpRigClientTest implements FabricClientGameTest {
 private static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
 private static CombatRigLayer.Frame frame(Minecraft c){return state(c).getData(CombatRigLayer.FRAME);}
 private static boolean active(Minecraft c){var f=frame(c);return f!=null&&f.clip().startsWith("jump_");}
 private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin fixture");}
 private static void release(ClientGameTestContext c){c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);c.getInput().releaseKey(v->v.keyUp);c.getInput().releaseKey(WildcraftBindings.DEFEND);}
 private static void prepare(ClientGameTestContext c,TestSingleplayerContext w,PlayerModelType type,HumanoidArm arm,String equip){
  release(c);c.setScreen(()->null);c.waitTicks(12);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})p.setItemSlot(slot,ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);p.setMainArm(arm);var sword=new ItemStack(Items.IRON_SWORD);check(FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Native Fuse fixture");p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setItemInHand(InteractionHand.OFF_HAND,equip.equals("empty")?ItemStack.EMPTY:new ItemStack(Items.SHIELD));p.getInventory().setItem(8,new ItemStack(Items.TORCH));p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);p.setHealth(20);p.setInvulnerableTime(0);PlayerStamina.fill(p);CounterAttackAudit.begin(p);p.inventoryMenu.broadcastChanges();});c.getInput().pressKey(v->v.keyHotbarSlots[0]);c.waitFor(v->v.player.onGround()&&v.player.getMainHandItem().is(Items.IRON_SWORD)&&v.player.getMainArm()==arm&&v.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty());c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(12);v.options.setCameraType(CameraType.THIRD_PERSON_BACK);RigAudit.fixture=skin(type);RigAudit.begin();JumpAudit.begin();});c.waitTicks(25);
  if(equip.equals("guard")){c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->v.player.isUsingItem()&&v.player.getUsedItemHand()==InteractionHand.OFF_HAND);c.waitTicks(12);}
 }
 private static void jump(ClientGameTestContext c){int tick=c.computeOnClient(v->v.player.tickCount);c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround()&&v.player.getDeltaMovement().y>0);c.getInput().releaseKey(v->v.keyJump);check(c.computeOnClient(v->v.player.tickCount-tick<=3),"Source must not add ordinary jump input delay");}
 private static void cycle(ClientGameTestContext c,TestSingleplayerContext w,PlayerModelType type,HumanoidArm arm,String equip){
  prepare(c,w,type,arm,equip);jump(c);c.waitFor(v->JumpAudit.takeoff>0&&JumpAudit.quads>0&&RigAudit.bodies>0);c.takeScreenshot("jump-takeoff-"+type+"-"+arm+"-"+equip);var apex=new AtomicReference<Double>(120.0);c.waitFor(v->{apex.set(java.lang.Math.max(apex.get(),v.player.getY()));return JumpAudit.air>0;});c.takeScreenshot("jump-air-"+type+"-"+arm+"-"+equip);c.waitFor(v->{apex.set(java.lang.Math.max(apex.get(),v.player.getY()));return v.player.onGround()&&JumpAudit.land>0;});check(apex.get()>121.15&&apex.get()<121.35,"Actual ordinary native jump apex unchanged "+apex.get());c.takeScreenshot("jump-land-"+type+"-"+arm+"-"+equip);c.waitFor(v->!active(v));check(c.computeOnClient(v->v.player.onGround()&&v.player.getY()==120&&v.player.getHealth()==20&&RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())&&JumpAudit.quads>0&&(!equip.equals("empty")?RigAudit.shields>0:true)),"Native position, original skin/item GPU and landing restored");w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==0&&p.getMainHandItem().getDamageValue()==0&&p.getMainHandItem().get(FusionContent.DATA).remaining()==32&&PlayerStamina.get(p).stamina()==100&&p.getOffhandItem().getDamageValue()==0,"Jump source cannot attack, wear, consume Fuse or stamina");});
 }
 private static void boundaries(ClientGameTestContext c,TestSingleplayerContext w){
  prepare(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");c.getInput().holdKey(v->v.keyUp);jump(c);c.waitFor(v->JumpAudit.air>0&&v.player.getZ()>.65);c.takeScreenshot("jump-native-moving-no-control-lock");c.getInput().releaseKey(v->v.keyUp);c.waitFor(v->v.player.onGround()&&!active(v));
  prepare(c,w,PlayerModelType.SLIM,HumanoidArm.LEFT,"carry");jump(c);c.waitFor(JumpRigClientTest::active);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.hurtServer(s.overworld(),p.damageSources().generic(),2),"Actual damage event is accepted during native jump");});c.waitFor(v->v.player.getHealth()<20);check(c.computeOnClient(v->!active(v)),"Actual hurt immediately exits source jump without immunity");c.takeScreenshot("jump-native-hurt-exit");c.waitFor(v->v.player.onGround());check(c.computeOnClient(v->!active(v)),"Hurt cannot revive a source landing");
  prepare(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");jump(c);c.waitFor(JumpRigClientTest::active);c.getInput().holdKeyFor(v->v.keyAttack,1);c.waitFor(v->MeleeActions.view(v.player).phase()==dev.wildcraft.network.MeleeView.AIR_SWING);check(c.computeOnClient(v->!active(v)),"Actual native air attack owns body instead of ordinary source jump");c.takeScreenshot("jump-native-air-attack-owner");c.waitFor(v->v.player.onGround()&&MeleeActions.view(v.player).phase()==dev.wildcraft.network.MeleeView.IDLE);check(c.computeOnClient(v->!active(v)),"Air attack exits cannot revive old source landing");
  prepare(c,w,PlayerModelType.SLIM,HumanoidArm.RIGHT,"empty");w.getServer().runOnServer(s->{s.overworld().setBlockAndUpdate(new BlockPos(30,119,30),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),30.95,120,30.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);});c.waitFor(v->v.player.onGround()&&v.player.getX()>30);c.waitTicks(12);jump(c);c.waitFor(v->JumpAudit.air>0);c.waitFor(v->v.player.onGround());check(c.computeOnClient(v->!active(v)&&v.player.getY()==120),"Actual edge support keeps native position and rejects source planted feet outside collision top");c.takeScreenshot("jump-native-edge-support-fallback");
  System.out.println("C6AI actual moving-key/no control lock, native hurt/no immunity, air attack ownership/no stale landing, real collision edge support fallback PASS");
 }
 private static float clockExpected(JumpAudit.ClockSample a){
  check(a.start()>=0,"Actual submitted native upward ground transition observed");float age=(a.tick()-a.start()+a.partial())/20F;
  if(a.frame().clip().startsWith("jump_land_")){check(a.land()>=0,"Actual native landing transition observed");return (.06F+(a.tick()-a.land()+a.partial())/20F)/.3F;}
  return a.frame().clip().startsWith("jump_takeoff_")?age/.2F:((age-.2F)%.6F)/.6F;
 }
 private static void pauseClocks(ClientGameTestContext c,TestSingleplayerContext w){
  boolean matrix=Boolean.getBoolean("wildcraft.test.jumpClockMatrix");String selected=System.getProperty("wildcraft.test.jumpClockPhase","takeoff");int cases=0;var coverage=new HashSet<String>();JumpAudit.clockEnabled=true;
  for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values())for(String equip:new String[]{"empty","carry"})for(String phase:new String[]{"takeoff","air","land"}){
   if(!matrix&&(cases>0||!phase.equals(selected)))continue;
   w.getServer().runCommand("tick rate 20");c.runOnClient(v->v.options.framerateLimit().set(120));prepare(c,w,type,arm,equip);
   w.getServer().runCommand("tick rate 5");c.runOnClient(v->v.options.framerateLimit().set(10));jump(c);
   c.waitFor(v->JumpAudit.clockLast!=null&&JumpAudit.clockLast.frame().clip().startsWith("jump_"+phase+"_")&&JumpAudit.quads>0);
   var unpaused=c.computeOnClient(v->JumpAudit.clockLast);check(Math.abs(unpaused.frame().time()-clockExpected(unpaused))<1e-5F,"Unpaused actual transition and render fraction qualify independent clock anchors");
   int before=JumpAudit.frames;c.setScreen(()->new net.minecraft.client.gui.screens.PauseScreen(true));c.waitFor(Minecraft::isPaused);c.waitTicks(2);
   var a=c.computeOnClient(v->JumpAudit.clockLast);check(JumpAudit.frames>before&&a.frame().clip().startsWith("jump_"+phase+"_"),"Real paused submission stays in actual targeted native jump phase");
   float expected=clockExpected(a);String label=type+"-"+arm+"-"+equip+"-"+phase;System.out.println("C6AR_NATIVE_PAUSE "+label+" actual="+a.frame().time()+" expected="+expected+" partial="+a.partial()+" tick="+a.tick()+" start="+a.start()+" land="+a.land());c.takeScreenshot("jump-clock-"+label);
   if(a.partial()>.001F)coverage.add(phase);if(!matrix)check(a.partial()>.001F,"Single probe requires nonzero actual frozen fraction");check(Math.abs(a.frame().time()-expected)<1e-5F,"Jump source preserves native frozen fraction for "+phase+": actual="+a.frame().time()+" expected="+expected);
   var pos=c.computeOnClient(v->v.player.position());var vel=c.computeOnClient(v->v.player.getDeltaMovement());int frames=JumpAudit.frames;long until=System.nanoTime()+350_000_000L;c.waitFor(v->System.nanoTime()>until);var b=c.computeOnClient(v->JumpAudit.clockLast);
   check(JumpAudit.frames>frames&&b.tick()==a.tick()&&b.frame().clip().equals(a.frame().clip())&&Math.abs(b.frame().time()-a.frame().time())<1e-6F,"Repeated actual submitted jump freezes in native pause");
   for(int i=0;i<a.frame().global().length;i++)check(a.frame().global()[i].equals(b.frame().global()[i],2e-5F),"Repeated paused full skin palette stays frozen joint "+i);
   check(c.computeOnClient(v->v.player.position().distanceToSqr(pos)<1e-10&&v.player.getDeltaMovement().distanceToSqr(vel)<1e-10),"Only native pause freezes actual position and velocity");
   c.setScreen(()->null);w.getServer().runCommand("tick rate 20");c.runOnClient(v->v.options.framerateLimit().set(120));c.waitFor(v->v.player.onGround()&&!active(v));
   check(c.computeOnClient(v->v.player.getY()==120&&v.player.getHealth()==20&&RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())&&JumpAudit.takeoff>0&&JumpAudit.air>0&&JumpAudit.land>0&&JumpAudit.quads>0),"Resume keeps actual native takeoff/air/landing, skin and sword GPU");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==0&&p.getMainHandItem().getDamageValue()==0&&p.getMainHandItem().get(FusionContent.DATA).remaining()==32&&PlayerStamina.get(p).stamina()==100&&p.getOffhandItem().getDamageValue()==0,"Pause adds no attack, fee, wear or Fuse consumption");});cases++;
  }
  if(matrix)for(String phase:new String[]{"takeoff","air","land"})check(coverage.contains(phase),"Nonzero frozen native fraction coverage for "+phase);
  System.out.println("C6AR_COMPLETE actualPausedJumpCases="+cases+" originalFootThresholdsAndPhysics=true");
 }
 @Override public void runTest(ClientGameTestContext c){var opts=CombatOptions.get();int follow=opts.follow;boolean jumpDodge=opts.jumpDodge;
  try(TestSingleplayerContext w=c.worldBuilder().create()){
   w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runOnServer(s->{for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.getPlayerList().getPlayers().getFirst().teleportTo(.5,120,.5);});c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.follow=0;opts.jumpDodge=false;v.options.guiScale().set(2);});
   if(Boolean.getBoolean("wildcraft.test.jumpClock")){pauseClocks(c,w);return;}
   if(Boolean.getBoolean("wildcraft.test.jumpBoundaryOnly")){boundaries(c,w);return;}
   if(Boolean.getBoolean("wildcraft.test.jumpSlotProbe")){prepare(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");c.getInput().pressKey(v->v.keyHotbarSlots[8]);c.waitFor(v->v.player.getMainHandItem().is(Items.TORCH));prepare(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");System.out.println("C6AI actual native slot8-to-prepare PASS");return;}
   for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values())for(String equip:new String[]{"empty","carry","guard"})cycle(c,w,type,arm,equip);
   prepare(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");jump(c);c.waitFor(JumpRigClientTest::active);c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));check(c.computeOnClient(v->!active(v)),"Actual GUI cancels source immediately");c.takeScreenshot("jump-native-gui");c.setScreen(()->null);c.waitFor(v->v.player.onGround());check(c.computeOnClient(v->!active(v)),"GUI close cannot revive cancelled source landing");
   prepare(c,w,PlayerModelType.SLIM,HumanoidArm.LEFT,"empty");jump(c);c.waitFor(JumpRigClientTest::active);c.getInput().pressKey(v->v.keyHotbarSlots[8]);c.waitFor(v->v.player.getMainHandItem().is(Items.TORCH));check(c.computeOnClient(v->!active(v)),"Actual new held item cancels jump source");c.takeScreenshot("jump-native-new-item");c.waitFor(v->v.player.onGround());
   prepare(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE));jump(c);check(c.computeOnClient(v->!active(v)),"Armor retains original jump skin/armor path");c.takeScreenshot("jump-native-armor");c.waitFor(v->v.player.onGround());
   prepare(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().teleportTo(.5,125,.5));c.waitFor(v->!v.player.onGround()&&v.player.getDeltaMovement().y<0);check(c.computeOnClient(v->!active(v)),"Native falling without positive jump does not start takeoff");c.waitFor(v->v.player.onGround()&&v.player.getHealth()<20);check(c.computeOnClient(v->!active(v)),"Original fall damage has no source jump immunity or fabricated landing");c.takeScreenshot("jump-native-fall-damage");
   c.runOnClient(v->v.options.framerateLimit().set(10));cycle(c,w,PlayerModelType.SLIM,HumanoidArm.LEFT,"carry");c.runOnClient(v->v.options.framerateLimit().set(120));
   w.getServer().runCommand("tick rate 5");cycle(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");w.getServer().runCommand("tick rate 20");
   prepare(c,w,PlayerModelType.WIDE,HumanoidArm.RIGHT,"empty");jump(c);c.waitFor(JumpRigClientTest::active);c.setScreen(()->new net.minecraft.client.gui.screens.PauseScreen(true));c.waitFor(Minecraft::isPaused);var frozen=c.computeOnClient(v->frame(v));long until=System.nanoTime()+300_000_000L;c.waitFor(v->System.nanoTime()>until);check(c.computeOnClient(v->frame(v)!=null&&frame(v).clip().equals(frozen.clip())&&java.lang.Math.abs(frame(v).time()-frozen.time())<1e-6F),"Actual native pause freezes jump observation without stale gameplay");c.takeScreenshot("jump-native-pause");c.setScreen(()->null);c.waitFor(v->v.player.onGround()&&!active(v));
   boundaries(c,w);
   System.out.println("C6AI real ordinary jump key, original apex/landing/physics/zero attack-wear-Fuse-cost, native skins-RL-empty/carry/guard, exact source-foot/native sword quads, GUI-item-armor-fall and pause/10FPS/5TPS PASS");
  }finally{release(c);c.runOnClient(v->{JumpAudit.clockEnabled=false;JumpAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;opts.follow=follow;opts.jumpDodge=jumpDodge;v.options.framerateLimit().set(120);v.options.setCameraType(CameraType.FIRST_PERSON);});}
 }
}
