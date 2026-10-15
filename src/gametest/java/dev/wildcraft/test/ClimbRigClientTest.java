package dev.wildcraft.test;

import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.input.*;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.traversal.Climbing;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Actual G/movement/collision and native GPU skins; no forced pose or gravity override. */
public final class ClimbRigClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static CombatRigLayer.Frame frame(Minecraft c){return state(c).getData(CombatRigLayer.FRAME);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin fixture");}
    private static Vec3 origin(Direction face){return new Vec3(face.getStepX()==0?.5:face.getStepX()>0?4.7:-3.7,121,face.getStepZ()==0?.5:face.getStepZ()>0?4.7:-3.7);}
    private static void pose(ServerPlayer p,Direction face){
        Climbing.reset(p,false);PlayerStamina.fill(p);p.getInventory().clearContent();p.setHealth(p.getMaxHealth());p.hurtTime=0;p.setNoGravity(false);p.setDeltaMovement(Vec3.ZERO);p.resetFallDistance();p.inventoryMenu.broadcastChanges();
        var at=origin(face);check(p.teleportTo(p.level(),at.x,at.y,at.z,Set.of(),face.toYRot(),0,true),"Fixture teleport accepted");
    }
    private static void release(ClientGameTestContext c){c.getInput().releaseKey(ClimbControls.CLIMB);c.getInput().releaseKey(v->v.keyUp);c.getInput().releaseKey(v->v.keyDown);c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyAttack);}
    private static void reset(ClientGameTestContext c,TestSingleplayerContext w,Direction face){
        release(c);c.waitTicks(2);w.getServer().runOnServer(s->pose(s.getPlayerList().getPlayers().getFirst(),face));var at=origin(face);
        c.waitFor(v->v.player.position().distanceTo(at)<.35);c.runOnClient(v->{v.player.setYRot(face.toYRot());v.player.setXRot(0);RigAudit.begin();});c.waitFor(v->ActionInput.tickFrame().gameplay());
    }
    private static void contacts(Minecraft c,Direction face){
        var f=frame(c);check(f!=null&&f.weight()>.99,"Qualified actual climb frame");
        float offset=f.global()[0].m32()-CombatRig.matrix(f.data().bindWorld()[0]).m32();var points=ClimbRig.contacts(f.data().clips().get(f.clip()),f.time());
        Vec3 normal=new Vec3(face.getStepX(),0,face.getStepZ());double plane=face.getStepX()>0||face.getStepZ()>0?5:-4;int count=0;
        for(var point:points){var at=ClimbRig.worldContact(c.player.getPosition(1),face,point,offset);double axis=face.getAxis()==Direction.Axis.X?at.x:at.z;if(point[3]==1)check(java.lang.Math.abs(axis-plane)<.012,"Planted source contact fits actual wall plane: "+f.clip()+" gap="+(axis-plane));if(java.lang.Math.abs(axis-plane)<.012)count++;}
        check(count>=2,"At least two current source contact targets reach the real wall; authored release flags stay unchanged");
        var transforms=new Matrix4f[f.data().skinBones()];var normals=new Matrix3f[transforms.length];
        for(int i=0;i<transforms.length;i++){transforms[i]=new Matrix4f(f.global()[i]).mul(new Matrix4f().set(f.data().inverseBind()[i]));normals[i]=transforms[i].normal(new Matrix3f());}
        for(int limb=0;limb<4;limb++)if(points[limb][3]==1){
            String part=(limb==0||limb==2?"left_":"right_")+(limb<2?"arm":"leg");boolean found=false;
            for(var mesh:f.data().meshes())if(!mesh.outer()&&mesh.part().equals(part)){
                var vertices=CombatRig.deform(mesh,transforms,normals);
                for(int i=0;i<vertices.length;i+=8)if(java.lang.Math.abs(vertices[i]-points[limb][0])<.18&&java.lang.Math.abs(vertices[i+1]-points[limb][1])<.18&&java.lang.Math.abs(vertices[i+2]-offset-points[limb][2])<.016)found=true;
            }
            check(found,"Actual skinned "+part+" surface reaches its planted contact target, beyond metadata alone");
        }
        check(java.lang.Math.abs(state(c).bodyRot-face.toYRot())<.01,"Body faces actual contact, not camera");
    }
    @Override public void runTest(ClientGameTestContext c){
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());for(int a=-9;a<=9;a++)for(int y=120;y<=133;y++){for(int n:new int[]{-5,5}){s.overworld().setBlockAndUpdate(new BlockPos(n,y,a),Blocks.STONE.defaultBlockState());s.overworld().setBlockAndUpdate(new BlockPos(a,y,n),Blocks.STONE.defaultBlockState());}}});
            c.getInput().resizeWindow(1280,960);c.runOnClient(v->{v.options.setCameraType(CameraType.THIRD_PERSON_BACK);v.options.guiScale().set(2);RigAudit.enabled=true;RigAudit.bodyYaw=null;});
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var face:new Direction[]{Direction.SOUTH,Direction.NORTH,Direction.EAST,Direction.WEST}){
                reset(c,w,face);c.runOnClient(v->RigAudit.fixture=skin(type));c.getInput().holdKey(ClimbControls.CLIMB);c.waitFor(v->Climbing.active(v.player));
                c.getInput().holdKey(v->v.keyUp);c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("climb_up")&&frame(v).weight()>.99);c.waitTicks(3);
                c.runOnClient(v->{contacts(v,face);check(RigAudit.bodies>0&&RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString()),"Actual native skin reaches GPU");});
                c.takeScreenshot("climb-rig-up-"+type+"-"+face);
                double start=c.computeOnClient(v->v.player.getY());float phase=c.computeOnClient(v->frame(v).time());c.waitTicks(2);double distance=c.computeOnClient(v->v.player.getY())-start;float advanced=c.computeOnClient(v->frame(v).time());
                check(distance>.1&&java.lang.Math.abs((advanced-phase+1)%1-(distance/.43)%1)<.09,"Up phase follows actual distance/.43; actual="+distance+" phase="+phase+" -> "+advanced);
                c.getInput().releaseKey(v->v.keyUp);c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("climb_idle_up"));double y=c.computeOnClient(v->v.player.getY());c.waitTicks(4);check(java.lang.Math.abs(c.computeOnClient(v->v.player.getY())-y)<.02,"Source idle adds no entity movement");c.takeScreenshot("climb-rig-hold-"+type+"-"+face);
                c.runOnClient(v->v.player.setYRot(face.toYRot()+35));c.waitTicks(2);c.takeScreenshot("climb-rig-contact-side-view-"+type+"-"+face);c.runOnClient(v->v.player.setYRot(face.toYRot()));
                for(boolean left:new boolean[]{true,false}){
                    c.getInput().holdKey(v->left?v.keyLeft:v.keyRight);String clip=left?"climb_left":"climb_right";
                    c.waitFor(v->frame(v)!=null&&frame(v).clip().equals(clip));c.runOnClient(v->contacts(v,face));c.takeScreenshot("climb-rig-"+clip+"-"+type+"-"+face);
                    Vec3 from=c.computeOnClient(v->v.player.position());float a=c.computeOnClient(v->frame(v).time());c.waitTicks(2);Vec3 to=c.computeOnClient(v->v.player.position());float b=c.computeOnClient(v->frame(v).time());double side=(to.x-from.x)*face.getStepZ()-(to.z-from.z)*face.getStepX();
                    check((left?side:-side)>.08&&java.lang.Math.abs((b-a+1)%1-(java.lang.Math.abs(side)/.32)%1)<.1,"A/D selects correct source and actual sideways distance/.32");
                    c.getInput().releaseKey(v->left?v.keyLeft:v.keyRight);c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("climb_idle_"+(left?"left":"right")));c.takeScreenshot("climb-rig-idle-"+(left?"left":"right")+"-"+type+"-"+face);
                }
                c.runOnClient(v->{v.player.setYRot(face.toYRot()+60);v.player.setXRot(45);});c.waitTicks(2);c.runOnClient(v->{var f=frame(v);int head=Arrays.asList(f.data().bones()).indexOf("head");var angles=f.global()[head].getUnnormalizedRotation(new Quaternionf()).getEulerAnglesYXZ(new Vector3f());check(java.lang.Math.abs(angles.y)<.36&&java.lang.Math.abs(angles.x)<.15,"Head visual limit20/8");check(java.lang.Math.abs(v.player.getXRot()-45)<.1,"Head limit never pulls native camera");});c.takeScreenshot("climb-rig-free-camera-head-limit-"+type+"-"+face);
                c.getInput().holdKey(v->v.keyDown);c.waitFor(v->v.player.getAttached(Climbing.VISUAL).motion()==2);check(c.computeOnClient(v->frame(v)==null),"Unqualified descending source retains old pose");c.takeScreenshot("climb-rig-native-down-"+type+"-"+face);c.getInput().releaseKey(v->v.keyDown);
                c.getInput().releaseKey(ClimbControls.CLIMB);c.waitFor(v->!Climbing.active(v.player));check(c.computeOnClient(v->frame(v)==null),"Actual grip release removes source frame");
            }
            reset(c,w,Direction.SOUTH);c.getInput().holdKey(ClimbControls.CLIMB);c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("climb_idle_up"));
            for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                var armor=switch(slot){case HEAD->Items.IRON_HELMET;case CHEST->Items.IRON_CHESTPLATE;case LEGS->Items.IRON_LEGGINGS;default->Items.IRON_BOOTS;};w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(slot,new ItemStack(armor));p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemBySlot(slot).is(armor));check(c.computeOnClient(v->frame(v)==null),"Unqualified armor native fallback");c.takeScreenshot("climb-rig-native-armor-"+slot);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(slot,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemBySlot(slot).isEmpty());
            }
            for(var hand:InteractionHand.values()){
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(hand,new ItemStack(Items.TORCH));p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemInHand(hand).is(Items.TORCH));check(c.computeOnClient(v->frame(v)==null),"Held inventory stays native");c.takeScreenshot("climb-rig-native-occupied-"+hand);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(hand,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemInHand(hand).isEmpty());
            }
            c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));c.waitTicks(3);check(c.computeOnClient(v->frame(v)==null),"GUI removes adopted traversal");c.takeScreenshot("climb-rig-gui");c.setScreen(()->null);release(c);
            reset(c,w,Direction.SOUTH);c.getInput().holdKey(ClimbControls.CLIMB);c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("climb_idle_up"));
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->state(v).currentSwing!=null);check(c.computeOnClient(v->frame(v)==null),"Actual native hand swing has presentation priority");c.takeScreenshot("climb-rig-native-attack-priority");c.getInput().releaseKey(v->v.keyAttack);release(c);
            reset(c,w,Direction.SOUTH);c.getInput().holdKey(ClimbControls.CLIMB);c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("climb_idle_up"));
            c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);var frozen=c.computeOnClient(ClimbRigClientTest::frame);long until=System.nanoTime()+400_000_000L;c.waitFor(v->System.nanoTime()>until);if(frozen!=null)check(c.computeOnClient(v->frame(v)==null||java.lang.Math.abs(frame(v).time()-frozen.time())<.0001),"Pause freezes surviving same climb clip, or genuine input release cancels");c.takeScreenshot("climb-rig-pause");c.setScreen(()->null);release(c);
            reset(c,w,Direction.SOUTH);w.getServer().runOnServer(s->{for(int x=-4;x<=4;x++)for(int z=3;z<=4;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,124,z),Blocks.STONE.defaultBlockState());});c.waitTicks(3);
            c.getInput().holdKey(ClimbControls.CLIMB);c.getInput().holdKey(v->v.keyUp);c.waitFor(v->v.player.getBoundingBox().maxY>=123.99);c.waitTicks(8);double blocked=c.computeOnClient(v->v.player.getY());c.waitTicks(8);check(java.lang.Math.abs(c.computeOnClient(v->v.player.getY())-blocked)<.001,"Native ceiling blocks entity");check(c.computeOnClient(v->frame(v)!=null&&frame(v).clip().equals("climb_idle_up")),"Blocked held-up input selects non-stepping support idle");c.takeScreenshot("climb-rig-real-ceiling-blocked");release(c);
            w.getServer().runOnServer(s->{for(int x=-4;x<=4;x++)for(int z=3;z<=4;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,124,z),Blocks.AIR.defaultBlockState());});
            reset(c,w,Direction.SOUTH);c.getInput().holdKey(ClimbControls.CLIMB);c.waitFor(v->frame(v)!=null);c.getInput().holdKey(v->v.keyLeft);
            c.waitFor(v->v.player.getAttached(Climbing.VISUAL).face()==Direction.EAST.ordinal(),100);c.getInput().releaseKey(v->v.keyLeft);c.waitTicks(5);check(c.computeOnClient(v->Climbing.active(v.player)&&frame(v)==null),"Actual inside corner keeps old path for this grip");c.takeScreenshot("climb-rig-native-corner");release(c);
            reset(c,w,Direction.SOUTH);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var at=origin(Direction.SOUTH);p.teleportTo(p.level(),at.x,132,at.z,Set.of(),0,0,true);});c.waitFor(v->v.player.getY()>131.7);c.getInput().holdKey(ClimbControls.CLIMB);c.getInput().holdKey(v->v.keyUp);c.waitFor(v->v.player.getY()>=134&&v.player.getZ()>5.2&&v.player.onGround(),100);check(c.computeOnClient(v->frame(v)==null),"Actual mantle stays native");c.takeScreenshot("climb-rig-native-mantle");release(c);
            reset(c,w,Direction.SOUTH);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();PlayerStamina.consume(p,PlayerStamina.get(p).stamina()-.8);});c.getInput().holdKey(ClimbControls.CLIMB);c.getInput().holdKey(v->v.keyUp);c.waitFor(v->v.player.getAttached(Climbing.VIEW).blocked());check(c.computeOnClient(v->frame(v)==null),"Stamina exhaustion removes source frame");release(c);
            reset(c,w,Direction.SOUTH);c.getInput().holdKey(ClimbControls.CLIMB);c.waitFor(v->frame(v)!=null);w.getServer().runCommand("damage @p 1 minecraft:generic");c.waitFor(v->!Climbing.active(v.player));check(c.computeOnClient(v->frame(v)==null),"Native damage cancels visual grip");release(c);c.waitTicks(15);
            reset(c,w,Direction.SOUTH);c.getInput().holdKey(ClimbControls.CLIMB);c.waitFor(v->frame(v)!=null);w.getServer().runCommand("kill @p");c.waitFor(v->!v.player.isAlive());check(c.computeOnClient(v->frame(v)==null),"Death clears source pose");release(c);c.runOnClient(v->v.player.respawn());c.waitFor(v->v.player.isAlive());
            reset(c,w,Direction.SOUTH);c.getInput().holdKey(ClimbControls.CLIMB);c.waitFor(v->frame(v)!=null);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.getLevel(Level.NETHER),0,120,0,Set.of(),0,0,true);});c.waitFor(v->v.level.dimension()==Level.NETHER);check(c.computeOnClient(v->frame(v)==null),"Dimension transition clears source pose");release(c);
            System.out.println("C6G actual G, wide/slim GPU, four wall directions, distance strides, planar contacts/skinned surfaces, A/D/idle/head, native descent/armor/held items, GUI/pause/attack/ceiling/corner/mantle/exhaustion/damage/death/dimension PASS");
        }finally{release(c);c.runOnClient(v->{RigAudit.enabled=false;RigAudit.fixture=null;RigAudit.bodyYaw=null;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
