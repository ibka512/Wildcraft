package dev.wildcraft.test;
import dev.wildcraft.client.animation.*;
import dev.wildcraft.client.combat.*;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.player.PlayerStamina;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual native key queues and separate piercing/stab audit, using the production mod. */
public final class SpearClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        var id=new AtomicInteger();var second=new AtomicInteger();var options=CombatOptions.get();int oldDodge=options.dodgeSkill;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            c.runOnClient(client->options.dodgeSkill=0);w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{for(int x=-6;x<=6;x++)for(int z=-4;z<=10;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);p.setYRot(0);p.setYHeadRot(0);p.setXRot(0);var host=new ItemStack(Items.IRON_SPEAR);FusionItems.attach(host,new ItemStack(Items.COBBLESTONE),s.registryAccess());p.setItemInHand(InteractionHand.MAIN_HAND,host);PlayerStamina.fill(p);p.inventoryMenu.broadcastChanges();var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,3.8);m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);m.setHealth(200);s.overworld().addFreshEntity(m);id.set(m.getId());});
            c.waitFor(client->client.player.getMainHandItem().is(Items.IRON_SPEAR)&&client.player.getY()>119&&ActionInput.tickFrame().gameplay());c.runOnClient(client->{client.player.setYRot(0);client.player.setXRot(0);client.options.setCameraType(CameraType.FIRST_PERSON);});c.waitTicks(45);
            w.getServer().runOnServer(s->SpearAttackAudit.begin(s.getPlayerList().getPlayers().getFirst()));
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);c.waitTicks(2);
            w.getServer().runOnServer(s->check(SpearAttackAudit.piercing==0&&SpearAttackAudit.nativePackets==0,"Press never also sends STAB"));c.getInput().releaseKey(v->v.keyAttack);c.waitFor(client->client.player.getMainHandItem().getDamageValue()==1);c.waitTicks(2);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpearAttackAudit.piercing==1&&SpearAttackAudit.stabs==1&&SpearAttackAudit.nativePackets==0&&CounterAttackAudit.calls==0&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"One native piercing operation rather than nearest Player.attack");var m=(LivingEntity)s.overworld().getEntity(id.get());m.setHealth(200);m.setInvulnerableTime(0);m.setPos(.5,120,3.8);m.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);var b=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);b.setPos(.5,120,4.7);b.setNoAi(true);b.setNoGravity(true);b.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);b.setHealth(200);s.overworld().addFreshEntity(b);second.set(b.getId());});
            c.takeScreenshot("c6b-first-native-spear-light");c.waitTicks(45);w.getServer().runOnServer(s->SpearAttackAudit.begin(s.getPlayerList().getPlayers().getFirst()));
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=18);c.takeScreenshot("c6b-first-spear-charge");verifyModelsAndProjection(c);
            c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.waitTicks(2);c.takeScreenshot("c6b-third-spear-charge");c.getInput().releaseKey(v->v.keyAttack);c.waitFor(client->client.player.getMainHandItem().getDamageValue()==3);c.waitTicks(2);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpearAttackAudit.piercing==1&&SpearAttackAudit.stabs==2&&SpearAttackAudit.nativePackets==0&&CounterAttackAudit.calls==0&&p.getMainHandItem().get(FusionContent.DATA).remaining()==29&&PlayerStamina.get(p).stamina()==84,"One charged gesture pierces two, native per-target wear/Fuse, one fee");s.overworld().getEntity(second.get()).discard();var m=(net.minecraft.world.entity.monster.zombie.Husk)s.overworld().getEntity(id.get());m.setHealth(200);m.setInvulnerableTime(0);m.setPos(.5,120,3.8);m.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);m.setTarget(p);});
            c.takeScreenshot("c6b-two-collinear-targets");c.waitTicks(30);c.runOnClient(client->client.options.setCameraType(CameraType.FIRST_PERSON));
            // Direct qualified opportunity fixture; earned real dodge is separately regressed by C5.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.resetAttackStrengthTicker();SpearAttackAudit.begin(p);SkillPreferences.receive(p,new SkillIntent(SkillIntent.PREFERENCES,(byte)0,(byte)0));check(ReactionTime.reward(p,(LivingEntity)s.overworld().getEntity(id.get())),"Qualified singleplayer time fixture");});
            c.waitFor(client->ReactionTime.active(client.player));c.waitTicks(4);c.takeScreenshot("c6b-spear-counter-real-time-recovery");
            c.runOnClient(client->{KeyMapping.click(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(client.options.keyAttack));client.options.keyAttack.setDown(true);ActionInput.frame(client);SkillControls.frame(client);client.options.keyAttack.setDown(false);ActionInput.frame(client);SkillControls.frame(client);});
            c.waitFor(client->!ReactionTime.active(client.player));c.waitTicks(3);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpearAttackAudit.piercing==1&&SpearAttackAudit.stabs==1&&SpearAttackAudit.nativePackets==0&&CounterAttackAudit.calls==0&&p.getMainHandItem().getDamageValue()==4&&p.getMainHandItem().get(FusionContent.DATA).remaining()==28,"Frame counter at native spear range, restored real-time cooldown, one dedicated call and no leaked STAB queue");});
            c.takeScreenshot("c6b-spear-counter-once");c.waitTicks(40);
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);c.getInput().holdKey(v->v.keyUse);c.waitFor(client->client.player.isUsingItem()&&MeleeActions.view(client.player).phase()==MeleeView.IDLE);c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(15);c.takeScreenshot("c6b-native-kinetic-use-cancels-charge");c.getInput().releaseKey(v->v.keyUse);c.waitTicks(3);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(1,new ItemStack(Items.STICK));p.getInventory().setSelectedSlot(1);p.inventoryMenu.broadcastChanges();});c.runOnClient(client->{client.player.getInventory().setSelectedSlot(1);client.options.setCameraType(CameraType.THIRD_PERSON_BACK);});c.waitTicks(3);c.takeScreenshot("c6b-back-native-spear");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));p.inventoryMenu.broadcastChanges();});c.waitTicks(3);c.takeScreenshot("c6b-elytra-waist-spear");
            System.out.println("C6B SPEAR native key queues, min/max piercing, two-target wear/Fuse, independent charge, native use, real-time frame counter and separate primary/stab/STAB audits, rig/head/armor/projection PASS");
        }finally{c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);c.runOnClient(client->{options.dodgeSkill=oldDodge;client.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
    private static void verifyModelsAndProjection(ClientGameTestContext c){c.runOnClient(client->{
        var state=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);check(state.getData(MeleePoses.LAYER).kind()==MeleeWeapon.SPEAR,"Live synchronized spear kind");state.setData(CombatPoses.LAYER,null);state.setData(dev.wildcraft.client.render.CharacterPoses.POSE,null);state.currentSwing=null;state.yRot=45;
        for(boolean slim:new boolean[]{false,true})for(var hand:HumanoidArm.values()){state.mainArm=hand;state.rightHandItemStack=hand==HumanoidArm.RIGHT?client.player.getMainHandItem():ItemStack.EMPTY;state.leftHandItemStack=hand==HumanoidArm.LEFT?client.player.getMainHandItem():ItemStack.EMPTY;var model=new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);model.setupAnim(state);check(Math.abs(model.head.yRot-Math.PI/4)<.001&&model.leftArm.xRot<-.7&&model.rightArm.xRot<-.7,"Both-rig/hand spear mask keeps head aim");var meshes=HumanoidModel.createArmorMeshSet(new CubeDeformation(1),new CubeDeformation(.5F));for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){var armor=new HumanoidModel<AvatarRenderState>(LayerDefinition.create(meshes.get(slot),64,32).bakeRoot());armor.setupAnim(state);check(Math.abs(armor.leftArm.xRot-model.leftArm.xRot)<.001&&Math.abs(armor.body.xRot-model.body.xRot)<.001,"Armor consumes mask once");}}
        for(var context:new ItemDisplayContext[]{ItemDisplayContext.GUI,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_LEFT_HAND,ItemDisplayContext.FIXED}){var render=new net.minecraft.client.renderer.item.ItemStackRenderState();client.getItemModelResolver().updateForLiving(render,client.player.getMainHandItem(),context,client.player);boolean found=false;for(var layer:((dev.wildcraft.mixin.client.FusionStateAccessor)render).wildcraft$layers()){if(layer==null||!(((dev.wildcraft.mixin.client.FusionLayerAccessor)layer).wildcraft$special() instanceof dev.wildcraft.client.fuse.FusionItemRenderer))continue;try{var field=layer.getClass().getDeclaredField("argumentForSpecialRendering");field.setAccessible(true);var part=(dev.wildcraft.client.fuse.FusionItemRenderer.Part)field.get(layer);boolean hand=context.firstPerson()||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND;check(part.y()>.7&&(hand?part.x()<.25:part.x()>.65),"Native hand texture points upper-left; inventory points upper-right");found=true;}catch(ReflectiveOperationException e){throw new AssertionError(e);}}check(found,"Actual material model attached in "+context);}
    });}
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
}
