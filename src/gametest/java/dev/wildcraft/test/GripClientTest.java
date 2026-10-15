package dev.wildcraft.test;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.*;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.PlayerStamina;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

/** Native GPU submission and actual keys, plus controlled rig/skin and empty-hand compatibility matrix. */
public final class GripClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        var id=new AtomicInteger();
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{for(int x=-5;x<=5;x++)for(int z=-4;z<=9;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);p.setYRot(0);p.setYHeadRot(0);p.setXRot(0);var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,3.2);m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);m.setHealth(200);s.overworld().addFreshEntity(m);id.set(m.getId());});
            c.waitFor(client->client.player.getY()>119&&ActionInput.tickFrame().gameplay());c.runOnClient(client->{client.options.setCameraType(CameraType.FIRST_PERSON);client.player.setYRot(0);client.player.setXRot(0);});
            for(var item:new Item[]{Items.IRON_SWORD,Items.IRON_AXE,Items.TRIDENT,Items.MACE,Items.IRON_SPEAR}){
                var kind=MeleeWeapon.of(new ItemStack(item));equip(w,id.get(),item,ItemStack.EMPTY);c.waitFor(client->client.player.getMainHandItem().is(item)&&client.player.getOffhandItem().isEmpty());c.waitTicks(45);
                c.runOnClient(client->GripAudit.begin());c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=kind.threshold);c.waitTicks(2);
                check(c.computeOnClient(client->GripAudit.main>0&&GripAudit.kind==kind&&(kind==MeleeWeapon.SWORD?GripAudit.support==0:GripAudit.support==GripAudit.main)),"Actual GPU one primary arm and only eligible empty support arm: "+kind);
                c.takeScreenshot("c6c-first-"+kind.name().toLowerCase()+"-skin-grip-charge");matrix(c,kind);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CounterAttackAudit.begin(p);SpearAttackAudit.begin(p);});c.getInput().releaseKey(v->v.keyAttack);
                int wear=kind==MeleeWeapon.AXE?2:1;c.waitFor(client->client.player.getMainHandItem().getDamageValue()==wear);c.waitFor(client->GripAudit.swing>0);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getMainHandItem().get(FusionContent.DATA).remaining()==31&&PlayerStamina.get(p).stamina()==100-kind.cost,"Hands do not add Fuse/energy transactions");check(kind==MeleeWeapon.SPEAR?SpearAttackAudit.piercing==1&&SpearAttackAudit.stabs==1&&CounterAttackAudit.calls==0:CounterAttackAudit.calls==1,"Exactly one native damage path with visible hands");});
                c.waitTicks(kind.strongRecovery+2);c.runOnClient(client->GripAudit.begin());c.waitTicks(3);check(c.computeOnClient(client->GripAudit.main==0&&GripAudit.support==0),"No grip persists after recovery");
            }
            // A real held shield or other item keeps ownership of the other hand.
            for(var other:new Item[]{Items.SHIELD,Items.TORCH}){
                equip(w,id.get(),Items.IRON_SPEAR,new ItemStack(other));c.waitFor(client->client.player.getMainHandItem().is(Items.IRON_SPEAR)&&client.player.getOffhandItem().is(other));c.waitTicks(45);c.runOnClient(client->GripAudit.begin());
                c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).age()>=18&&MeleeActions.view(client.player).phase()==MeleeView.CHARGE);c.waitTicks(2);
                check(c.computeOnClient(client->GripAudit.main>0&&GripAudit.support==0&&client.player.getOffhandItem().is(other)),"Occupied hand is never borrowed or visually erased");c.takeScreenshot("c6c-occupied-"+other.toString().replace(':','-'));verifyOccupiedRig(c);
                c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.runOnClient(client->GripAudit.begin());c.waitTicks(3);check(c.computeOnClient(client->GripAudit.main==0&&GripAudit.support==0),"No first-person arms leak into third-person rendering");c.takeScreenshot("c6c-third-occupied-"+other.toString().replace(':','-'));c.runOnClient(client->client.options.setCameraType(CameraType.FIRST_PERSON));
                c.setScreen(()->new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));c.waitTicks(3);c.getInput().releaseKey(v->v.keyAttack);c.setScreen(()->null);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.IDLE);c.runOnClient(client->GripAudit.begin());c.waitTicks(3);check(c.computeOnClient(client->GripAudit.main==0),"GUI cancellation leaves no first-person arm lease");
            }
            // Actual synchronized left dominant hand, including native special-model and occupied-hand paths.
            w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setMainArm(HumanoidArm.LEFT));c.waitFor(client->client.player.getMainArm()==HumanoidArm.LEFT);
            for(var leftItem:new Item[]{Items.IRON_SPEAR,Items.TRIDENT}){
                equip(w,id.get(),leftItem,ItemStack.EMPTY);c.waitFor(client->client.player.getMainHandItem().is(leftItem)&&client.player.getOffhandItem().isEmpty());c.waitTicks(45);c.runOnClient(client->GripAudit.begin());c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=MeleeWeapon.of(new ItemStack(leftItem)).threshold);c.waitTicks(2);check(c.computeOnClient(client->GripAudit.main>0&&GripAudit.support==GripAudit.main),"Actual left GPU grip owns both free arms");c.takeScreenshot("c6c-left-"+leftItem.toString().replace(':','-'));
                c.setScreen(()->new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));c.waitTicks(3);c.getInput().releaseKey(v->v.keyAttack);c.setScreen(()->null);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.IDLE);
            }
            equip(w,id.get(),Items.IRON_SPEAR,new ItemStack(Items.TORCH));c.waitFor(client->client.player.getMainHandItem().is(Items.IRON_SPEAR)&&client.player.getOffhandItem().is(Items.TORCH));c.waitTicks(45);c.runOnClient(client->GripAudit.begin());c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=18);c.waitTicks(2);check(c.computeOnClient(client->GripAudit.main>0&&GripAudit.support==0),"Actual left main keeps occupied right hand");verifyOccupiedRig(c);c.takeScreenshot("c6c-left-occupied-torch");c.setScreen(()->new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));c.waitTicks(3);c.getInput().releaseKey(v->v.keyAttack);c.setScreen(()->null);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.IDLE);
            w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setMainArm(HumanoidArm.RIGHT));c.waitFor(client->client.player.getMainArm()==HumanoidArm.RIGHT);
            equip(w,id.get(),Items.TRIDENT,ItemStack.EMPTY);c.waitFor(client->client.player.getMainHandItem().is(Items.TRIDENT)&&client.player.getOffhandItem().isEmpty());c.waitTicks(45);c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);
            c.getInput().holdKey(v->v.keyUse);c.waitFor(client->client.player.isUsingItem());c.runOnClient(client->GripAudit.begin());c.waitTicks(3);check(c.computeOnClient(client->GripAudit.main==0&&GripAudit.support==0),"Native trident use immediately wins over synchronized melee pose");c.takeScreenshot("c6c-native-use-keeps-own-hands");c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);
            c.waitFor(client->!client.player.isUsingItem());equip(w,id.get(),Items.IRON_SWORD,ItemStack.EMPTY);c.waitFor(client->client.player.getMainHandItem().is(Items.IRON_SWORD));c.waitTicks(45);
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=12);c.getInput().holdKeyFor(v->v.keyJump,2);c.waitFor(client->!client.player.onGround());c.runOnClient(client->GripAudit.begin());c.getInput().releaseKey(v->v.keyAttack);c.waitFor(client->GripAudit.air>0);c.takeScreenshot("c6c-first-air-sword-grip");c.waitFor(client->client.player.onGround());c.waitTicks(20);
            equip(w,id.get(),Items.IRON_SWORD,ItemStack.EMPTY);c.waitTicks(25);c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=12);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,130,.5);p.setOnGround(false);});c.waitFor(client->client.player.getY()>124&&client.player.fallDistance>=2);c.runOnClient(client->GripAudit.begin());c.getInput().releaseKey(v->v.keyAttack);c.waitFor(client->GripAudit.drop>0);c.takeScreenshot("c6c-first-drop-sword-grip");
            System.out.println("C6C GRIP actual GPU skin arms for five native classes, one native attack, free/occupied support, third-person/GUI/native use, wide/slim left/right sleeves and balanced collector matrices PASS");
        }finally{GripAudit.enabled=false;c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);c.runOnClient(client->client.options.setCameraType(CameraType.FIRST_PERSON));}
    }
    private static void equip(TestSingleplayerContext w,int id,Item item,ItemStack other){w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();MeleeActions.reset(p);p.stopUsingItem();p.setYRot(0);p.setXRot(0);var host=new ItemStack(item);FusionItems.attach(host,new ItemStack(Items.COBBLESTONE),s.registryAccess());p.setItemInHand(InteractionHand.MAIN_HAND,host);p.setItemInHand(InteractionHand.OFF_HAND,other);PlayerStamina.fill(p);p.inventoryMenu.broadcastChanges();var m=(net.minecraft.world.entity.LivingEntity)s.overworld().getEntity(id);m.setHealth(200);m.setInvulnerableTime(0);m.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);m.setPos(.5,120,3.2);});}
    private static void matrix(ClientGameTestContext c,MeleeWeapon kind){c.runOnClient(client->{
        GripAudit.enabled=false;var avatar=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);var skin=avatar.skin;var hands=new FirstPersonHandsAndItemsRenderState();hands.mainHandItem=client.player.getMainHandItem();var original=hands.mainHandItem.copy();
        for(boolean slim:new boolean[]{false,true})for(var arm:HumanoidArm.values()){
            avatar.mainArm=arm;avatar.skin=net.minecraft.world.entity.player.PlayerSkin.insecure(skin.body(),null,null,slim?net.minecraft.world.entity.player.PlayerModelType.SLIM:net.minecraft.world.entity.player.PlayerModelType.WIDE);client.getItemModelResolver().updateForLiving(hands.mainHandRenderState,hands.mainHandItem,arm==HumanoidArm.RIGHT?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND,client.player);avatar.rightHandItemStack=arm==HumanoidArm.RIGHT?hands.mainHandItem:ItemStack.EMPTY;avatar.leftHandItemStack=arm==HumanoidArm.LEFT?hands.mainHandItem:ItemStack.EMPTY;
            for(boolean sleeves:new boolean[]{false,true}){avatar.showRightSleeve=avatar.showLeftSleeve=sleeves;
                var parts=new ArrayList<ModelPart>();var matrices=new ArrayList<Matrix4f>();SubmitNodeCollector collector=(SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),new Class<?>[]{SubmitNodeCollector.class},(o,m,args)->{if(m.getName().equals("submitModelPart")){parts.add((ModelPart)args[0]);var matrix=new Matrix4f(((PoseStack)args[1]).last().pose());check(matrix.isFinite(),"Finite native arm transform");matrices.add(matrix);check(((ModelPart)args[0]).getChild(arm==HumanoidArm.RIGHT&&parts.size()==1||arm==HumanoidArm.LEFT&&parts.size()==2?"right_sleeve":"left_sleeve").visible==sleeves,"Actual skin sleeve visibility");}return null;});
                var poses=new PoseStack();var before=new Matrix4f(poses.last().pose());MeleeHands.submit(avatar,hands,poses,collector);check(parts.size()==(kind==MeleeWeapon.SWORD?1:2)&&poses.last().pose().equals(before),"Actual geometry count and restored parent pose");check(ItemStack.isSameItemSameComponents(original,hands.mainHandItem),"Render never alters stack components");
                avatar.isInvisible=true;parts.clear();MeleeHands.submit(avatar,hands,poses,collector);check(parts.isEmpty(),"Invisible skin arms suppressed");avatar.isInvisible=false;
                for(var phase:new byte[]{MeleeView.SWING,MeleeView.AIR_SWING,MeleeView.DROP_SWING}){avatar.setData(MeleePoses.LAYER,new MeleeView(phase,kind.id(),true,2,kind.strongRecovery,(byte)0,0,10,0,0));check(MeleeHands.active(avatar,hands,InteractionHand.MAIN_HAND,hands.mainHandItem),"All owned attack phases eligible");}avatar.setData(MeleePoses.LAYER,MeleeActions.view(client.player));
            }
        }
        avatar.isUsingItem=true;check(!MeleeHands.active(avatar,hands,InteractionHand.MAIN_HAND,hands.mainHandItem),"Use wins before server cancellation arrives");avatar.isUsingItem=false;hands.isScoping=true;check(!MeleeHands.active(avatar,hands,InteractionHand.MAIN_HAND,hands.mainHandItem),"Scoping wins");hands.isScoping=false;avatar.isSpectator=true;check(!MeleeHands.active(avatar,hands,InteractionHand.MAIN_HAND,hands.mainHandItem),"Spectator wins");avatar.isSpectator=false;avatar.isAutoSpinAttack=true;check(!MeleeHands.active(avatar,hands,InteractionHand.MAIN_HAND,hands.mainHandItem),"Riptide wins");avatar.isAutoSpinAttack=false;check(!MeleeHands.active(avatar,hands,InteractionHand.MAIN_HAND,new ItemStack(Items.STICK)),"Wrong render item cannot inherit a transient weapon pose");check(!MeleeHands.active(avatar,hands,InteractionHand.OFF_HAND,hands.mainHandItem),"No duplicated offhand primary geometry");
        GripAudit.begin();
    });}
    private static void verifyOccupiedRig(ClientGameTestContext c){c.runOnClient(client->{var state=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);state.setData(CombatPoses.LAYER,null);state.setData(dev.wildcraft.client.render.CharacterPoses.POSE,null);state.currentSwing=null;
        var model=new net.minecraft.client.model.player.PlayerModel(net.minecraft.client.model.geom.builders.LayerDefinition.create(net.minecraft.client.model.player.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,false),64,64).bakeRoot(),false);var v=state.getData(MeleePoses.LAYER);state.setData(MeleePoses.LAYER,MeleeView.OFF);model.setupAnim(state);var support=state.mainArm==HumanoidArm.RIGHT?model.leftArm:model.rightArm;float x=support.xRot,y=support.yRot;state.setData(MeleePoses.LAYER,v);model.setupAnim(state);check(support.xRot==x&&support.yRot==y,"Third-person occupied arm keeps native held-item pose");});}
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
}
