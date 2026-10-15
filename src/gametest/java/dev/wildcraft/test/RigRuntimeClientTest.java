package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.WildcraftBindings;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Real inputs, authority, native skins/items and GPU; source preview timing never changes combat rules. */
public final class RigRuntimeClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var skin=DefaultPlayerSkin.get(new UUID(0,i));if(skin.model()==type)return skin;}throw new AssertionError("No native skin fixture");}
    @Override public void runTest(ClientGameTestContext c){
        var target=new AtomicInteger();var opts=CombatOptions.get();boolean hold=opts.hold;int follow=opts.follow;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());p.teleportTo(.5,120,.5);p.setYRot(0);p.setXRot(0);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.inventoryMenu.broadcastChanges();var mob=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);mob.setPos(.5,120,7.5);mob.setNoAi(true);mob.setNoGravity(true);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);mob.setHealth(200);s.overworld().addFreshEntity(mob);target.set(mob.getId());});
            c.waitFor(v->v.player.getMainHandItem().is(Items.IRON_SWORD)&&v.player.getY()>119&&v.level.getEntity(target.get())!=null);
            c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.hold=false;opts.follow=0;v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);v.options.guiScale().set(2);v.player.setYRot(0);v.player.setXRot(0);RigAudit.begin();});
            c.waitFor(v->dev.wildcraft.client.input.ActionInput.tickFrame().gameplay());c.waitTicks(3);c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(v->Combat.locked(v.player));
            c.waitFor(v->RigAudit.bodies>0&&RigAudit.last.weight()>.99);
            if(Boolean.getBoolean("wildcraft.test.rigRuntimeJumpProbe")){c.waitFor(v->v.player.onGround());c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(v->!v.player.onGround());check(c.computeOnClient(v->{var f=state(v).getData(CombatRigLayer.FRAME);return f==null||f.clip().startsWith("jump_takeoff_")||f.clip().startsWith("jump_air_");}),"Actual airborne player retains native movement or qualified ordinary jump source, never a stationary grounded combat pose");return;}
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var hand:HumanoidArm.values())for(boolean sleeves:new boolean[]{true,false}){
                w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setMainArm(hand));c.waitFor(v->v.player.getMainArm()==hand);
                c.runOnClient(v->{RigAudit.fixture=skin(type);RigAudit.sleeves=sleeves;RigAudit.begin();});c.waitFor(v->RigAudit.bodies>0&&RigAudit.last.weight()>.99&&RigAudit.hands>0);
                check(c.computeOnClient(v->RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())&&RigAudit.last.data().variant().equals(type==PlayerModelType.SLIM?"slim":"standard")),"Real native fixture bound to correct GPU mesh");
                c.takeScreenshot("rig-runtime-idle-"+type+"-"+hand+"-sleeves-"+sleeves);
                c.runOnClient(v->{v.player.setXRot(-30);v.player.setYRot(30);});c.waitTicks(3);
                c.runOnClient(v->{var a=state(v);var f=a.getData(CombatRigLayer.FRAME);var rotation=f.global()[3].getUnnormalizedRotation(new org.joml.Quaternionf());check(Math.abs(rotation.x)>.1||Math.abs(rotation.y)>.1,"Independent head aim reaches continuous skin");});
                c.takeScreenshot("rig-runtime-free-head-"+type+"-"+hand+"-"+sleeves);c.runOnClient(v->{v.player.setXRot(0);v.player.setYRot(0);});c.waitTicks(25);
                w.getServer().runOnServer(s->{var mob=(LivingEntity)s.overworld().getEntity(target.get());CounterAttackAudit.begin(s.getPlayerList().getPlayers().getFirst());mob.setPos(.5,120,2.7);mob.setHealth(200);mob.setInvulnerableTime(0);mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);});c.waitTicks(3);
                c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_BACK));
                int wear=c.computeOnClient(v->v.player.getMainHandItem().getDamageValue());
                c.runOnClient(v->{RigAudit.begin();v.options.keyAttack.setDown(true);dev.wildcraft.client.input.ActionInput.frame(v);v.options.keyAttack.setDown(false);dev.wildcraft.client.input.ActionInput.frame(v);});
                c.waitFor(v->RigAudit.last!=null&&RigAudit.last.clip().equals("A_"+(hand==HumanoidArm.RIGHT?"R":"L")),60);c.takeScreenshot("rig-runtime-real-single-hit-"+type+"-"+hand+"-"+sleeves);
                c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);c.waitTicks(8);
                check(c.computeOnClient(v->v.player.getMainHandItem().getDamageValue()==wear+1),"One native input owns one sword wear, source animation adds no hit");
                w.getServer().runOnServer(s->{var mob=(LivingEntity)s.overworld().getEntity(target.get());check(CounterAttackAudit.calls==1&&mob.getHealth()<200,"One actual native attack; source clips add no extra attack");mob.setPos(.5,120,7.5);mob.setHealth(200);mob.setInvulnerableTime(0);mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);});
                c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            }
            c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);float sample=c.computeOnClient(v->state(v).getData(CombatRigLayer.FRAME).time());long until=System.nanoTime()+450_000_000L;c.waitFor(v->System.nanoTime()>until);check(c.computeOnClient(v->Math.abs(state(v).getData(CombatRigLayer.FRAME).time()-sample)<.0001),"Pause freezes source animation clock");c.takeScreenshot("rig-runtime-pause");c.setScreen(()->null);
            c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));c.waitTicks(3);check(c.computeOnClient(v->state(v).getData(CombatRigLayer.FRAME)==null),"GUI cancels continuous combat pose");c.setScreen(()->null);c.waitTicks(12);
            for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                Item armor=switch(slot){case HEAD->Items.IRON_HELMET;case CHEST->Items.IRON_CHESTPLATE;case LEGS->Items.IRON_LEGGINGS;default->Items.IRON_BOOTS;};
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(slot,new ItemStack(armor));p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemBySlot(slot).is(armor));c.waitTicks(2);check(c.computeOnClient(v->state(v).getData(CombatRigLayer.FRAME)==null),"Unqualified armor retains native body");c.takeScreenshot("rig-runtime-native-armor-"+slot);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(slot,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemBySlot(slot).isEmpty());
            }
            for(var other:new Item[]{Items.TORCH,Items.SHIELD}){
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(other));p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getOffhandItem().is(other));check(c.computeOnClient(v->state(v).getData(CombatRigLayer.FRAME)==null),"Occupied hand retains both native owned items");c.takeScreenshot("rig-runtime-native-occupied-"+other.toString().replace(':','-'));
            }
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getOffhandItem().isEmpty());c.waitTicks(12);
            c.getInput().holdKey(v->v.keyUp);c.waitFor(v->v.player.getDeltaMovement().horizontalDistanceSqr()>.0004);check(c.computeOnClient(v->{var moving=state(v).getData(CombatRigLayer.FRAME);return moving==null||moving.clip().startsWith("walk_");}),"Moving retains native gait or the qualified C6I walk, never a stationary combat pose");c.getInput().releaseKey(v->v.keyUp);c.waitTicks(12);
            c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(v->!v.player.onGround());check(c.computeOnClient(v->{var f=state(v).getData(CombatRigLayer.FRAME);return f==null||f.clip().startsWith("jump_takeoff_")||f.clip().startsWith("jump_air_");}),"Actual airborne player retains native movement or qualified ordinary jump source, never a stationary grounded combat pose");c.waitFor(v->v.player.onGround());
            System.out.println("CONTINUOUS RIG production locked idle + real normal sword release, wide/slim left/right native skins/sleeves/head, one wear, pause/GUI/armor/occupied/movement/air fallback PASS");
        }finally{RigAudit.enabled=false;RigAudit.fixture=null;c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUp);c.runOnClient(v->{opts.hold=hold;opts.follow=follow;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
