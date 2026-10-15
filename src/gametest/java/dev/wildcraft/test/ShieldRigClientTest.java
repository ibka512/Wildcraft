package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.*;
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual shield ownership/blocking and native item model follow the approved continuous guard clips. */
public final class ShieldRigClientTest implements FabricClientGameTest {
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin fixture missing");}
    @Override public void runTest(ClientGameTestContext c){
        var id=new AtomicInteger();var options=CombatOptions.get();boolean hold=options.hold;int follow=options.follow;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());p.teleportTo(.5,120,.5);p.setYRot(0);p.setXRot(0);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));p.inventoryMenu.broadcastChanges();var mob=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);mob.setPos(.5,120,7.5);mob.setNoAi(true);mob.setNoGravity(true);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);mob.setHealth(200);s.overworld().addFreshEntity(mob);id.set(mob.getId());});
            c.waitFor(v->v.player.getOffhandItem().is(Items.SHIELD)&&v.player.getY()>119&&v.level.getEntity(id.get())!=null);c.getInput().resizeWindow(1280,960);
            c.runOnClient(v->{options.hold=false;options.follow=0;v.player.setYRot(0);v.player.setXRot(0);v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);v.options.guiScale().set(2);RigAudit.begin();});c.waitFor(v->ActionInput.tickFrame().gameplay());c.waitTicks(3);c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(v->Combat.locked(v.player));
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var hand:HumanoidArm.values()){
                String side=hand==HumanoidArm.RIGHT?"R":"L";
                w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setMainArm(hand));c.waitFor(v->v.player.getMainArm()==hand);c.runOnClient(v->{RigAudit.fixture=skin(type);RigAudit.sleeves=true;RigAudit.begin();});
                c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->v.player.isUsingItem()&&Combat.view(v.player).defending());c.waitFor(v->RigAudit.last!=null&&RigAudit.last.clip().equals("hold_"+side)&&RigAudit.last.weight()>.99&&RigAudit.shields>0);
                check(c.computeOnClient(v->RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Native player texture is retained");c.takeScreenshot("shield-rig-actual-hold-"+type+"-"+hand);
                c.runOnClient(v->{v.player.setYRot(35);v.player.setXRot(-20);});c.waitTicks(3);
                c.runOnClient(v->{var frame=state(v).getData(CombatRigLayer.FRAME);int bone=Arrays.asList(frame.data().bones()).indexOf(hand==HumanoidArm.RIGHT?"left_shield_mount":"right_shield_mount");var front=frame.global()[bone].transformDirection(new org.joml.Vector3f(0,0,-1)).normalize();check(front.z>.9,"Approved guard plate faces body target while head aim remains free: "+front);});c.takeScreenshot("shield-rig-free-head-"+type+"-"+hand);
                c.waitTicks(8);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();float health=p.getHealth();int wear=p.getOffhandItem().getDamageValue();p.hurtServer(s.overworld(),p.damageSources().mobAttack((LivingEntity)s.overworld().getEntity(id.get())),6);check(p.getHealth()==health&&p.getOffhandItem().getDamageValue()>wear,"Actual ordinary native shield blocks and wears; visual source owns no damage rule");});c.waitTicks(3);
                c.runOnClient(v->{RigAudit.begin();v.player.setYRot(0);v.player.setXRot(0);});c.getInput().releaseKey(WildcraftBindings.DEFEND);c.waitFor(v->!v.player.isUsingItem());c.waitFor(v->RigAudit.last!=null&&RigAudit.last.clip().equals("lower_"+side));c.takeScreenshot("shield-rig-actual-lower-"+type+"-"+hand);c.waitTicks(8);check(c.computeOnClient(v->state(v).getData(CombatRigLayer.FRAME)==null),"Unraised idle returns to native shield carry");
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CounterAttackAudit.begin(p);var mob=(LivingEntity)s.overworld().getEntity(id.get());mob.setPos(.5,120,2.7);mob.setHealth(200);mob.setInvulnerableTime(0);mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);});c.waitTicks(25);
                int wear=c.computeOnClient(v->v.player.getMainHandItem().getDamageValue());int shieldWear=c.computeOnClient(v->v.player.getOffhandItem().getDamageValue());
                c.runOnClient(v->{RigAudit.begin();v.options.setCameraType(CameraType.THIRD_PERSON_BACK);v.options.keyAttack.setDown(true);ActionInput.frame(v);v.options.keyAttack.setDown(false);ActionInput.frame(v);});c.waitFor(v->RigAudit.last!=null&&RigAudit.last.clip().equals("shield_A_"+side)&&RigAudit.shields>0);c.takeScreenshot("shield-rig-real-single-sword-"+type+"-"+hand);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);c.waitTicks(5);
                check(c.computeOnClient(v->v.player.getMainHandItem().getDamageValue()==wear+1&&v.player.getOffhandItem().getDamageValue()==shieldWear),"One sword wear; held shield gains no fake attack wear");w.getServer().runOnServer(s->{check(CounterAttackAudit.calls==1,"One actual native sword attack");var mob=(LivingEntity)s.overworld().getEntity(id.get());check(mob.getHealth()<200,"Native attack hit actual target");mob.setPos(.5,120,7.5);mob.setHealth(200);mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);});c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            }
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var shield=new ItemStack(Items.SHIELD);shield.set(net.minecraft.core.component.DataComponents.BASE_COLOR,net.minecraft.world.item.DyeColor.BLUE);shield.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);check(dev.wildcraft.fuse.FusionItems.attach(shield,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Actual shield Fuse fixture attaches");p.setItemInHand(InteractionHand.OFF_HAND,shield);p.inventoryMenu.broadcastChanges();});
            c.waitFor(v->Boolean.TRUE.equals(v.player.getOffhandItem().get(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE))&&v.player.getOffhandItem().has(dev.wildcraft.fuse.FusionContent.DATA));c.runOnClient(v->RigAudit.begin());c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->RigAudit.last!=null&&RigAudit.last.clip().startsWith("hold_")&&RigAudit.shields>0);c.takeScreenshot("shield-rig-blue-foil-fused-native-model");c.getInput().releaseKey(WildcraftBindings.DEFEND);c.waitFor(v->!v.player.isUsingItem());c.waitTicks(8);
            c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->RigAudit.last!=null&&RigAudit.last.clip().startsWith("hold_"));c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);var pausedFrame=c.computeOnClient(v->state(v).getData(CombatRigLayer.FRAME));long until=System.nanoTime()+450_000_000L;c.waitFor(v->System.nanoTime()>until);check(c.computeOnClient(v->{var now=state(v).getData(CombatRigLayer.FRAME);System.out.println("SHIELD PAUSE before="+(pausedFrame==null?"native":pausedFrame.clip()+":"+pausedFrame.time())+" after="+(now==null?"native":now.clip()+":"+now.time())+" using="+v.player.isUsingItem()+" locked="+Combat.locked(v.player));
                if(pausedFrame!=null&&now!=null&&now.clip().equals(pausedFrame.clip()))return Math.abs(now.time()-pausedFrame.time())<.0001;
                return !v.player.isUsingItem()||!Combat.locked(v.player);}),"Pause freezes a legal source frame or preserves native input cancellation; never revive a cleared guard");c.takeScreenshot("shield-rig-pause");c.setScreen(()->null);
            c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));c.waitTicks(3);check(c.computeOnClient(v->state(v).getData(CombatRigLayer.FRAME)==null),"GUI cancels continuous guard");c.getInput().releaseKey(WildcraftBindings.DEFEND);c.setScreen(()->null);c.waitTicks(10);
            System.out.println("SHIELD RIG actual locked raise/hold/lower, wide/slim left/right native shields/skins, independent head/front plate, native block/wear + single sword hit, pause/GUI/restoration PASS");
        }finally{RigAudit.enabled=false;RigAudit.fixture=null;c.getInput().releaseKey(WildcraftBindings.DEFEND);c.getInput().releaseKey(v->v.keyAttack);c.runOnClient(v->{options.hold=hold;options.follow=follow;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
