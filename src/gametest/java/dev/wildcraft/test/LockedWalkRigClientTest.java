package dev.wildcraft.test;

import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.PauseScreen;
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
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/** Real target lock and cardinal movement; existing damage, shields and inventory own the action. */
public final class LockedWalkRigClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static CombatRigLayer.Frame frame(Minecraft c){return state(c).getData(CombatRigLayer.FRAME);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin fixture");}
    private static void release(ClientGameTestContext c){c.getInput().releaseKey(v->v.keyUp);c.getInput().releaseKey(v->v.keyDown);c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);}
    private static KeyMapping key(Options o,String direction){return switch(direction){case "forward"->o.keyUp;case "backward"->o.keyDown;case "left"->o.keyLeft;default->o.keyRight;};}
    private static void feet(Minecraft c){
        var s=state(c);var f=s.getData(CombatRigLayer.FRAME);check(f!=null&&f.clip().contains("_cycle_"),"Actual selected walking cycle");var data=f.data();var transforms=new Matrix4f[data.skinBones()];var normals=new Matrix3f[transforms.length];
        for(int i=0;i<transforms.length;i++){transforms[i]=new Matrix4f(f.global()[i]).mul(new Matrix4f().set(data.inverseBind()[i]));normals[i]=transforms[i].normal(new Matrix3f());}
        var contacts=LockedWalkRig.feet(data.clips().get(f.clip()),f.time());int supports=0;
        for(int i=0;i<2;i++)if(contacts[i][3]==1){
            var point=LockedWalkRig.worldFoot(c.player.getPosition(1),s.bodyRot,contacts[i]);check(java.lang.Math.abs(point.y-120)<.012,"Authored planted sole fits actual flat floor");
            String part=i==0?"left_leg":"right_leg";float lowest=Float.POSITIVE_INFINITY;
            for(var mesh:data.meshes())if(!mesh.outer()&&mesh.part().equals(part)){var vertices=CombatRig.deform(mesh,transforms,normals);for(int v=0;v<vertices.length;v+=8)lowest=java.lang.Math.min(lowest,vertices[v+1]);}
            check(java.lang.Math.abs(lowest)<.025,"Actual skinned planted sole, beyond metadata alone: "+part+" lowest="+lowest);supports++;
        }
        check(supports>=1,"Source walk retains a declared support foot");
    }
    @Override public void runTest(ClientGameTestContext c){
        var target=new AtomicInteger();var opts=CombatOptions.get();boolean hold=opts.hold;int follow=opts.follow;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.getPlayerList().getPlayers().getFirst().teleportTo(.5,120,.5);var mob=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);mob.setPos(.5,120,9.5);mob.setNoAi(true);mob.setNoGravity(true);s.overworld().addFreshEntity(mob);target.set(mob.getId());});c.waitFor(v->v.level.getEntity(target.get())!=null);c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.hold=false;opts.follow=0;v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);v.options.guiScale().set(2);RigAudit.enabled=true;});
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var hand:HumanoidArm.values())for(boolean shield:new boolean[]{false,true})for(String direction:new String[]{"forward","backward","left","right"}){
                release(c);c.waitTicks(2);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.setMainArm(hand);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.setItemInHand(InteractionHand.OFF_HAND,shield?new ItemStack(Items.SHIELD):ItemStack.EMPTY);p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.position().distanceTo(new Vec3(.5,120,.5))<.1&&v.player.getMainArm()==hand&&v.player.getOffhandItem().isEmpty()!=shield);
                c.runOnClient(v->{RigAudit.fixture=skin(type);RigAudit.begin();v.player.setYRot(0);v.player.setXRot(0);});c.waitFor(v->ActionInput.tickFrame().gameplay());if(!c.computeOnClient(v->Combat.locked(v.player))){c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(v->Combat.locked(v.player));}c.waitTicks(5);
                String expected="walk_"+direction+"_cycle_"+(shield?"carry":"empty")+"_"+(hand==HumanoidArm.RIGHT?"R":"L");c.getInput().holdKey(v->key(v,direction));c.waitFor(v->frame(v)!=null&&frame(v).clip().equals(expected),100);c.waitTicks(3);c.runOnClient(LockedWalkRigClientTest::feet);
                check(c.computeOnClient(v->RigAudit.bodies>0&&RigAudit.hands>0&&(!shield||RigAudit.shields>0)&&RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Actual real skin/sword/shield reaches GPU");c.takeScreenshot("lock-walk-"+type+"-"+hand+"-shield"+shield+"-"+direction);
                Vec3 from=c.computeOnClient(v->v.player.position());float phase=c.computeOnClient(v->frame(v).time());float stride=c.computeOnClient(v->frame(v).data().clips().get(expected).distance());c.waitTicks(2);Vec3 to=c.computeOnClient(v->v.player.position());float advanced=c.computeOnClient(v->frame(v).time());
                check(to.distanceTo(from)>.04&&java.lang.Math.abs((advanced-phase+1)%1-(to.distanceTo(from)/stride)%1)<.1,"Walking cycle follows actual distance and approved stride");check(c.computeOnClient(v->v.player.getMainHandItem().getDamageValue()==0&&(!shield||v.player.getOffhandItem().getDamageValue()==0)),"Rendering adds no native wear/block");
                c.getInput().releaseKey(v->key(v,direction));c.waitFor(v->v.player.getDeltaMovement().horizontalDistanceSqr()<.000025,30);c.waitTicks(6);check(c.computeOnClient(v->frame(v)==null||!frame(v).clip().startsWith("walk_")),"Stop hands back to valid idle/native path");c.takeScreenshot("lock-walk-stopped-"+type+"-"+hand+"-shield"+shield+"-"+direction);
            }
            c.getInput().holdKey(v->v.keyUp);c.waitFor(v->frame(v)!=null&&frame(v).clip().startsWith("walk_forward_cycle"));c.getInput().holdKey(v->v.keyLeft);c.waitTicks(3);check(c.computeOnClient(v->frame(v)==null||!frame(v).clip().startsWith("walk_")),"Unqualified diagonal retains established native gait");c.takeScreenshot("lock-walk-native-diagonal");release(c);c.waitTicks(10);
            c.getInput().holdKey(v->v.keyUp);c.waitFor(v->frame(v)!=null&&frame(v).clip().startsWith("walk_forward_cycle"));c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->v.player.isUsingItem());c.waitFor(v->frame(v)!=null&&frame(v).clip().contains("_guard_"));check(c.computeOnClient(v->!frame(v).clip().contains("_carry_")),"Moving actual defense replaces carry source with qualified guard gait");c.takeScreenshot("lock-walk-real-moving-defense");c.getInput().releaseKey(WildcraftBindings.DEFEND);release(c);c.waitTicks(10);
            release(c);c.waitTicks(10);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);s.overworld().getEntity(target.get()).setPos(1.7,120,9.5);for(int y=120;y<=122;y++)for(int z=-3;z<=4;z++)s.overworld().setBlockAndUpdate(new BlockPos(2,y,z),Blocks.STONE.defaultBlockState());});c.waitFor(v->v.player.position().distanceTo(new Vec3(.5,120,.5))<.1);c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);});
            c.getInput().holdKey(v->v.keyLeft);c.waitFor(v->v.player.horizontalCollision,100);c.waitTicks(8);Vec3 blocked=c.computeOnClient(v->v.player.position());c.waitTicks(5);check(c.computeOnClient(v->v.player.position().distanceTo(blocked)<.005&&(frame(v)==null||!frame(v).clip().contains("_cycle_"))),"Held blocked motion cannot keep walking cycle");c.takeScreenshot("lock-walk-real-side-wall-blocked");release(c);
            w.getServer().runOnServer(s->{for(int y=120;y<=122;y++)for(int z=-3;z<=4;z++)s.overworld().setBlockAndUpdate(new BlockPos(2,y,z),Blocks.AIR.defaultBlockState());s.overworld().getEntity(target.get()).setPos(.5,120,9.5);var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.position().distanceTo(new Vec3(.5,120,.5))<.1&&v.player.getOffhandItem().isEmpty());c.waitTicks(5);c.getInput().holdKey(v->v.keyUp);c.waitFor(v->frame(v)!=null&&frame(v).clip().contains("_cycle_"));
            c.runOnClient(v->{v.options.keyAttack.setDown(true);ActionInput.frame(v);v.options.keyAttack.setDown(false);ActionInput.frame(v);});c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.SWING);check(c.computeOnClient(v->frame(v)==null||!frame(v).clip().startsWith("walk_")),"Actual attack takes pose ownership from walking");c.takeScreenshot("lock-walk-real-attack-priority");release(c);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);c.waitTicks(10);
            c.getInput().holdKey(v->v.keyUp);c.waitFor(v->frame(v)!=null&&frame(v).clip().contains("_cycle_"));c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);var paused=c.computeOnClient(LockedWalkRigClientTest::frame);long until=System.nanoTime()+350_000_000L;c.waitFor(v->System.nanoTime()>until);if(paused!=null)check(c.computeOnClient(v->frame(v)==null||!frame(v).clip().equals(paused.clip())||java.lang.Math.abs(frame(v).time()-paused.time())<.0001),"Pause freezes surviving cycle or genuine ownership cancellation exits");c.takeScreenshot("lock-walk-pause");c.setScreen(()->null);release(c);c.waitTicks(10);
            c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));c.waitTicks(2);check(c.computeOnClient(v->frame(v)==null),"GUI cancels gait");c.setScreen(()->null);c.waitTicks(2);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE));c.getInput().holdKeyFor(v->v.keyUp,5);check(c.computeOnClient(v->frame(v)==null),"Unqualified armor remains native");c.takeScreenshot("lock-walk-native-armor");
            System.out.println("C6I actual locked cardinal inputs, wide/slim left/right empty/carry native sword/shield, real distance/flat contacts/skinned soles, no wear, stop/blocked/attack/pause/native diagonal/defense/GUI/armor PASS");
        }finally{release(c);c.getInput().releaseKey(WildcraftBindings.DEFEND);c.runOnClient(v->{RigAudit.enabled=false;RigAudit.fixture=null;opts.hold=hold;opts.follow=follow;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
