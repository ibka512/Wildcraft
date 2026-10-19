package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.FirstPersonSword;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.PlayerStamina;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Real accepted attack + actual native queues and deferred GPU skin, not a preview-player pass. */
public final class FirstPersonSwordClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Missing native skin");}
    private static void tap(ClientGameTestContext c){c.runOnClient(v->{v.options.keyAttack.setDown(true);ActionInput.frame(v);v.options.keyAttack.setDown(false);ActionInput.frame(v);});}
    private static void equip(TestSingleplayerContext w,int target,Item other,HumanoidArm arm){
        w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();MeleeActions.reset(p);p.stopUsingItem();p.setMainArm(arm);p.teleportTo(.5,120,.5);p.setYRot(0);p.setXRot(0);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);var sword=new ItemStack(Items.IRON_SWORD);FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),s.registryAccess());p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setItemInHand(InteractionHand.OFF_HAND,other==null?ItemStack.EMPTY:new ItemStack(other));PlayerStamina.fill(p);p.inventoryMenu.broadcastChanges();var m=(LivingEntity)s.overworld().getEntity(target);m.setHealth(200);m.setInvulnerableTime(0);m.setPos(.5,120,3.2);m.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);CounterAttackAudit.begin(p);});
    }
    @Override public void runTest(ClientGameTestContext c){
        var target=new AtomicInteger();var opts=CombatOptions.get();boolean hold=opts.hold;int follow=opts.follow;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{for(int x=-5;x<=5;x++)for(int z=-4;z<=9;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.teleportTo(.5,120,.5);var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,3.2);m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);m.setHealth(200);s.overworld().addFreshEntity(m);target.set(m.getId());});
            c.waitFor(v->v.player.getY()>119&&ActionInput.tickFrame().gameplay());c.getInput().resizeWindow(1280,960);
            c.runOnClient(v->{opts.hold=false;opts.follow=0;v.options.setCameraType(CameraType.FIRST_PERSON);v.player.setYRot(0);v.player.setXRot(0);RigAudit.enabled=true;});
            int cases=0;
            if(!Boolean.getBoolean("wildcraft.test.firstPersonSwordClockOnly")){
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values())for(boolean shield:new boolean[]{false,true})for(boolean sleeve:new boolean[]{false,true}){
                equip(w,target.get(),shield?Items.SHIELD:null,arm);c.waitFor(v->v.player.getMainArm()==arm&&v.player.getMainHandItem().is(Items.IRON_SWORD)&&v.player.getOffhandItem().isEmpty()!=shield);c.waitTicks(35);
                c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);RigAudit.fixture=skin(type);RigAudit.sleeves=sleeve;FirstPersonSwordAudit.begin();});
                tap(c);c.waitFor(v->FirstPersonSwordAudit.skinGpu>0&&FirstPersonSwordAudit.swordQuads>0,60);
                check(c.computeOnClient(v->FirstPersonSwordAudit.clip.equals((shield?"shield_":"")+"A_"+(arm==HumanoidArm.RIGHT?"R":"L"))&&FirstPersonSwordAudit.variant.equals(type==PlayerModelType.SLIM?"slim":"standard")&&FirstPersonSwordAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Actual native skin, source variant and dominant hand");
                if(shield)c.waitFor(v->FirstPersonSwordAudit.shieldItems>0&&FirstPersonSwordAudit.shieldGpu>0,20);
                c.takeScreenshot("first-person-sword-"+type+"-"+arm+"-shield-"+shield+"-sleeve-"+sleeve);
                c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);c.waitTicks(3);
                check(c.computeOnClient(v->v.player.getMainHandItem().getDamageValue()==1&&v.player.getMainHandItem().get(FusionContent.VIEW).uses()==31),"Exactly one native sword wear/public Fuse visual charge");
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==1&&PlayerStamina.get(p).stamina()==100&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"One native attack, no extra stamina transaction");});
                System.out.println("C6AO_MATRIX "+type+" "+arm+" shield="+shield+" sleeves="+sleeve+" frames="+FirstPersonSwordAudit.arms+" skinGPU="+FirstPersonSwordAudit.skinGpu+" nativeQuads="+FirstPersonSwordAudit.swordQuads+" palm="+FirstPersonSwordAudit.maxPalmGap+" quad="+FirstPersonSwordAudit.maxQuadGap);cases++;
                c.runOnClient(v->FirstPersonSwordAudit.begin());c.waitTicks(2);check(c.computeOnClient(v->FirstPersonSwordAudit.arms==0),"No source arm lease after native recovery");
            }
            // Camera-space arms also follow the native moving/crouched camera; no full-body source retargeting.
            for(boolean crouch:new boolean[]{false,true}){
                equip(w,target.get(),null,HumanoidArm.RIGHT);c.waitTicks(35);c.runOnClient(v->FirstPersonSwordAudit.begin());
                if(crouch)c.getInput().holdKey(v->v.keyShift);c.getInput().holdKey(v->v.keyUp);c.waitFor(v->v.player.getDeltaMovement().horizontalDistanceSqr()>.0004&&(!crouch||v.player.isCrouching()));
                tap(c);c.waitFor(v->FirstPersonSwordAudit.movingArms>0&&FirstPersonSwordAudit.swordQuads>0,40);
                if(crouch)check(c.computeOnClient(v->FirstPersonSwordAudit.crouchingArms>0),"Real crouch camera retains source-native grip");
                c.takeScreenshot("first-person-sword-native-moving-crouch-"+crouch);c.getInput().releaseKey(v->v.keyUp);c.getInput().releaseKey(v->v.keyShift);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);c.waitTicks(8);
            }
            // Occupied unrelated hand and strong attacks retain the previous presentation.
            equip(w,target.get(),Items.TORCH,HumanoidArm.RIGHT);c.waitTicks(35);c.runOnClient(v->FirstPersonSwordAudit.begin());tap(c);c.waitTicks(8);check(c.computeOnClient(v->FirstPersonSwordAudit.arms==0&&v.player.getOffhandItem().is(Items.TORCH)),"Torch ownership/fallback");
            equip(w,target.get(),null,HumanoidArm.RIGHT);c.waitTicks(35);c.runOnClient(v->FirstPersonSwordAudit.begin());c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.CHARGE&&MeleeActions.view(v.player).age()>=12);c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(13);check(c.computeOnClient(v->FirstPersonSwordAudit.arms==0),"Charged sword retains old path");
            }
            // Slow native ticks give a reproducible pause/GUI exit while the actual swing remains active.
            equip(w,target.get(),null,HumanoidArm.RIGHT);c.waitTicks(35);w.getServer().runCommand("tick rate 5");c.runOnClient(v->{v.options.framerateLimit().set(10);FirstPersonSwordAudit.begin();});tap(c);c.waitFor(v->FirstPersonSwordAudit.arms>0,60);
            c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);check(c.computeOnClient(v->MeleeActions.view(v.player).phase()==MeleeView.SWING),"Native swing still active while paused");c.waitTicks(2);
            float expected=c.computeOnClient(v->.65F+1.4F*Math.clamp((FirstPersonSwordAudit.lastAge+FirstPersonSwordAudit.lastPartial)/Math.max(1,FirstPersonSwordAudit.lastDuration),0,1));
            float frozen=c.computeOnClient(v->FirstPersonSwordAudit.lastTime);
            System.out.println("C6AO_NATIVE_PAUSE source="+frozen+" expected="+expected+" partial="+FirstPersonSwordAudit.lastPartial);
            c.takeScreenshot("first-person-sword-pause-native-fraction");check(Math.abs(frozen-expected)<.00001F,"Source preserves native frozen interpolation fraction");long until=System.nanoTime()+400_000_000L;c.waitFor(v->System.nanoTime()>until);
            check(c.computeOnClient(v->Math.abs(FirstPersonSwordAudit.lastTime-frozen)<.0001F),"Pause does not advance source age");c.takeScreenshot("first-person-sword-native-5tps-10fps-pause");
            c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));c.runOnClient(v->FirstPersonSwordAudit.begin());c.waitTicks(3);check(c.computeOnClient(v->FirstPersonSwordAudit.arms==0),"GUI cancels source without queued hit");c.setScreen(()->null);w.getServer().runCommand("tick rate 20");c.runOnClient(v->v.options.framerateLimit().set(120));
            System.out.println("C6AO_COMPLETE matrix="+cases+" source=8 nativeAuthority=true");
        }finally{c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUp);c.getInput().releaseKey(v->v.keyShift);c.runOnClient(v->{FirstPersonSwordAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;opts.hold=hold;opts.follow=follow;v.options.framerateLimit().set(120);v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
