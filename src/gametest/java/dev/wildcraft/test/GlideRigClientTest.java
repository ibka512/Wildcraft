package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.client.render.ParagliderLayer;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Matrix4f;

/** Real equipped glider, jump, steering and item takeover; source canopy is never a dummy item. */
public final class GlideRigClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static CombatRigLayer.Frame frame(Minecraft c){return state(c).getData(CombatRigLayer.FRAME);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin fixture");}
    private static void release(ClientGameTestContext c){c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyUp);}
    private static void jump(ClientGameTestContext c){c.getInput().releaseKey(v->v.keyJump);c.waitTicks(1);c.getInput().holdKeyFor(v->v.keyJump,1);c.waitTicks(2);}
    private static void reset(ClientGameTestContext c,TestSingleplayerContext w){
        release(c);c.waitTicks(2);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();Gliding.reset(p,false);PlayerStamina.fill(p);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.setHealth(p.getMaxHealth());p.hurtTime=0;p.getAbilities().flying=p.getAbilities().mayfly=false;p.onUpdateAbilities();p.setNoGravity(false);p.setDeltaMovement(Vec3.ZERO);GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));p.teleportTo(p.level(),.5,250,.5,Set.of(),0,0,true);p.inventoryMenu.broadcastChanges();});
        c.waitFor(v->v.player.getY()>235&&GliderEquipment.equipped(v.player)&&!v.player.onGround());c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);RigAudit.begin();GlideRigAudit.begin();v.player.getInventory().setSelectedSlot(0);});c.waitFor(v->ActionInput.tickFrame().gameplay());c.waitTicks(2);jump(c);c.waitFor(v->Gliding.active(v.player));
    }
    private static void grip(Minecraft c){
        var s=state(c);var f=s.getData(CombatRigLayer.FRAME);var canopy=s.getData(GlideRig.CANOPY);check(f!=null&&f.clip().equals("glide_hold")&&canopy!=null,"Actual selected hold and canopy");
        var names=Arrays.asList(f.data().bones());
        for(String side:new String[]{"left","right"}){
            var palm=f.global()[names.indexOf(side+"_palm_grip")].transformPosition(new Vector3f(0,.0625F,0));
            var bar=new Matrix4f(CombatRigLayer.BASIS).mul(canopy.matrix()).transformPosition(new Vector3f(side.equals("left")?.46875F:-.46875F,-.4375F,-.4375F));
            check(palm.distance(bar)<.003,"Actual adopted palm and retained original canopy grip share bank matrix: "+side+" error="+palm.distance(bar));
        }
        check(f.global()[0].getTranslation(new Vector3f()).distance(CombatRig.matrix(f.data().bindWorld()[0]).getTranslation(new Vector3f()))<.0001,"Visual bank adds no source root displacement");
    }
    @Override public void runTest(ClientGameTestContext c){
        GlideViewSafety.begin();
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runOnServer(s->{for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());});c.getInput().resizeWindow(1280,960);
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var hand:HumanoidArm.values()){
                reset(c,w);w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setMainArm(hand));c.waitFor(v->v.player.getMainArm()==hand);
                c.runOnClient(v->{RigAudit.fixture=skin(type);v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("glide_hold")&&GlideRigAudit.worldCanopies>0&&RigAudit.bodies>0);c.runOnClient(GlideRigClientTest::grip);c.takeScreenshot("glide-rig-neutral-"+type+"-"+hand);
                for(int direction:new int[]{1,-1,0}){
                    release(c);if(direction!=0)c.getInput().holdKey(v->direction>0?v.keyLeft:v.keyRight);
                    c.waitFor(v->{var a=state(v).getData(GlideRig.CANOPY);return a!=null&&java.lang.Math.abs(a.bank()-direction*8)<.1;},60);c.runOnClient(GlideRigClientTest::grip);c.takeScreenshot("glide-rig-live-bank-"+direction+"-"+type+"-"+hand);
                    c.runOnClient(v->{check(java.lang.Math.abs(v.player.getYRot())<.01&&java.lang.Math.abs(v.player.getXRot())<.01,"Visual bank never turns/rolls camera");v.options.setCameraType(CameraType.FIRST_PERSON);GlideRigAudit.begin();});c.waitFor(v->GlideRigAudit.viewArms>0&&GlideRigAudit.viewCanopies>0);check(c.computeOnClient(v->GlideRigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Real first-person native skin matches source variant");
                    for(int[] size:new int[][]{{1280,960},{1728,590},{720,960}})for(int fov:new int[]{70,90,110}){
                        c.getInput().resizeWindow(size[0],size[1]);c.runOnClient(v->v.options.fov().set(fov));c.waitTicks(2);c.runOnClient(GlideRigClientTest::grip);c.runOnClient(GlideViewSafety::check);c.takeScreenshot("glide-rig-view-"+type+"-"+hand+"-bank"+direction+"-"+size[0]+"x"+size[1]+"-fov"+fov);
                    }
                    c.getInput().resizeWindow(1280,960);c.runOnClient(v->{v.options.fov().set(70);v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});
                }
                release(c);jump(c);c.waitFor(v->!Gliding.active(v.player));
                check(c.computeOnClient(v->state(v).getData(GlideRig.CANOPY)==null),"Real jump closure hides canopy immediately");c.takeScreenshot("glide-rig-jump-close-"+type+"-"+hand);c.waitTicks(5);check(c.computeOnClient(v->frame(v)==null),"Real jump closes source recovery within .20s");
                reset(c,w);c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("glide_hold"));
                release(c);c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);var paused=c.computeOnClient(GlideRigClientTest::frame);long until=System.nanoTime()+350_000_000L;c.waitFor(v->System.nanoTime()>until);c.runOnClient(v->{var f=frame(v);if(f!=null&&f.clip().equals(paused.clip()))check(java.lang.Math.abs(f.time()-paused.time())<.0001,"Pause freezes surviving same source clip");else check(!Gliding.active(v.player)&&state(v).getData(GlideRig.CANOPY)==null,"Genuine pause input cancellation hides canopy immediately");});c.takeScreenshot("glide-rig-pause-"+type+"-"+hand);c.setScreen(()->null);
                release(c);c.waitTicks(3);if(c.computeOnClient(v->Gliding.active(v.player)))jump(c);c.waitFor(v->!Gliding.active(v.player));check(c.computeOnClient(v->state(v).getData(GlideRig.CANOPY)==null&&!state(v).getDataOrDefault(ParagliderLayer.OPEN,false)),"Canopy closes immediately without waiting source exit");c.takeScreenshot("glide-rig-real-close-"+type+"-"+hand);c.waitTicks(5);check(c.computeOnClient(v->frame(v)==null),"Empty-hand source exit relinquishes renderer in .20s");
            }
            reset(c,w);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));p.inventoryMenu.broadcastChanges();});c.waitFor(v->!Gliding.active(v.player));c.waitFor(v->v.player.getAttached(Gliding.VIEW)!=null&&!v.player.getAttached(Gliding.VIEW).blocked());c.waitTicks(2);
            jump(c);c.waitFor(v->Gliding.active(v.player));c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("glide_hold"));c.runOnClient(v->{var s=state(v);check(s.rightHandItemState.isEmpty()&&s.leftHandItemState.isEmpty(),"Open rig owns both visual hands");check(v.player.getMainHandItem().is(Items.IRON_SWORD)&&v.player.getOffhandItem().is(Items.SHIELD),"Real holstered stacks are retained");});c.takeScreenshot("glide-rig-real-sword-shield-holstered");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(1,new ItemStack(Items.TORCH));p.inventoryMenu.broadcastChanges();});c.getInput().holdKeyFor(v->v.keyHotbarSlots[1],1);c.waitFor(v->!Gliding.active(v.player)&&v.player.getMainHandItem().is(Items.TORCH));check(c.computeOnClient(v->frame(v)==null&&state(v).getData(GlideRig.CANOPY)==null),"Taking item immediately relinquishes source arms and canopy");c.takeScreenshot("glide-rig-real-item-takeover");
            reset(c,w);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE));check(c.computeOnClient(v->frame(v)==null&&state(v).getData(GlideRig.CANOPY)==null),"Unqualified armor keeps native glider hands/body");c.takeScreenshot("glide-rig-native-armor-fallback");
            System.out.println("C6H real equipped jump/close, wide/slim left/right actual native skins and canopy grips, live left/right/neutral bank8 with root/camera retained, first-person aspect/FOV matrix, pause/holstered inventory/item handover/armor PASS");
        }finally{GlideViewSafety.save();release(c);c.runOnClient(v->{RigAudit.enabled=false;RigAudit.fixture=null;GlideRigAudit.enabled=false;v.options.fov().set(70);v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
