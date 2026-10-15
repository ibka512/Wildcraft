package dev.wildcraft.test;
import dev.wildcraft.client.combat.*;
import dev.wildcraft.client.focus.*;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.focus.*;
import dev.wildcraft.input.Action;
import dev.wildcraft.network.*;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.fuse.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.atomic.*;

/** Production jar and real input/movement. Explicit lifecycle probes are separate from the earned escape. */
public final class ReactionClientTest implements FabricClientGameTest {
    private static void check(boolean okay,String text){if(!okay)throw new AssertionError(text);}
    private static class Goal extends ZombieAttackGoal{Goal(Husk z){super(z,1,true);}void attempt(LivingEntity t){checkAndPerformAttack(t);}}
    private static void prepare(TestSingleplayerContext w,int id){w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ReactionTime.cancel(p);FocusTime.interrupt(p);p.stopUsingItem();MeleeActions.reset(p);p.teleportTo(.5,120,.5);p.setNoGravity(false);p.setOnGround(true);p.setYRot(0);p.setXRot(0);p.setHealth(20);p.setDeltaMovement(Vec3.ZERO);PlayerStamina.fill(p);var m=(Husk)s.overworld().getEntity(id);ThreatActions.cancel(m);m.setTarget(p);m.setHealth(20);m.setPos(.5,120,1.8);m.setDeltaMovement(Vec3.ZERO);p.inventoryMenu.broadcastChanges();});}
    private static void episode(TestSingleplayerContext w,int id){w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(ReactionTime.reward(p,(Husk)s.overworld().getEntity(id)),"Controlled lifecycle opportunity accepted");});}
    private static void restored(TestSingleplayerContext w,String what){w.getServer().runOnServer(s->check(!ReactionTime.active(s)&&s.tickRateManager().tickrate()==20,what+" releases world time"));}
    @Override public void runTest(ClientGameTestContext c){
        int oldBow=CombatOptions.get().bowSkill,oldDodge=CombatOptions.get().dodgeSkill,oldFollow=CombatOptions.get().follow,oldStrength=FocusOptions.get().strength;
        var id=new AtomicInteger();var begin=new AtomicLong();
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setNoAi(true);m.setNoGravity(true);s.overworld().addFreshEntity(m);id.set(m.getId());var sword=new ItemStack(Items.IRON_SWORD);FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),s.registryAccess());p.setItemInHand(InteractionHand.MAIN_HAND,sword);});
            prepare(w,id.get());c.runOnClient(client->{CombatOptions.get().bowSkill=0;CombatOptions.get().dodgeSkill=0;CombatOptions.get().follow=0;client.options.setCameraType(CameraType.FIRST_PERSON);client.player.setYRot(0);client.player.setXRot(0);});
            c.waitFor(client->client.player.getMainHandItem().is(Items.IRON_SWORD)&&client.level.getEntity(id.get())!=null&&client.player.onGround()&&ActionInput.tickFrame().gameplay());c.waitTicks(25);
            // The real native goal prepares, then a real bound key drives native collision and movement packets.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();new Goal((Husk)s.overworld().getEntity(id.get())).attempt(p);});
            c.waitFor(client->ThreatActions.view((LivingEntity)client.level.getEntity(id.get())).age()>=6);
            c.getInput().holdKeyFor(WildcraftBindings.DODGE,1);
            c.waitFor(client->ReactionTime.active(client.player));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getZ()<.26&&p.getHealth()==20&&PlayerStamina.get(p).stamina()>=92,"Actual hop leaves imminent native melee range without immunity or extra energy fee");check(s.tickRateManager().tickrate()==5&&!FocusTime.active(s),"Earned short episode owns the shared time lease alone");CounterAttackAudit.begin(p);});
            c.runOnClient(client->{FocusOptions.get().strength=0;check(FocusClient.visible()&&FocusClient.postEffects(java.util.List.of()).isEmpty(),"Turning filter off does not disable the gameplay skill");});
            // Observe the newly synchronized opportunity on an idle frame before injecting a fresh press.
            c.runOnClient(client->{ActionInput.frame(client);SkillControls.frame(client);});begin.set(System.nanoTime());c.runOnClient(client->{client.player.setXRot(8);net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(client.options.keyAttack));client.options.keyAttack.setDown(true);ActionInput.frame(client);SkillControls.frame(client);client.options.keyAttack.setDown(false);ActionInput.frame(client);SkillControls.frame(client);});
            c.waitFor(client->client.player.getMainHandItem().getDamageValue()==1&&!ReactionTime.active(client.player));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getMainHandItem().getDamageValue()==1&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"One frame tap owns one native attack/wear/Fuse");var m=(Husk)s.overworld().getEntity(id.get());check(m.getHealth()<20,"Counter requires current actual aim to hit the threat");float hp=m.getHealth();MeleeActions.receive(p,new MeleeIntent(MeleeIntent.RELEASE,(byte)0));MeleeActions.receive(p,new MeleeIntent(MeleeIntent.RELEASE,(byte)0));check(m.getHealth()==hp&&p.getMainHandItem().getDamageValue()==1&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"Repeated counter packets cannot settle again");});c.waitTicks(3);w.getServer().runOnServer(s->{check(CounterAttackAudit.calls==1,"Real native click queue must not dispatch a second attack after the frame-owned counter");CounterAttackAudit.player=null;});restored(w,"Counter");c.takeScreenshot("c5-earned-hop-frame-counter");
            System.out.println("C5 frame counter observed within "+((System.nanoTime()-begin.get())/1_000_000)+"ms (includes client acknowledgement; not a performance benchmark)");
            c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(12);prepare(w,id.get());
            c.runOnClient(client->{FocusOptions.get().strength=1;CombatOptions.get().dodgeSkill=1;});c.waitTicks(3);episode(w,id.get());c.waitFor(client->ReactionTime.view(client.player).phase()==ReactionView.READY);
            w.getServer().runOnServer(s->check(s.tickRateManager().tickrate()==20,"Manual opportunity does not slow before confirmation"));
            c.getInput().holdKeyFor(WildcraftBindings.CONFIRM_SKILL,1);c.waitFor(client->ReactionTime.active(client.player));c.takeScreenshot("c5-manual-link-time-first-person");
            c.waitFor(client->!ReactionTime.active(client.player));restored(w,"Real-time timeout");
            prepare(w,id.get());c.runOnClient(client->CombatOptions.get().dodgeSkill=0);c.waitTicks(3);episode(w,id.get());c.waitFor(client->ReactionTime.active(client.player));
            c.setScreen(()->new CombatSettingsScreen(null));c.waitFor(client->!ReactionTime.active(client.player));restored(w,"Menu interruption");c.takeScreenshot("c5-skill-modes-scrollable-settings");c.setScreen(()->null);c.waitTicks(3);
            prepare(w,id.get());episode(w,id.get());c.waitFor(client->ReactionTime.active(client.player));w.getServer().runOnServer(s->{s.tickRateManager().setTickRate(12);ReactionTime.pulse(s);check(!ReactionTime.active(s)&&s.tickRateManager().tickrate()==12,"External rate is preserved");s.tickRateManager().setTickRate(20);});
            c.waitFor(client->!ReactionTime.active(client.player));prepare(w,id.get());episode(w,id.get());c.waitFor(client->ReactionTime.active(client.player));w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));ReactionTime.pulse(s);check(!ReactionTime.active(s)&&s.tickRateManager().tickrate()==20,"Weapon replacement closes transient episode");p.inventoryMenu.broadcastChanges();});c.waitFor(client->client.player.getMainHandItem().is(Items.IRON_AXE));
            c.runOnClient(client->CombatOptions.get().dodgeSkill=2);c.waitTicks(3);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(!ReactionTime.reward(p,(Husk)s.overworld().getEntity(id.get()))&&s.tickRateManager().tickrate()==20,"Disabled perfect-dodge skill cannot acquire world time");});
            c.runOnClient(client->CombatOptions.get().dodgeSkill=0);c.waitTicks(3);prepare(w,id.get());
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.resetAttackStrengthTicker();});episode(w,id.get());c.waitFor(client->ReactionTime.active(client.player));w.getServer().waitFor(s->{var p=s.getPlayerList().getPlayers().getFirst();return ReactionTime.active(p)&&p.getAttackStrengthScale(.5F)>=.9F;});
            w.getServer().runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getAttackStrengthScale(.5F)>=.9F,"Native weapon recovery reaches full strength inside the bounded real-time episode"));
            c.runOnClient(client->client.pauseGame(false));c.waitFor(client->!ReactionTime.active(client.player));restored(w,"Pause");c.setScreen(()->null);c.waitTicks(3);
            prepare(w,id.get());episode(w,id.get());c.waitFor(client->ReactionTime.active(client.player));w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.hurtServer(s.overworld(),p.damageSources().generic(),1);check(!ReactionTime.active(s)&&p.getHealth()<20,"An active episode is still vulnerable to real damage");});restored(w,"Real hurt");
            c.waitFor(client->!ReactionTime.active(client.player));prepare(w,id.get());episode(w,id.get());c.waitFor(client->ReactionTime.active(client.player));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.getLevel(net.minecraft.world.level.Level.NETHER),.5,120,.5,java.util.Set.of(),0,0,true);});c.waitFor(client->client.level.dimension().equals(net.minecraft.world.level.Level.NETHER));restored(w,"Dimension");
            w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(),.5,120,.5,java.util.Set.of(),0,0,true));c.waitFor(client->client.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD));c.waitTicks(3);
            // Native airborne bow follows an independent manual preference and the same confirmation Action.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,150,.5);p.setNoGravity(true);p.setOnGround(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));p.getInventory().setItem(9,new ItemStack(Items.ARROW,8));p.inventoryMenu.broadcastChanges();});
            c.runOnClient(client->CombatOptions.get().bowSkill=1);c.waitFor(client->client.player.getY()>149&&client.player.getMainHandItem().is(Items.BOW));c.waitTicks(3);c.getInput().holdKey(v->v.keyUse);c.waitFor(client->client.player.isUsingItem());c.waitTicks(3);check(c.computeOnClient(client->!FocusTime.active(client.player)),"Manual bow does not activate from drawing alone");
            c.getInput().holdKeyFor(WildcraftBindings.CONFIRM_SKILL,1);c.waitFor(client->FocusTime.active(client.player));c.takeScreenshot("c5-manual-airborne-bow");c.getInput().releaseKey(v->v.keyUse);c.waitFor(client->!FocusTime.active(client.player));restored(w,"Manual bow release");
            c.runOnClient(client->CombatOptions.get().bowSkill=2);c.waitTicks(3);c.getInput().holdKey(v->v.keyUse);c.waitFor(client->client.player.isUsingItem());c.waitTicks(3);check(c.computeOnClient(client->!FocusTime.active(client.player)),"Disabled bow retains ordinary use");c.getInput().releaseKey(v->v.keyUse);
            c.waitFor(client->!client.player.isUsingItem());
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,122,.5);p.setNoGravity(true);p.setOnGround(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.inventoryMenu.broadcastChanges();});
            c.waitFor(client->client.player.getY()>121.9&&client.player.getY()<122.1&&client.player.getMainHandItem().is(Items.IRON_SWORD));c.runOnClient(client->{client.player.setYRot(0);client.player.setXRot(45);client.options.setCameraType(CameraType.FIRST_PERSON);});c.waitTicks(25);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.fallDistance=12;p.setDeltaMovement(0,-.01,0);});
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);c.getInput().releaseKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.DROP_SWING);verifyAirMasks(c);c.takeScreenshot("c5-first-person-downward-swing");
            c.waitTicks(12);c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_BACK));w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.fallDistance=0;});c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);c.getInput().releaseKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.AIR_SWING);c.takeScreenshot("c5-third-person-airborne-swing");
            System.out.println("C5 REACTION real native goal + key hop/collision/packets, earned lease, frame counter/Fuse once, filter independent, manual/disabled dodge and bow, real-time recovery, timeout/menu/pause/hurt/dimension/replacement/external time, airborne/drop rig masks PASS");
        }finally{CounterAttackAudit.player=null;c.getInput().releaseKey(WildcraftBindings.DODGE);c.getInput().releaseKey(WildcraftBindings.CONFIRM_SKILL);c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);c.runOnClient(client->{var o=CombatOptions.get();o.bowSkill=oldBow;o.dodgeSkill=oldDodge;o.follow=oldFollow;CombatOptions.save();FocusOptions.get().strength=oldStrength;client.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
    private static void verifyAirMasks(ClientGameTestContext c){c.runOnClient(client->{
        var state=(net.minecraft.client.renderer.entity.state.AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,0);
        var actual=state.getData(dev.wildcraft.client.animation.MeleePoses.LAYER);check(actual!=null&&actual.phase()==MeleeView.DROP_SWING,"Actual server drop phase reaches native render extraction");
        state.setData(dev.wildcraft.client.animation.CombatPoses.LAYER,null);state.setData(dev.wildcraft.client.render.CharacterPoses.POSE,null);state.setData(dev.wildcraft.client.animation.GuardPoses.SHIELD,null);state.yRot=30;
        for(byte phase:new byte[]{MeleeView.AIR_SWING,MeleeView.DROP_SWING})for(boolean slim:new boolean[]{false,true})for(var hand:net.minecraft.world.entity.HumanoidArm.values()){
            state.mainArm=hand;state.setData(dev.wildcraft.client.animation.MeleePoses.LAYER,new MeleeView(phase,false,false,2,5,(byte)0,0,1,0,0));
            var model=new net.minecraft.client.model.player.PlayerModel(net.minecraft.client.model.geom.builders.LayerDefinition.create(net.minecraft.client.model.player.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);model.setupAnim(state);check(Math.abs(model.head.yRot-Math.toRadians(30))<.01,"Air/drop masks preserve native head aim");
            var meshes=net.minecraft.client.model.HumanoidModel.createArmorMeshSet(new net.minecraft.client.model.geom.builders.CubeDeformation(1),new net.minecraft.client.model.geom.builders.CubeDeformation(.5F));
            for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){var armor=new net.minecraft.client.model.HumanoidModel<net.minecraft.client.renderer.entity.state.AvatarRenderState>(net.minecraft.client.model.geom.builders.LayerDefinition.create(meshes.get(slot),64,32).bakeRoot());armor.setupAnim(state);check(Math.abs(armor.body.xRot-model.body.xRot)<.001&&Math.abs(armor.rightArm.xRot-model.rightArm.xRot)<.001,"Air/drop armor mask applied once "+slot);}
        }
    });}

}
