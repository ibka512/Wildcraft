package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.combat.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.joml.*;
/** Test-only actual native model/reach diagnostic; no runtime logging or forced renderer phase. */
public final class RightBowReachProbe implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){try(TestSingleplayerContext w=c.worldBuilder().create()){
  w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runOnServer(s->{for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.getPlayerList().getPlayers().getFirst().teleportTo(.5,120,.5);});
  GroundBowRigClientTest.prepare(c,w,PlayerModelType.WIDE,InteractionHand.MAIN_HAND);w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setMainArm(HumanoidArm.RIGHT));c.waitFor(v->v.player.getMainArm()==HumanoidArm.RIGHT);c.runOnClient(v->{GroundBowAudit.enabled=false;});c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.isUsingItem());
  for(int tick:new int[]{5,10,13,18,20}){c.waitFor(v->v.player.getTicksUsingItem()>=tick);c.runOnClient(v->{try{
   var p=v.player;var state=GroundBowRigClientTest.state(v);var d=CombatRig.asset("standard");var names=Arrays.asList(d.bones());int actual=p.getTicksUsingItem();String name=actual>=20?"bow_ground_hold_empty_R":"bow_ground_draw_empty_R";float time=actual>=20?0:CombatRig.pullSample(d.clips().get(name),actual/20F);var g=CombatRig.sample("standard",name,time);var f=new CombatRigLayer.Frame(d,g,name,time,1);
   var nock=CombatRig.class.getDeclaredMethod("nativeBowString",CombatRigLayer.Frame.class,net.minecraft.client.renderer.item.ItemStackRenderState.class,int.class,HumanoidArm.class);nock.setAccessible(true);var target=(Vector3f)nock.invoke(null,f,state.rightHandItemState,actual,HumanoidArm.RIGHT);
   var shoulder=g[names.indexOf("left_upper_arm")].getTranslation(new Vector3f());var elbow=g[names.indexOf("left_elbow_deform")].getTranslation(new Vector3f());var palm=g[names.indexOf("left_palm_grip")].getTranslation(new Vector3f());var fit=GroundBowRig.class.getDeclaredMethod("fitDrawingArm",Matrix4f[].class,List.class,Vector3f.class,String.class);fit.setAccessible(true);
   System.out.println("C6AL REACH ticks="+actual+" target="+target+" sourcePalm="+palm+" shoulderDistance="+target.distance(shoulder)+" lengths="+shoulder.distance(elbow)+","+elbow.distance(palm)+" fit="+fit.invoke(null,g,names,target,"left")+" onGround="+p.onGround()+" pose="+p.getPose()+" mainArm="+p.getMainArm()+" sameUse="+(p.getUseItem()==p.getMainHandItem())+" nativeBowEmpty="+state.rightHandItemState.isEmpty()+" phase="+MeleeActions.view(p).phase()+" source="+(state.getData(CombatRigLayer.FRAME)==null?"NONE":state.getData(CombatRigLayer.FRAME).clip()));
  }catch(ReflectiveOperationException e){throw new RuntimeException(e);}});}
  c.takeScreenshot("right-bow-actual-native-reach-probe");
 }finally{c.getInput().releaseKey(v->v.keyUse);c.runOnClient(v->{RigAudit.enabled=false;RigAudit.fixture=null;GroundBowAudit.enabled=false;});}}
}
