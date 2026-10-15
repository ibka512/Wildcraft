package dev.wildcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.animation.CombatPoses;
import dev.wildcraft.client.combat.*;
import dev.wildcraft.client.focus.FocusSettingsScreen;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.Combat;
import dev.wildcraft.network.CombatIntent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.atomic.AtomicInteger;

/** Native mappings -> production packets -> actual target, movement, shield and UI. */
public final class CombatClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        var o=CombatOptions.get();boolean oldHold=o.hold,oldNeutral=o.neutral,oldJump=o.jumpDodge;int oldRange=o.range,oldFollow=o.follow,oldBow=o.bowSkill,oldDodge=o.dodgeSkill;
        var aggressive=new AtomicInteger();var center=new AtomicInteger();var left=new AtomicInteger();var right=new AtomicInteger();
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                for(int x=-7;x<=7;x++)for(int z=-3;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                p.teleportTo(.5,120,.5);p.setYRot(0);p.setYHeadRot(0);p.setXRot(0);p.setHealth(20);
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
                var a=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);a.setPos(.5,120,6.5);a.setNoAi(true);a.setNoGravity(true);s.overworld().addFreshEntity(a);center.set(a.getId());
                var b=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);b.setPos(-3.5,120,7.5);b.setNoAi(true);b.setNoGravity(true);s.overworld().addFreshEntity(b);left.set(b.getId());
                var d=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);d.setPos(4.5,120,7.5);d.setNoAi(true);d.setNoGravity(true);s.overworld().addFreshEntity(d);right.set(d.getId());
                var cow=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);cow.setPos(1.5,120,3.5);cow.setNoAi(true);cow.setNoGravity(true);s.overworld().addFreshEntity(cow);
                p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client->client.player.getY()>119&&client.player.getOffhandItem().is(Items.SHIELD)&&client.level.getEntity(right.get())!=null&&ActionInput.tickFrame().gameplay());
            c.runOnClient(client->{o.hold=false;o.neutral=false;o.range=1;o.follow=0;o.jumpDodge=false;client.options.setCameraType(CameraType.FIRST_PERSON);client.player.setYRot(0);client.player.setXRot(0);WildcraftBindings.DEFEND.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_J));KeyMapping.resetMapping();});
            c.waitTicks(3);c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);
            c.waitFor(client->Combat.view(client.player).targetId()==center.get());
            c.getInput().holdKeyFor(WildcraftBindings.TARGET_LEFT,1);c.waitFor(client->Combat.view(client.player).targetId()==left.get());
            c.getInput().holdKeyFor(WildcraftBindings.TARGET_RIGHT,1);c.waitFor(client->Combat.view(client.player).targetId()==center.get());
            c.getInput().holdKeyFor(WildcraftBindings.TARGET_RIGHT,1);c.waitFor(client->Combat.view(client.player).targetId()==right.get());
            c.getInput().holdKeyFor(WildcraftBindings.TARGET_LEFT,1);c.waitFor(client->Combat.view(client.player).targetId()==center.get());
            c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(client->!Combat.locked(client.player));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var mob=EntityTypes.ENDERMAN.create(s.overworld(),EntitySpawnReason.COMMAND);
                mob.setPos(.5,120,4.5);mob.setNoAi(true);mob.setNoGravity(true);s.overworld().addFreshEntity(mob);mob.setTarget(p);
                check(mob.getTarget()==p,"Native neutral aggression owns a real survival player target");aggressive.set(mob.getId());});
            c.waitFor(client->client.level.getEntity(aggressive.get())!=null);c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);
            c.waitFor(client->Combat.view(client.player).targetId()==aggressive.get());
            w.getServer().runOnServer(s->s.overworld().getEntity(aggressive.get()).discard());c.waitFor(client->!Combat.locked(client.player));
            c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(client->Combat.view(client.player).targetId()==center.get());
            double before=c.computeOnClient(client->client.player.getZ());final double walkBefore=before;
            c.runOnClient(client->client.player.setYRot(90));c.getInput().holdKey(v->v.keyUp);
            c.waitFor(client->client.player.getZ()>walkBefore+.4,60);c.getInput().releaseKey(v->v.keyUp);
            check(c.computeOnClient(client->Math.abs(client.player.getYRot()-90)<.1&&Math.abs(client.player.getX()-.5)<.12),"Native movement follows enemy while first-person aim remains free");
            c.takeScreenshot("c2-first-person-free-aim-lock");
            c.runOnClient(client->{o.follow=3;client.options.setCameraType(CameraType.THIRD_PERSON_BACK);client.player.setYRot(90);});
            c.waitFor(client->client.player.getYRot()<89,80);
            check(c.computeOnClient(client->client.player.getYRot()>55),"Third-person help turns gradually and leaves an aim dead zone");
            c.runOnClient(client->{client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);client.player.setYRot(90);});c.waitTicks(10);
            check(c.computeOnClient(client->Math.abs(client.player.getYRot()-90)<.1),"Front-facing observation camera is not pulled by third-person combat help");
            c.runOnClient(client->client.player.setYRot(25));c.waitTicks(4);
            check(c.computeOnClient(client->Math.abs(client.player.getYRot()-25)<.1),"Manual aim inside the dead zone is retained");
            c.runOnClient(client->{o.follow=0;client.options.setCameraType(CameraType.FIRST_PERSON);client.player.setYRot(90);});
            c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(client->Combat.view(client.player).defending()&&client.player.isUsingItem()&&client.player.getUseItem().is(Items.SHIELD));
            c.waitTicks(10);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();float health=p.getHealth();var enemy=(LivingEntity)s.overworld().getEntity(center.get());p.hurtServer(s.overworld(),p.damageSources().mobAttack(enemy),6);check(p.getHealth()==health&&p.getOffhandItem().getDamageValue()>0,"Actual production block faces target despite independent aim");});
            c.waitFor(client->client.player.getOffhandItem().getDamageValue()>0&&client.player.isUsingItem()&&Combat.view(client.player).defending());
            c.getInput().holdKey(v->v.keyShift);c.waitFor(client->client.player.isShiftKeyDown());
            check(c.computeOnClient(client->Combat.view(client.player).defending()),"Defense and sneak are independent and coexist");c.getInput().releaseKey(v->v.keyShift);
            before=c.computeOnClient(client->client.player.getZ());final double defenseBefore=before;c.getInput().holdKey(v->v.keyUp);
            c.waitFor(client->client.player.getZ()>defenseBefore+.2,60);c.getInput().releaseKey(v->v.keyUp);
            c.runOnClient(client->client.player.setYRot(0));c.takeScreenshot("c2-first-person-independent-shield");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.IRON_LEGGINGS));p.inventoryMenu.broadcastChanges();});
            c.waitFor(client->client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE));
            c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.waitTicks(20);c.takeScreenshot("c2-third-person-movement-shield");
            c.runOnClient(client->{
                var state=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);
                var layer=state.getData(CombatPoses.LAYER);check(layer!=null&&layer.lock()>0&&layer.defense()>0,"Live lock and shield layers reach renderer: layer="+layer+", view="+Combat.view(client.player)+", using="+client.player.isUsingItem()+", compatible="+Combat.compatible(client.player)+", paused="+client.isPaused());
                var skin=state.skin;
                for(boolean slim:new boolean[]{false,true})for(var arm:HumanoidArm.values()){
                    state.skin=PlayerSkin.insecure(skin.body(),null,null,slim?PlayerModelType.SLIM:PlayerModelType.WIDE);
                    var model=new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);
                    state.yRot=65;state.setData(CombatPoses.LAYER,null);model.setupAnim(state);float leg=model.leftLeg.xRot,head=model.head.yRot;
                    state.setData(CombatPoses.LAYER,new CombatPoses.Layer(layer.lock(),layer.defense(),arm,false));model.setupAnim(state);
                    check(Math.abs(model.leftLeg.xRot-leg)<.001&&Math.abs(model.head.yRot-head)<.001,"Shield mask preserves native gait and independent head");
                    check(Math.abs((arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm).xRot+1.08)<.1,"Shield mask applies to both arms on both rigs");
                    check(Math.abs((arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm).yRot-(arm==HumanoidArm.RIGHT?-.5236F:.5236F))<.1,"Locked shield faces body target while head remains independently aimed");
                    check(model.leftSleeve.xRot==0&&model.jacket.yRot==0,"Native clothing children are not assigned duplicate parent transforms");
                    var meshes=net.minecraft.client.model.HumanoidModel.createArmorMeshSet(new CubeDeformation(1),new CubeDeformation(.5F));
                    for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                        var armor=new net.minecraft.client.model.HumanoidModel<AvatarRenderState>(LayerDefinition.create(meshes.get(slot),64,32).bakeRoot());
                        armor.setupAnim(state);
                        check(Math.abs(armor.body.y-model.body.y)<.001&&Math.abs(armor.body.yRot-model.body.yRot)<.001
                            &&Math.abs((arm==HumanoidArm.RIGHT?armor.rightArm:armor.leftArm).xRot-(arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm).xRot)<.001
                            &&Math.abs((arm==HumanoidArm.RIGHT?armor.rightArm:armor.leftArm).yRot-(arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm).yRot)<.001,
                            "Native armor receives the same combat mask exactly once: "+slot);
                    }
                }
                state.skin=skin;
            });
            c.setScreen(()->new FocusSettingsScreen(null));c.waitTicks(3);
            c.waitFor(client->!Combat.view(client.player).defending()&&Combat.view(client.player).targetId()<0);
            c.runOnClient(client->{WildcraftBindings.DEFEND.setDown(true);});c.waitTicks(2);c.setScreen(()->null);c.waitTicks(3);
            check(c.computeOnClient(client->!Combat.view(client.player).defending()&&!client.player.isUsingItem()),"GUI close does not restart held defense");
            c.getInput().releaseKey(WildcraftBindings.DEFEND);c.waitTicks(2);
            c.runOnClient(client->{o.hold=true;client.player.setYRot(0);client.player.setXRot(0);});c.getInput().holdKey(WildcraftBindings.LOCK_TARGET);
            c.waitFor(client->Combat.view(client.player).targetId()==center.get());c.getInput().releaseKey(WildcraftBindings.LOCK_TARGET);c.waitFor(client->Combat.view(client.player).targetId()<0);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));p.getInventory().setItem(9,new ItemStack(Items.ARROW,8));p.inventoryMenu.broadcastChanges();});
            c.waitFor(client->client.player.getMainHandItem().is(Items.BOW));c.getInput().holdKey(WildcraftBindings.LOCK_TARGET);c.waitFor(client->Combat.view(client.player).targetId()==center.get());c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(client->Combat.view(client.player).defending());
            c.getInput().holdKey(v->v.keyUse);c.waitFor(client->client.player.isUsingItem()&&client.player.getUseItem().is(Items.BOW));
            c.waitFor(client->!Combat.view(client.player).defending());
            c.runOnClient(client->client.player.setYRot(15));c.waitTicks(3);
            check(c.computeOnClient(client->Math.abs(client.player.getYRot()-15)<.1&&Combat.locked(client.player)),"Locked native bow keeps precise free aim");
            c.runOnClient(client->{var state=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);var layer=state.getData(CombatPoses.LAYER);check(layer!=null&&layer.defense()==0,"Native bow pose immediately owns arms after defense without waiting for shield fade");});
            c.getInput().releaseKey(WildcraftBindings.LOCK_TARGET);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.isUsingItem()&&p.getUseItem().is(Items.BOW),"Server acknowledges native bow priority over independent shield");});
            c.getInput().releaseKey(WildcraftBindings.DEFEND);c.getInput().releaseKey(v->v.keyUse);c.waitFor(client->!client.player.isUsingItem());
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,150,.5);p.setNoGravity(true);p.setOnGround(false);dev.wildcraft.player.GliderEquipment.set(p,new ItemStack(dev.wildcraft.registry.WildcraftItems.PARAGLIDER));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.inventoryMenu.broadcastChanges();});
            c.waitFor(client->client.player.getY()>149&&dev.wildcraft.player.GliderEquipment.equipped(client.player)&&dev.wildcraft.traversal.Gliding.eligible(client.player));
            c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(client->dev.wildcraft.traversal.Gliding.active(client.player));
            c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(client->!dev.wildcraft.traversal.Gliding.active(client.player)&&Combat.view(client.player).defending()&&client.player.isUsingItem());
            c.takeScreenshot("c2-glide-to-independent-defense");c.getInput().releaseKey(WildcraftBindings.DEFEND);c.waitFor(client->!client.player.isUsingItem());
            c.getInput().holdKeyFor(WildcraftBindings.SETTINGS,1);c.waitFor(client->client.gui.screen() instanceof FocusSettingsScreen);
            c.runOnClient(client->{
                var container=(net.minecraft.client.gui.components.AbstractContainerWidget)client.gui.screen().children().stream().filter(net.minecraft.client.gui.components.AbstractContainerWidget.class::isInstance).findFirst().orElseThrow();
                container.setScrollAmount(1000);
                var button=container.children().stream().filter(v->v instanceof net.minecraft.client.gui.components.Button).map(v->(net.minecraft.client.gui.components.Button)v).filter(v->v.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("combat.wildcraft.settings").getString())).findFirst().orElseThrow();
                check(button.getY()>=container.getY()&&button.getBottom()<=container.getBottom(),"Combat navigation is reachable by native scrolling");
                button.onPress(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN,0,0));
            });
            c.waitFor(client->client.gui.screen() instanceof CombatSettingsScreen);c.takeScreenshot("c2-combat-preferences");
            c.runOnClient(client->{
                var container=(net.minecraft.client.gui.components.AbstractContainerWidget)client.gui.screen().children().stream().filter(net.minecraft.client.gui.components.AbstractContainerWidget.class::isInstance).findFirst().orElseThrow();
                for(var widget:container.children())if(widget instanceof net.minecraft.client.gui.components.Button button)button.onPress(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN,0,0));
                check(o.range==2&&o.neutral&&o.follow==1&&!o.hold&&o.jumpDodge&&o.bowSkill==1&&o.dodgeSkill==1,"Native settings controls change all four preferences");
                o.range=0;o.neutral=false;o.follow=0;CombatOptions.load();
                check(CombatOptions.get().range==2&&CombatOptions.get().neutral&&CombatOptions.get().follow==1&&!CombatOptions.get().hold&&CombatOptions.get().jumpDodge&&CombatOptions.get().bowSkill==1&&CombatOptions.get().dodgeSkill==1,"Independent preferences round-trip without rewriting old focus file");
            });
            c.setScreen(()->null);
        }finally{
            for(var binding:new KeyMapping[]{WildcraftBindings.DEFEND,WildcraftBindings.LOCK_TARGET,WildcraftBindings.TARGET_LEFT,WildcraftBindings.TARGET_RIGHT})c.getInput().releaseKey(binding);
            c.getInput().releaseKey(v->v.keyUp);c.getInput().releaseKey(v->v.keyShift);c.getInput().releaseKey(v->v.keyUse);
            c.runOnClient(client->{var restored=CombatOptions.get();restored.jumpDodge=oldJump;restored.hold=oldHold;restored.neutral=oldNeutral;restored.range=oldRange;restored.follow=oldFollow;restored.bowSkill=oldBow;restored.dodgeSkill=oldDodge;CombatOptions.save();WildcraftBindings.DEFEND.setKey(WildcraftBindings.DEFEND.getDefaultKey());KeyMapping.resetMapping();client.options.setCameraType(CameraType.FIRST_PERSON);});
        }
        System.out.println("C2 COMBAT native lock/select/switch/hold, independent aim movement, shield+sneak/block/bow/GUI ownership, native wide/slim both-arm masks and preference round-trip PASS");
    }
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
