package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual jump/input/source GPU and one native hit; no artificial airborne player or melee phase. */
public final class AirMeleeRigClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static CombatRigLayer.Frame frame(Minecraft c){return ((AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1)).getData(CombatRigLayer.FRAME);}
    private static boolean active(Minecraft c){var f=frame(c);return f!=null&&f.clip().contains("_air_attack_");}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin fixture");}
    private static void setup(ClientGameTestContext c,TestSingleplayerContext w,int mob,Item item,ItemStack off,HumanoidArm hand,PlayerModelType type){
        c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyJump);c.waitTicks(2);
        w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.setMainArm(hand);var stack=new ItemStack(item);check(FusionItems.attach(stack,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Actual Fuse fixture");p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setItemInHand(InteractionHand.OFF_HAND,off.copy());p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.setHealth(20);p.setInvulnerableTime(0);PlayerStamina.fill(p);p.inventoryMenu.broadcastChanges();var target=(LivingEntity)s.overworld().getEntity(mob);target.setPos(.5,121,2.7);target.setHealth(200);target.setInvulnerableTime(0);target.setDeltaMovement(Vec3.ZERO);CounterAttackAudit.begin(p);});
        c.getInput().pressKey(v->v.keyHotbarSlots[0]);
        c.waitFor(v->v.player.onGround()&&v.player.getMainHandItem().is(item)&&v.player.getMainArm()==hand&&MeleeActions.view(v.player).phase()==MeleeView.IDLE);
        c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);RigAudit.fixture=skin(type);RigAudit.begin();AirMeleeAudit.begin();v.options.setCameraType(CameraType.THIRD_PERSON_BACK);});c.waitFor(v->ActionInput.tickFrame().gameplay());c.waitTicks(25);
        c.runOnClient(v->{RigAudit.begin();AirMeleeAudit.begin();});
    }
    private static void jumpAttack(ClientGameTestContext c){
        c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround()&&v.player.getDeltaMovement().y>0);c.getInput().releaseKey(v->v.keyJump);
        c.runOnClient(v->{v.options.keyAttack.setDown(true);ActionInput.frame(v);v.options.keyAttack.setDown(false);ActionInput.frame(v);});
    }
    @Override public void runTest(ClientGameTestContext c){
        var id=new AtomicInteger();var options=CombatOptions.get();int oldFollow=options.follow;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,121,2.7);m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);m.setHealth(200);s.overworld().addFreshEntity(m);id.set(m.getId());});
            c.waitFor(v->v.level.getEntity(id.get())!=null);c.getInput().resizeWindow(1280,960);c.runOnClient(v->{options.follow=0;v.options.guiScale().set(2);});
            for(var item:new Item[]{Items.IRON_SWORD,Items.IRON_AXE})for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var hand:HumanoidArm.values())for(boolean carry:new boolean[]{false,true}){
                setup(c,w,id.get(),item,carry?new ItemStack(Items.SHIELD):ItemStack.EMPTY,hand,type);jumpAttack(c);
                String name=(item==Items.IRON_SWORD?"sword":"axe")+"_air_attack_"+(carry?"carry":"empty")+"_"+(hand==HumanoidArm.RIGHT?"R":"L");
                c.waitFor(v->AirMeleeAudit.full>0&&AirMeleeAudit.clip.equals(name));
                check(c.computeOnClient(v->RigAudit.hands>0&&(!carry||RigAudit.shields>0)&&RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Actual native skin and item GPU");
                c.takeScreenshot("air-melee-"+type+"-"+name);
                c.waitFor(v->v.player.onGround()&&MeleeActions.view(v.player).phase()==MeleeView.IDLE);check(c.computeOnClient(v->!active(v)),"Actual landing/recovery relinquishes air body");c.waitTicks(3);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var target=(LivingEntity)s.overworld().getEntity(id.get());check(CounterAttackAudit.calls==1&&target.getHealth()<200,"One actual native hit");check(p.getMainHandItem().getDamageValue()==(item==Items.IRON_AXE?2:1)&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"Original one wear/Fuse settlement");check(PlayerStamina.get(p).stamina()==100,"Ordinary air strike stays free");check(!p.isUsingItem()&&p.getOffhandItem().getDamageValue()==0,"Carried shield gets no imaginary block or wear");check(Math.abs(p.getY()-120)<1e-5&&p.getHealth()==20,"Native jump/gravity and health preserved y="+p.getY()+" health="+p.getHealth()+" ground="+p.onGround());});
            }
            setup(c,w,id.get(),Items.IRON_AXE,new ItemStack(Items.TORCH),HumanoidArm.RIGHT,PlayerModelType.WIDE);jumpAttack(c);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.AIR_SWING);check(c.computeOnClient(v->!active(v)),"Actual occupied other hand retains native path");c.takeScreenshot("air-melee-native-occupied");c.waitFor(v->v.player.onGround());
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.inventoryMenu.broadcastChanges();});c.waitFor(v->!v.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty());jumpAttack(c);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.AIR_SWING);check(c.computeOnClient(v->!active(v)),"Actual armor keeps prior masks");c.takeScreenshot("air-melee-native-armor");c.waitFor(v->v.player.onGround());
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);jumpAttack(c);c.waitFor(v->AirMeleeAudit.full>0);c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));check(c.computeOnClient(v->!active(v)),"Inventory owns hands immediately");c.takeScreenshot("air-melee-gui");c.setScreen(()->null);
            c.waitFor(v->v.player.onGround());w.getServer().runCommand("tick rate 5");
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.LEFT,PlayerModelType.SLIM);c.runOnClient(v->v.options.framerateLimit().set(10));jumpAttack(c);c.waitFor(v->AirMeleeAudit.full>0&&active(v));c.takeScreenshot("air-melee-actual-5tps-10fps");
            c.runOnClient(v->{v.player.setXRot(18);v.player.setYRot(25);v.options.framerateLimit().set(120);});c.waitTicks(1);check(c.computeOnClient(v->active(v)&&v.player.getXRot()==18&&v.player.getYRot()==25),"Independent aim and original slow world progress");c.takeScreenshot("air-melee-independent-look");
            c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);var frozen=c.computeOnClient(AirMeleeRigClientTest::frame);check(c.computeOnClient(v->frozen!=null?frozen.clip().contains("_air_attack_"):MeleeActions.view(v.player).phase()==MeleeView.IDLE),"Pause hands belong to surviving phase or original CANCEL");long until=System.nanoTime()+300_000_000L;c.waitFor(v->System.nanoTime()>until);check(c.computeOnClient(v->active(v)?frozen!=null&&Math.abs(frame(v).time()-frozen.time())<1e-6:MeleeActions.view(v.player).phase()==MeleeView.IDLE),"Pause freezes a surviving phase or honors original input CANCEL; no frozen stale body");c.takeScreenshot("air-melee-actual-pause");c.setScreen(()->null);c.waitFor(v->v.player.onGround()&&MeleeActions.view(v.player).phase()==MeleeView.IDLE);
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);jumpAttack(c);c.waitFor(v->active(v));w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.hurtServer(s.overworld(),p.damageSources().generic(),1);});c.waitFor(v->v.player.getHealth()<20&&v.player.hurtTime>0);check(c.computeOnClient(v->!active(v)),"Real damage immediately wins over source and grants no immunity");c.takeScreenshot("air-melee-real-hurt");c.waitFor(v->v.player.onGround());
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(1,new ItemStack(Items.TORCH));p.inventoryMenu.broadcastChanges();});jumpAttack(c);c.waitFor(v->active(v));c.getInput().pressKey(v->v.keyHotbarSlots[1]);c.waitFor(v->v.player.getMainHandItem().is(Items.TORCH));check(c.computeOnClient(v->!active(v)),"Actual selected item owns hands immediately");c.takeScreenshot("air-melee-real-slot");c.waitFor(v->v.player.onGround());
            w.getServer().runCommand("tick rate 20");
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).age()>=20);c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround());c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.AIR_SWING&&MeleeActions.view(v.player).charged());check(c.computeOnClient(v->!active(v)),"Actual charged airborne action keeps prior path");c.takeScreenshot("air-melee-native-charged");c.waitFor(v->v.player.onGround());
            c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);
            setup(c,w,id.get(),Items.IRON_SWORD,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,124,.5,Set.of(),0,0,true);});c.waitFor(v->v.player.getY()>123);c.waitFor(v->!v.player.onGround()&&v.player.fallDistance>=2);c.runOnClient(v->{v.options.keyAttack.setDown(true);ActionInput.frame(v);v.options.keyAttack.setDown(false);ActionInput.frame(v);});c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.DROP_SWING);check(c.computeOnClient(v->!active(v)),"Actual gravity-driven drop attack retains original fall multiplier and prior pose");c.takeScreenshot("air-melee-native-drop");
            System.out.println("C6S actual ordinary air sword/axe16 combinations, source GPU/head-root, native jump/landing, one hit/wear/Fuse/free cost, occupied/armor/GUI/5TPS/10FPS/pause/real-look/hurt/selected-item/charged/drop PASS");
        }finally{c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(v->v.keyAttack);c.runOnClient(v->{AirMeleeAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;options.follow=oldFollow;v.options.framerateLimit().set(120);v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
