package dev.wildcraft.test;

import static dev.wildcraft.test.SwordTransitionRigClientTest.*;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.fuse.FusionContent;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Actual chest elytra and used sword, real slots and instant hand/waist ownership. */
public final class SwordWingsRigClientTest implements FabricClientGameTest {
    private static void target(ClientGameTestContext c,TestSingleplayerContext w,int id,boolean near){
        double x=near?.5:4.5;
        w.getServer().runOnServer(server->{var mob=server.overworld().getEntity(id);mob.setPos(x,120,2.7);mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);});
        c.waitFor(v->v.level.getEntity(id)!=null&&Math.abs(v.level.getEntity(id).getX()-x)<.01);
    }
    private static void wings(ClientGameTestContext c,TestSingleplayerContext w){
        w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var wings=new ItemStack(Items.ELYTRA);wings.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,p.getMainArm()==HumanoidArm.LEFT||!p.getOffhandItem().isEmpty());p.setItemSlot(EquipmentSlot.CHEST,wings);p.inventoryMenu.broadcastChanges();});
        c.waitFor(v->v.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA));
        c.runOnClient(v->{var s=RigAudit.fixture;boolean left=v.player.getMainArm()==HumanoidArm.LEFT,carry=!v.player.getOffhandItem().isEmpty();var different=new net.minecraft.core.ClientAsset.ResourceTexture(net.minecraft.resources.Identifier.withDefaultNamespace("entity/equipment/wings/elytra"));RigAudit.fixture=new PlayerSkin(s.body(),left?different:carry?s.body():null,left?s.body():null,s.model(),false);RigAudit.cape=left||carry;SwordWaistAudit.begin();SwordTransitionAudit.begin();WingsWaistAudit.begin();});
        c.waitTicks(3);
        check(c.computeOnClient(v->state(v).getDataOrDefault(BackTransitions.WAIST,false)),"Real chest elytra selects original waist layout before native transition");
    }
    private static void reset(ClientGameTestContext c){c.runOnClient(v->{RigAudit.begin();SwordTransitionAudit.begin();SwordWaistAudit.begin();WingsWaistAudit.begin();});}
    private static void rendered(ClientGameTestContext c,String clip,boolean draw){
        c.waitFor(v->SwordTransitionAudit.full>0&&clip.equals(SwordTransitionAudit.clip)&&WingsWaistAudit.models>0&&WingsWaistAudit.poses>0&&(!v.player.getItemBySlot(EquipmentSlot.CHEST).hasFoil()||WingsWaistAudit.foil>0)&&WingsWaistAudit.balanced>0&&(draw?SwordWaistAudit.nativeGrips>0&&SwordWaistAudit.quads>0:SwordTransitionAudit.backGpu>0));
        check(c.computeOnClient(v->RigAudit.bodies>0&&RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Actual source skin texture submitted with native wings and equipment");
    }
    @Override public void runTest(ClientGameTestContext c){var id=new AtomicInteger();var options=CombatOptions.get();int oldFollow=options.follow;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.getPlayerList().getPlayers().getFirst().teleportTo(.5,120,.5);var mob=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);mob.setPos(.5,120,2.7);mob.setNoAi(true);mob.setNoGravity(true);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);mob.setHealth(200);s.overworld().addFreshEntity(mob);id.set(mob.getId());});
            c.waitFor(v->v.level.getEntity(id.get())!=null);c.getInput().resizeWindow(1280,960);c.runOnClient(v->{options.follow=0;v.options.guiScale().set(2);});
            for(var type:PlayerModelType.values())for(var arm:HumanoidArm.values())for(boolean shield:new boolean[]{false,true}){
                c.runOnClient(v->{RigAudit.cape=null;SwordWaistAudit.enabled=false;WingsWaistAudit.enabled=false;});prepare(c,w,id.get(),type,arm,shield);target(c,w,id.get(),false);wings(c,w);
                String suffix=(shield?"shield":"empty")+"_"+(arm==HumanoidArm.RIGHT?"R":"L");
                c.runOnClient(v->v.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));select(c,7,Items.AIR);rendered(c,"equipment_stow_waist_"+suffix,false);c.takeScreenshot("wings-sword-stow-"+type+"-"+suffix);done(c);
                reset(c);c.runOnClient(v->v.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));select(c,0,Items.IRON_SWORD);rendered(c,"equipment_draw_waist_"+suffix,true);c.takeScreenshot("wings-sword-draw-"+type+"-"+suffix);done(c);
                System.out.println("C6AY actual elytra waist "+type+" "+suffix+" nativeGripError="+SwordWaistAudit.maxGripError+" wingsMatrixError="+WingsWaistAudit.maxError+" PASS");
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==1&&p.getInventory().getItem(0).getCount()==1&&p.getMainHandItem().getDamageValue()==1&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31&&PlayerStamina.get(p).stamina()==100&&p.getOffhandItem().getDamageValue()==0,"Waist gestures preserve actual single inventory, native one hit/wear/Fuse and no new costs");});
            }
            select(c,7,Items.AIR);done(c);reset(c);c.runOnClient(v->v.options.framerateLimit().set(10));select(c,0,Items.IRON_SWORD);rendered(c,"equipment_draw_waist_shield_R",true);c.takeScreenshot("wings-sword-10fps");done(c);c.runOnClient(v->v.options.framerateLimit().set(120));
            select(c,7,Items.AIR);done(c);reset(c);w.getServer().runCommand("tick rate 5");long started=System.nanoTime();select(c,0,Items.IRON_SWORD);rendered(c,"equipment_draw_waist_shield_R",true);c.takeScreenshot("wings-sword-native-5tps");done(c);check(System.nanoTime()-started<1_200_000_000L,"Original waist actual-time event not stretched by slow world ticks");w.getServer().runCommand("tick rate 20");
            select(c,7,Items.AIR);done(c);reset(c);select(c,0,Items.IRON_SWORD);c.waitFor(v->active(v));c.runOnClient(v->{v.player.setXRot(18);v.player.setYRot(25);});c.waitTicks(1);check(c.computeOnClient(v->active(v)&&v.player.getXRot()==18),"Cape waist retains current native independent head");c.takeScreenshot("wings-sword-native-look");done(c);c.runOnClient(v->{v.player.setXRot(0);v.player.setYRot(0);});
            select(c,7,Items.AIR);done(c);reset(c);select(c,0,Items.IRON_SWORD);c.waitFor(v->active(v));c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);float frozen=c.computeOnClient(SwordTransitionRigClientTest::progress);check(frozen<1,"Pause retains actual live waist event");long until=System.nanoTime()+400_000_000L;c.waitFor(v->System.nanoTime()>until);check(c.computeOnClient(v->active(v)&&Math.abs(progress(v)-frozen)<.001F),"Waist source and native ownership clock freeze together");c.takeScreenshot("wings-sword-pause");c.setScreen(()->null);done(c);
            select(c,7,Items.AIR);done(c);select(c,0,Items.IRON_SWORD);c.waitFor(v->active(v));c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));check(c.computeOnClient(v->!active(v)),"GUI cancels waist body immediately");c.takeScreenshot("wings-sword-menu");c.setScreen(()->null);c.waitTicks(4);check(c.computeOnClient(v->!active(v)),"Closing GUI cannot replay waist event");
            select(c,7,Items.AIR);done(c);target(c,w,id.get(),true);select(c,0,Items.IRON_SWORD);c.waitFor(v->active(v));c.runOnClient(v->{v.options.keyAttack.setDown(true);ActionInput.frame(v);v.options.keyAttack.setDown(false);ActionInput.frame(v);});c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.SWING);check(c.computeOnClient(v->!active(v)),"Actual attack takes hands without source delay");c.takeScreenshot("wings-sword-native-attack");c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE&&v.player.getMainHandItem().getDamageValue()==2);
            select(c,7,Items.AIR);done(c);select(c,0,Items.IRON_SWORD);c.waitFor(v->active(v));c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.isUsingItem());check(c.computeOnClient(v->!active(v)),"Real offhand shield native use immediately owns hands");c.takeScreenshot("wings-sword-native-shield");c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->!v.player.isUsingItem());
            select(c,7,Items.AIR);done(c);select(c,0,Items.IRON_SWORD);c.waitFor(v->active(v));select(c,8,Items.TORCH);check(c.computeOnClient(v->!active(v)),"Different actual torch interrupts waist source");c.takeScreenshot("wings-sword-new-torch");
            select(c,7,Items.AIR);done(c);select(c,0,Items.IRON_SWORD);c.waitFor(v->active(v));w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.hurtServer(s.overworld(),p.damageSources().generic(),1);});c.waitFor(v->v.player.hurtTime>0&&v.player.getHealth()<20);check(c.computeOnClient(v->!active(v)),"Actual hurt immediately restores native cape body path without immunity");c.takeScreenshot("wings-sword-native-hurt");c.waitTicks(12);
            select(c,7,Items.AIR);done(c);reset(c);select(c,0,Items.IRON_SWORD);c.waitFor(v->active(v));
            c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround()&&v.player.getDeltaMovement().y>0);c.getInput().releaseKey(v->v.keyJump);
            check(c.computeOnClient(v->!active(v)),"Natural jump immediately restores native wings, without extending source clock");c.takeScreenshot("wings-sword-natural-jump-exit");
            c.waitFor(v->!v.player.onGround()&&v.player.getDeltaMovement().y<-.02);c.runOnClient(v->WingsWaistAudit.begin());c.getInput().holdKey(v->v.keyJump);c.waitFor(v->v.player.isFallFlying());c.getInput().releaseKey(v->v.keyJump);
            c.waitFor(v->WingsWaistAudit.nativeModels>0);check(c.computeOnClient(v->!active(v)),"Actual native elytra flight owns original wings and no waist source");c.takeScreenshot("wings-sword-native-flight-exit");c.waitFor(v->v.player.onGround()&&!v.player.isFallFlying());
            for(var armor:new Item[]{Items.ELYTRA,Items.IRON_CHESTPLATE}){
                select(c,7,Items.AIR);done(c);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(armor));p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getItemBySlot(EquipmentSlot.CHEST).is(armor)&&v.player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET));select(c,0,Items.IRON_SWORD);c.waitTicks(1);check(c.computeOnClient(v->!active(v)&&state(v).getDataOrDefault(BackTransitions.WAIST,false)),"Unqualified real helmet plus wings/armor keep existing native waist path");c.takeScreenshot("wings-sword-native-"+armor);
            }
            System.out.println("C6AY 16 actual native elytra waist sword draw/stow combinations, skin/native sword-shield/wings models and body matrix, original ownership/clock/pause/GUI/attack/use/item exit and armor-wings fallback PASS; complex armor and other weapon wings sources remain unqualified");
        }finally{c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);c.runOnClient(v->{SwordWaistAudit.enabled=false;WingsWaistAudit.enabled=false;SwordTransitionAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;RigAudit.cape=null;v.options.framerateLimit().set(120);options.follow=oldFollow;v.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);});}
    }
}
