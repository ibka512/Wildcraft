package dev.wildcraft.test;

import dev.wildcraft.client.animation.*;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.client.render.BackEquipmentLayer;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.player.PlayerStamina;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
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

/** Packaged production native key input, synchronized profiles, native item models and armor masks. */
public final class WeaponClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        var id=new AtomicInteger();
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{for(int x=-5;x<=5;x++)for(int z=-4;z<=8;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,2.5);m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);m.setHealth(200);s.overworld().addFreshEntity(m);id.set(m.getId());});
            c.waitFor(client->client.player.getY()>119&&client.level.getEntity(id.get())!=null&&ActionInput.tickFrame().gameplay());
            c.runOnClient(client->{client.player.setYRot(0);client.player.setXRot(0);client.options.setCameraType(CameraType.FIRST_PERSON);});
            for(var kind:new MeleeWeapon[]{MeleeWeapon.TRIDENT,MeleeWeapon.MACE}){
                var item=kind==MeleeWeapon.TRIDENT?Items.TRIDENT:Items.MACE;
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();MeleeActions.reset(p);var host=new ItemStack(item);FusionItems.attach(host,new ItemStack(Items.COBBLESTONE),s.registryAccess());p.setItemInHand(InteractionHand.MAIN_HAND,host);PlayerStamina.fill(p);p.inventoryMenu.broadcastChanges();var m=(LivingEntity)s.overworld().getEntity(id.get());m.setHealth(200);m.setInvulnerableTime(0);m.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);m.setPos(.5,120,2.5);});
                c.waitFor(client->client.player.getMainHandItem().is(item));c.waitTicks(45);
                c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);c.waitTicks(2);
                check(c.computeOnClient(client->MeleeActions.view(client.player).kind()==kind),"Authoritative kind reaches the packaged client");
                w.getServer().runOnServer(s->check(((LivingEntity)s.overworld().getEntity(id.get())).getHealth()==200,"Native press never also attacks"));
                c.getInput().releaseKey(v->v.keyAttack);c.waitFor(client->client.player.getMainHandItem().getDamageValue()==1);c.waitTicks(3);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getMainHandItem().getDamageValue()==1&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31&&PlayerStamina.get(p).stamina()==100,"One native light input / wear / Fuse, no fee");var m=(LivingEntity)s.overworld().getEntity(id.get());m.setHealth(200);m.setInvulnerableTime(0);m.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);m.setPos(.5,120,2.5);});
                c.takeScreenshot("c6-"+kind.name().toLowerCase()+"-first-light");c.waitTicks(45);
                c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=kind.threshold);
                c.takeScreenshot("c6-"+kind.name().toLowerCase()+"-first-charge");verifyModels(c,kind);verifyFusionAnchors(c,kind);
                c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.waitTicks(2);c.takeScreenshot("c6-"+kind.name().toLowerCase()+"-third-charge");
                c.getInput().releaseKey(v->v.keyAttack);c.waitFor(client->client.player.getMainHandItem().getDamageValue()==2);c.waitTicks(2);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getMainHandItem().getDamageValue()==2&&p.getMainHandItem().get(FusionContent.DATA).remaining()==30&&PlayerStamina.get(p).stamina()==100-kind.cost,"One actual paid strong release");p.getInventory().setItem(1,new ItemStack(Items.STICK));p.getInventory().setSelectedSlot(1);p.inventoryMenu.broadcastChanges();});
                c.runOnClient(client->client.player.getInventory().setSelectedSlot(1));
                c.waitFor(client->client.player.getMainHandItem().is(Items.STICK));c.waitTicks(3);
                check(c.computeOnClient(client->{var state=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);var items=state.getData(BackEquipmentLayer.ITEMS);return items!=null&&!items[0].isEmpty();}),"Recorded long/heavy fused native model renders on back");
                c.takeScreenshot("c6-"+kind.name().toLowerCase()+"-back-equipment");
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));p.inventoryMenu.broadcastChanges();});c.waitTicks(3);
                c.takeScreenshot("c6-"+kind.name().toLowerCase()+"-elytra-waist");
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();});
                c.runOnClient(client->{client.player.getInventory().setSelectedSlot(0);client.options.setCameraType(CameraType.FIRST_PERSON);});c.waitTicks(45);
            }
            // Actual USE must still enter native trident throwing, cancelling a prepared melee phase.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.TRIDENT));p.inventoryMenu.broadcastChanges();});c.waitFor(client->client.player.getMainHandItem().is(Items.TRIDENT));c.waitTicks(45);
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);
            c.getInput().holdKey(v->v.keyUse);c.waitFor(client->client.player.isUsingItem()&&MeleeActions.view(client.player).phase()==MeleeView.IDLE);c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(12);
            c.takeScreenshot("c6-native-trident-use");c.getInput().releaseKey(v->v.keyUse);c.waitFor(client->client.player.getMainHandItem().isEmpty());c.takeScreenshot("c6-native-trident-thrown");
            System.out.println("C6A WEAPONS native keys, distinct charge/cost, one wear/Fuse, native throw, back/elytra, both rigs/hands/head/armor masks PASS");
        }
    }
    private static void verifyFusionAnchors(ClientGameTestContext c,MeleeWeapon kind){c.runOnClient(client->{
        for(var context:new ItemDisplayContext[]{ItemDisplayContext.GUI,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_LEFT_HAND,ItemDisplayContext.FIXED}){
            var state=new net.minecraft.client.renderer.item.ItemStackRenderState();client.getItemModelResolver().updateForLiving(state,client.player.getMainHandItem(),context,client.player);
            var layers=((dev.wildcraft.mixin.client.FusionStateAccessor)state).wildcraft$layers();boolean nativeTrident=((dev.wildcraft.mixin.client.FusionLayerAccessor)layers[0]).wildcraft$special() instanceof net.minecraft.client.renderer.special.TridentSpecialRenderer;int parts=0;
            for(var layer:layers){if(layer==null||!(((dev.wildcraft.mixin.client.FusionLayerAccessor)layer).wildcraft$special() instanceof dev.wildcraft.client.fuse.FusionItemRenderer))continue;
                try{var field=layer.getClass().getDeclaredField("argumentForSpecialRendering");field.setAccessible(true);var part=(dev.wildcraft.client.fuse.FusionItemRenderer.Part)field.get(layer);parts++;
                    var bounds=part.model().getModelBoundingBox();double size=Math.max(bounds.getXsize(),Math.max(bounds.getYsize(),bounds.getZsize()));
                    check(Float.isFinite(part.scale())&&part.scale()>0&&size*part.scale()<=.31F,"Bounded material model in each native display context");
                    if(nativeTrident)check(Math.abs(part.x())<.16&&part.y()>-.25&&part.y()<.125&&Math.abs(part.z())<.2,"Material lies on the actual native trident fork, not generic sprite coordinates");
                }catch(ReflectiveOperationException e){throw new AssertionError("Test-only inspection of native material argument",e);}
            }
            check(parts==1,"Exactly one appended native material model for "+kind+" / "+context);
        }
    });}
    private static void verifyModels(ClientGameTestContext c,MeleeWeapon kind){c.runOnClient(client->{
        var state=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);
        check(state.getData(MeleePoses.LAYER).kind()==kind,"Live extraction carries new profile");state.setData(CombatPoses.LAYER,null);state.setData(dev.wildcraft.client.render.CharacterPoses.POSE,null);state.currentSwing=null;state.yRot=50;
        for(boolean slim:new boolean[]{false,true})for(var hand:HumanoidArm.values()){
            state.mainArm=hand;state.rightHandItemStack=hand==HumanoidArm.RIGHT?client.player.getMainHandItem():ItemStack.EMPTY;state.leftHandItemStack=hand==HumanoidArm.LEFT?client.player.getMainHandItem():ItemStack.EMPTY;var model=new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);
            state.setData(MeleePoses.LAYER,MeleeView.OFF);model.setupAnim(state);float head=model.head.yRot;
            for(byte phase:new byte[]{MeleeView.CHARGE,MeleeView.SWING,MeleeView.AIR_SWING,MeleeView.DROP_SWING}){
                state.setData(MeleePoses.LAYER,new MeleeView(phase,kind.id(),true,phase==MeleeView.CHARGE?kind.threshold:kind.strongRecovery/2,phase==MeleeView.CHARGE?kind.threshold:kind.strongRecovery,(byte)0,0,40,0,0));state.setData(MeleePoses.PARTIAL,0F);model.setupAnim(state);
                check(Math.abs(model.head.yRot-head)<.001,"Profile preserves native independent head aim");
                if(phase==MeleeView.CHARGE)check(model.rightArm.xRot<-.7&&model.leftArm.xRot<-.7,"Both arms participate for left/right and wide/slim");
                var meshes=HumanoidModel.createArmorMeshSet(new CubeDeformation(1),new CubeDeformation(.5F));
                for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){var armor=new HumanoidModel<AvatarRenderState>(LayerDefinition.create(meshes.get(slot),64,32).bakeRoot());armor.setupAnim(state);check(Math.abs(armor.body.xRot-model.body.xRot)<.001&&Math.abs(armor.leftArm.xRot-model.leftArm.xRot)<.001&&Math.abs(armor.rightArm.xRot-model.rightArm.xRot)<.001,"Native armor shares one profile: "+slot);}
            }
        }
    });}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
