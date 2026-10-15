package dev.wildcraft.test;

import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.*;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real goal/X success; READY has no slowdown and must finish art over the current native landing pose. */
public final class DodgeFeedbackModesClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static class Goal extends ZombieAttackGoal {Goal(Husk h){super(h,1,true);}void attempt(LivingEntity p){checkAndPerformAttack(p);}}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Skin fixture");}
    private static void release(ClientGameTestContext c){c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyDown);c.getInput().releaseKey(WildcraftBindings.DODGE);}
    private static void prepare(ClientGameTestContext c,TestSingleplayerContext w,int id,PlayerModelType type,HumanoidArm arm,boolean carry,String direction,int mode){
        release(c);c.getInput().holdKeyFor(v->v.keyHotbarSlots[0],1);c.waitTicks(16);
        w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ReactionTime.cancel(p);FocusTime.interrupt(p);p.stopUsingItem();MeleeActions.receive(p,new MeleeIntent(MeleeIntent.CANCEL,(byte)0));p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);p.setMainArm(arm);p.setHealth(20);p.setInvulnerableTime(0);PlayerStamina.fill(p);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);var sword=new ItemStack(Items.IRON_SWORD);check(FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Actual Fuse fixture");p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.getInventory().setItem(1,new ItemStack(Items.TORCH));p.setItemInHand(InteractionHand.OFF_HAND,carry?new ItemStack(Items.SHIELD):ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();var m=(Husk)s.overworld().getEntity(id);ThreatActions.cancel(m);m.setHealth(20);m.setTarget(p);m.setDeltaMovement(Vec3.ZERO);m.setPos(direction.equals("left")?-.8:direction.equals("right")?1.8:.5,120,direction.equals("back")?1.8:1.2);CounterAttackAudit.begin(p);});
        c.waitFor(v->v.player.onGround()&&v.player.getMainArm()==arm&&v.player.getMainHandItem().is(Items.IRON_SWORD)&&v.player.getOffhandItem().isEmpty()==!carry);
        c.runOnClient(v->{CombatOptions.get().dodgeSkill=mode;v.player.setYRot(0);v.player.setXRot(0);RigAudit.fixture=skin(type);RigAudit.begin();DodgeFeedbackAudit.begin();});c.waitTicks(3);
    }
    private static void escape(ClientGameTestContext c,TestSingleplayerContext w,int id,String direction){
        w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();new Goal((Husk)s.overworld().getEntity(id)).attempt(p);});c.waitFor(v->ThreatActions.view((LivingEntity)v.level.getEntity(id)).age()>=6);
        if(direction.equals("left"))c.getInput().holdKey(v->v.keyLeft);else if(direction.equals("right"))c.getInput().holdKey(v->v.keyRight);else c.getInput().holdKey(v->v.keyDown);c.getInput().holdKeyFor(WildcraftBindings.DODGE,1);
    }
    private static void unchanged(TestSingleplayerContext w){w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==0&&p.getHealth()==20&&p.getMainHandItem().getDamageValue()==0&&p.getMainHandItem().get(FusionContent.DATA).remaining()==32&&p.getOffhandItem().getDamageValue()==0&&PlayerStamina.get(p).stamina()>=92,"Real miss/original eight-cost hop, no animation attack/damage/wear/Fuse/block");});}
    private static void early(ClientGameTestContext c){c.waitFor(v->state(v).getData(DodgeFeedbackRig.PULSE)!=null&&DodgeFeedbackAudit.torsoDelta>.005);release(c);check(DodgeFeedbackAudit.lastTime<.9,"Exit fixture reaches live source before its natural end");}
    private static void off(ClientGameTestContext c,String reason){check(c.computeOnClient(v->state(v).getData(DodgeFeedbackRig.PULSE)==null),reason);}
    @Override public void runTest(ClientGameTestContext c){var id=new AtomicInteger();var opts=CombatOptions.get();int mode=opts.dodgeSkill,follow=opts.follow;boolean jump=opts.jumpDodge;
        String selected=System.getProperty("wildcraft.test.dodgeModeCase","all");int manualCases=0;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runOnServer(s->{for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,1.8);m.setNoAi(true);m.setNoGravity(true);s.overworld().addFreshEntity(m);id.set(m.getId());});c.waitFor(v->v.player.getY()>119&&v.level.getEntity(id.get())!=null);c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.follow=0;opts.jumpDodge=false;v.options.setCameraType(CameraType.THIRD_PERSON_BACK);v.options.guiScale().set(2);});c.waitFor(v->ActionInput.tickFrame().gameplay());
            for(var type:PlayerModelType.values())for(var arm:HumanoidArm.values())for(boolean carry:new boolean[]{false,true})for(String dir:new String[]{"left","right","back"}){
                if(!selected.equals("all")&&!selected.equals(type+"-"+arm+"-"+carry+"-"+dir))continue;
                System.out.println("DODGE MANUAL CASE "+type+"-"+arm+"-"+carry+"-"+dir);
                prepare(c,w,id.get(),type,arm,carry,dir,1);escape(c,w,id.get(),dir);c.waitFor(v->ReactionTime.view(v.player).phase()==ReactionView.READY&&DodgeFeedbackAudit.torsoDelta>.005);release(c);
                check(c.computeOnClient(v->state(v).getData(DodgeFeedbackRig.PULSE).clip().contains("feedback_"+dir+"_")),"Actual bound movement selects the requested source direction");
                // PNG readback/write can stall a .24s envelope; observe the return before taking the image.
                c.waitFor(v->state(v).getData(DodgeFeedbackRig.PULSE)==null);
                check(DodgeFeedbackAudit.nativeFrames>0&&DodgeFeedbackAudit.nativeItemChecks>0&&DodgeFeedbackAudit.nativeGpuFrames>0,"Manual source genuinely continues through HOP end over current native body and original item transforms, submitted to GPU");
                double elapsed=(DodgeFeedbackAudit.finishedNanos-DodgeFeedbackAudit.firstNanos)/1_000_000_000.0,remaining=.24*(1-DodgeFeedbackAudit.firstTime);
                check(DodgeFeedbackAudit.finishedNanos>0&&elapsed>=remaining-.008&&elapsed<=remaining+.08,"Manual source has no early gap or added lifetime");
                double lastGap=(DodgeFeedbackAudit.finishedNanos-DodgeFeedbackAudit.lastNanos)/1e9;
                check(DodgeFeedbackAudit.lastTime>.9||lastGap>=.24*(1-DodgeFeedbackAudit.lastTime)-.008,"A skipped final sample must be explained by the actual final frame gap, never early loss of baseline");
                check(c.computeOnClient(v->ReactionTime.view(v.player).phase()==ReactionView.READY&&RigAudit.bodies>0&&RigAudit.hands>0&&RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Real READY/current skin and items reach GPU before confirmation");
                c.takeScreenshot("dodge-manual-return-before-confirm-"+dir+"-"+type+"-"+arm+"-"+(carry?"carry":"empty"));
                w.getServer().runOnServer(s->check(s.tickRateManager().tickrate()==20,"READY does not slow world time"));c.getInput().holdKeyFor(WildcraftBindings.CONFIRM_SKILL,1);c.waitFor(v->ReactionTime.active(v.player));unchanged(w);c.waitFor(v->!ReactionTime.active(v.player));w.getServer().runOnServer(s->check(s.tickRateManager().tickrate()==20,"Confirmed lease releases without art extension"));
                manualCases++;
            }
            check(manualCases==(selected.equals("all")?24:1),"Requested actual manual cases executed");if(!selected.equals("all"))return;
            for(var type:PlayerModelType.values())for(var arm:HumanoidArm.values())for(String dir:new String[]{"left","right","back"}){
                prepare(c,w,id.get(),type,arm,arm==HumanoidArm.LEFT,dir,2);escape(c,w,id.get(),dir);c.waitFor(v->MeleeActions.hopping(v.player));release(c);c.takeScreenshot("dodge-disabled-real-"+dir+"-"+type+"-"+arm);c.waitFor(v->!MeleeActions.hopping(v.player)&&v.player.onGround());c.waitTicks(5);check(DodgeFeedbackAudit.frames==0,"Disabled mode never invents a successful source pulse");off(c,"Disabled source stays off");unchanged(w);w.getServer().runOnServer(s->check(!ReactionTime.active(s)&&s.tickRateManager().tickrate()==20,"Disabled real native miss retains normal time"));
            }
            // Menu init and removal can both happen before another render extraction.
            prepare(c,w,id.get(),PlayerModelType.WIDE,HumanoidArm.RIGHT,false,"back",1);escape(c,w,id.get(),"back");early(c);int canceled=DodgeFeedbackAudit.serial;c.runOnClient(v->{v.gui.setScreen(new InventoryScreen(v.player));v.gui.setScreen(null);check(state(v).getData(DodgeFeedbackRig.PULSE)==null,"Menu between render frames consumes active source");});off(c,"Closed GUI cannot replay same success");c.takeScreenshot("dodge-source-gui-between-renders");
            prepare(c,w,id.get(),PlayerModelType.WIDE,HumanoidArm.RIGHT,false,"back",1);escape(c,w,id.get(),"back");early(c);int fresh=DodgeFeedbackAudit.serial;check(fresh!=0&&fresh!=canceled,"Fresh actual success after GUI is not blocked by old canceled hop");
            c.runOnClient(v->{v.options.keyAttack.setDown(true);check(state(v).getData(DodgeFeedbackRig.PULSE)==null,"Live attack key hands over source immediately");v.options.keyAttack.setDown(false);check(state(v).getData(DodgeFeedbackRig.PULSE)==null,"Released attack does not restart canceled source");});
            c.takeScreenshot("dodge-source-fresh-success-then-attack");
            prepare(c,w,id.get(),PlayerModelType.SLIM,HumanoidArm.LEFT,true,"back",1);escape(c,w,id.get(),"back");early(c);c.runOnClient(v->{v.gameMode.useItem(v.player,InteractionHand.OFF_HAND);check(v.player.isUsingItem(),"Real native shield-use fixture");check(state(v).getData(DodgeFeedbackRig.PULSE)==null,"Actual use owns hands immediately");});c.takeScreenshot("dodge-source-real-use-handoff");
            prepare(c,w,id.get(),PlayerModelType.WIDE,HumanoidArm.RIGHT,false,"back",1);escape(c,w,id.get(),"back");early(c);c.runOnClient(v->{v.player.getInventory().setSelectedSlot(1);((dev.wildcraft.mixin.client.CarriedItemAccess)v.gameMode).wildcraft$syncCarriedItem();check(state(v).getData(DodgeFeedbackRig.PULSE)==null,"Real selected torch owns hands immediately");});c.takeScreenshot("dodge-source-real-slot-handoff");
            prepare(c,w,id.get(),PlayerModelType.WIDE,HumanoidArm.RIGHT,false,"back",1);escape(c,w,id.get(),"back");early(c);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.hurtServer(s.overworld(),p.damageSources().generic(),1);check(p.getHealth()<20,"Source feedback adds no damage immunity");});c.waitFor(v->v.player.getHealth()<20);off(c,"Actual damage cancels source");c.takeScreenshot("dodge-source-real-damage-handoff");
            prepare(c,w,id.get(),PlayerModelType.WIDE,HumanoidArm.RIGHT,false,"back",1);escape(c,w,id.get(),"back");c.waitFor(v->MeleeActions.hopping(v.player)&&ReactionTime.view(v.player).phase()==ReactionView.IDLE);
            c.runOnClient(v->{v.gui.setScreen(new InventoryScreen(v.player));v.gui.setScreen(null);check(state(v).getData(DodgeFeedbackRig.PULSE)==null,"Menu consumes the actual hop before its late reward arrives");});release(c);c.waitFor(v->ReactionTime.view(v.player).phase()==ReactionView.READY);c.waitTicks(6);check(DodgeFeedbackAudit.frames==0,"Actual late earned reward from the same canceled hop never replays art");off(c,"Late canceled event remains off");unchanged(w);c.takeScreenshot("dodge-source-real-late-reward-after-gui");
            System.out.println("C6M MODES real manual 24/native HOP-to-current-body continuation/vanilla item-pose equality/READY-CONFIRM/lease; disabled 12 real misses; live menu/fresh success/attack/native use/slot/damage exits and GPU PASS");
        }finally{release(c);c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);c.getInput().releaseKey(WildcraftBindings.CONFIRM_SKILL);c.runOnClient(v->{DodgeFeedbackAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;opts.dodgeSkill=mode;opts.follow=follow;opts.jumpDodge=jump;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
