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
public final class DodgeFeedbackClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
    private static class Goal extends ZombieAttackGoal{Goal(Husk h){super(h,1,true);}void attempt(LivingEntity p){checkAndPerformAttack(p);}}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Skin fixture");}
    @Override public void runTest(ClientGameTestContext c){var id=new AtomicInteger();var opts=CombatOptions.get();int mode=opts.dodgeSkill,follow=opts.follow;boolean jump=opts.jumpDodge;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runOnServer(s->{for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,1.8);m.setNoAi(true);m.setNoGravity(true);s.overworld().addFreshEntity(m);id.set(m.getId());});c.waitFor(v->v.player.getY()>119&&v.level.getEntity(id.get())!=null);c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.dodgeSkill=0;opts.follow=0;opts.jumpDodge=false;v.options.setCameraType(CameraType.THIRD_PERSON_BACK);v.options.guiScale().set(2);});c.waitFor(v->ActionInput.tickFrame().gameplay());c.waitTicks(4);
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values())for(boolean carry:new boolean[]{false,true})for(String dir:new String[]{"back","left","right"}){
                c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyDown);c.waitTicks(16);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ReactionTime.cancel(p);FocusTime.interrupt(p);p.stopUsingItem();MeleeActions.reset(p);p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);p.setMainArm(arm);p.setHealth(20);p.setInvulnerableTime(0);PlayerStamina.fill(p);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);var sword=new ItemStack(Items.IRON_SWORD);check(FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Actual Fuse fixture");p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setItemInHand(InteractionHand.OFF_HAND,carry?new ItemStack(Items.SHIELD):ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();var m=(Husk)s.overworld().getEntity(id.get());ThreatActions.cancel(m);m.setHealth(20);m.setTarget(p);m.setDeltaMovement(Vec3.ZERO);m.setPos(dir.equals("left")?-.8:dir.equals("right")?1.8:.5,120,dir.equals("back")?1.8:1.2);CounterAttackAudit.begin(p);});
                c.waitFor(v->v.player.onGround()&&v.player.getMainArm()==arm&&v.player.getMainHandItem().is(Items.IRON_SWORD)&&v.player.getOffhandItem().isEmpty()==!carry);c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);RigAudit.fixture=skin(type);RigAudit.begin();DodgeFeedbackAudit.begin();v.options.framerateLimit().set(type==PlayerModelType.WIDE&&arm==HumanoidArm.LEFT&&!carry&&dir.equals("back")?10:120);});c.waitTicks(3);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();new Goal((Husk)s.overworld().getEntity(id.get())).attempt(p);});c.waitFor(v->ThreatActions.view((LivingEntity)v.level.getEntity(id.get())).age()>=6);
                if(dir.equals("left"))c.getInput().holdKey(v->v.keyLeft);else if(dir.equals("right"))c.getInput().holdKey(v->v.keyRight);else c.getInput().holdKey(v->v.keyDown);c.getInput().holdKeyFor(WildcraftBindings.DODGE,1);c.waitFor(v->ReactionTime.active(v.player)&&DodgeFeedbackAudit.frames>0&&DodgeFeedbackAudit.torsoDelta>.005&&RigAudit.bodies>0&&RigAudit.hands>0);c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyDown);
                boolean low=type==PlayerModelType.WIDE&&arm==HumanoidArm.LEFT&&!carry&&dir.equals("back");
                c.takeScreenshot("dodge-feedback-real-"+dir+"-"+type+"-"+arm+"-"+(carry?"carry":"empty"));check(c.computeOnClient(v->RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Actual skin/model reaches GPU");c.waitFor(v->state(v).getData(DodgeFeedbackRig.PULSE)==null);if(low)check(DodgeFeedbackAudit.finishedNanos>0&&(DodgeFeedbackAudit.finishedNanos-DodgeFeedbackAudit.firstNanos)/1_000_000_000.0<=.24*(1-DodgeFeedbackAudit.firstTime)+.16,"10FPS source envelope consumes actual frame gaps within one frame plus scheduling tolerance");check(c.computeOnClient(v->ReactionTime.active(v.player)),"Short .24 body envelope ends while original 1.5 lease remains");w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==0&&p.getHealth()==20&&p.getMainHandItem().getDamageValue()==0&&p.getMainHandItem().get(FusionContent.DATA).remaining()==32&&p.getOffhandItem().getDamageValue()==0&&PlayerStamina.get(p).stamina()>=92,"Real missed attack/one eight-cost hop, no source damage/wear/Fuse/block");});c.waitFor(v->!ReactionTime.active(v.player));w.getServer().runOnServer(s->check(s.tickRateManager().tickrate()==20,"Original real-time lease releases without art extension"));
            }
            System.out.println("C6M real native goal miss and X events, all directions/wide-slim/left-right/empty-carry GPU, fixed current root-pelvis-legs/head aim, .24 envelope independent of 1.5 lease/no damage-wear-Fuse PASS");
        }finally{c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyDown);c.getInput().releaseKey(WildcraftBindings.DODGE);c.runOnClient(v->{DodgeFeedbackAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;opts.dodgeSkill=mode;opts.follow=follow;opts.jumpDodge=jump;v.options.framerateLimit().set(120);v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
