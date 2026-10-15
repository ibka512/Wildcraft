package dev.wildcraft.test;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.combat.*;
import dev.wildcraft.client.animation.*;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class MeleeClientTest implements FabricClientGameTest {
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
    private static void reset(TestSingleplayerContext w,int id){w.getServer().runOnServer(s->{var m=(LivingEntity)s.overworld().getEntity(id);m.setHealth(20);m.setInvulnerableTime(0);m.setPos(.5,120,2.5);m.setDeltaMovement(Vec3.ZERO);var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);p.setYRot(0);p.setXRot(0);});}
    @Override public void runTest(ClientGameTestContext c){
        var id=new AtomicInteger();var options=CombatOptions.get();boolean oldJump=options.jumpDodge;int oldFollow=options.follow;boolean oldHold=options.hold;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                for(int x=-12;x<=12;x++)for(int z=-8;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                p.teleportTo(.5,120,.5);p.setYRot(0);p.setXRot(0);p.setHealth(20);PlayerStamina.fill(p);
                var sword=new ItemStack(Items.IRON_SWORD);FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),s.registryAccess());p.setItemInHand(InteractionHand.MAIN_HAND,sword);
                var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,2.5);m.setNoAi(true);m.setNoGravity(true);s.overworld().addFreshEntity(m);id.set(m.getId());p.inventoryMenu.broadcastChanges();});
            c.waitFor(client->client.player.getY()>119&&client.player.getMainHandItem().is(Items.IRON_SWORD)&&client.level.getEntity(id.get())!=null&&ActionInput.tickFrame().gameplay());
            c.runOnClient(client->{options.jumpDodge=false;options.follow=0;options.hold=false;client.options.setCameraType(CameraType.FIRST_PERSON);client.player.setYRot(0);client.player.setXRot(0);WildcraftBindings.DODGE.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K));KeyMapping.resetMapping();});
            c.waitFor(client->client.hitResult instanceof EntityHitResult e&&e.getEntity().getId()==id.get());c.waitTicks(25);
            c.getInput().holdKey(WildcraftBindings.DEFEND);
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);c.waitTicks(2);
            c.getInput().releaseKey(WildcraftBindings.DEFEND);
            w.getServer().runOnServer(s->check(((LivingEntity)s.overworld().getEntity(id.get())).getHealth()==20,"Native press does not also hit before release"));
            c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(3);
            c.waitFor(client->client.player.getMainHandItem().getDamageValue()==1);c.waitTicks(3);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getMainHandItem().getDamageValue()==1&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"One normal release equals one native wear and Fuse use");check(PlayerStamina.get(p).stamina()==100,"Ordinary hit has no added fee");});
            c.takeScreenshot("c3-first-person-native-light-hit");reset(w,id.get());c.waitTicks(25);
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=12);
            c.takeScreenshot("c3-first-person-sword-charge");c.getInput().releaseKey(v->v.keyAttack);
            c.waitFor(client->client.player.getMainHandItem().getDamageValue()==2);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var m=(LivingEntity)s.overworld().getEntity(id.get());check(20-m.getHealth()>=9&&p.getMainHandItem().get(FusionContent.DATA).remaining()==30,"Paid native sword hit boosts damage and consumes exactly one remaining use");check(PlayerStamina.get(p).stamina()==88,"Sword activation charges once");});
            reset(w,id.get());c.waitTicks(25);
            c.runOnClient(client->{client.options.keyAttack.setDown(true);ActionInput.frame(client);client.options.keyAttack.setDown(false);ActionInput.frame(client);});
            c.waitFor(client->client.player.getMainHandItem().getDamageValue()==3);c.waitTicks(3);
            w.getServer().runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getMainHandItem().getDamageValue()==3,"A complete between-tick attack edge yields one hit"));
            reset(w,id.get());c.waitTicks(25);c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);
            c.setScreen(()->new CombatSettingsScreen(null));c.waitTicks(3);
            c.runOnClient(client->{client.options.keyAttack.setDown(true);});c.waitTicks(2);c.setScreen(()->null);c.waitTicks(3);
            w.getServer().runOnServer(s->check(((LivingEntity)s.overworld().getEntity(id.get())).getHealth()==20,"Screen interruption and held return cannot attack"));c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(3);
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));p.inventoryMenu.broadcastChanges();});
            c.waitFor(client->client.player.getMainHandItem().is(Items.IRON_AXE)&&MeleeActions.view(client.player).phase()==MeleeView.IDLE);c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(25);
            w.getServer().runOnServer(s->check(((LivingEntity)s.overworld().getEntity(id.get())).getHealth()==20,"Changing weapon cancels instead of releasing a hidden hit"));
            w.getServer().runOnServer(s->PlayerStamina.fill(s.getPlayerList().getPlayers().getFirst()));
            c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE&&MeleeActions.view(client.player).age()>=20);
            c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.waitTicks(3);c.takeScreenshot("c3-third-person-axe-charge");verifyModels(c);
            c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(3);
            c.waitFor(client->client.player.getMainHandItem().getDamageValue()==2);
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(20-((LivingEntity)s.overworld().getEntity(id.get())).getHealth()>15&&PlayerStamina.get(p).stamina()==82,"Axe charge uses distinct damage and one 18-energy fee");});
            // Native block mining owns a press begun on a block, even with a combat weapon.
            c.waitTicks(20);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,-3.5);s.overworld().setBlockAndUpdate(new BlockPos(0,121,-2),Blocks.OAK_LOG.defaultBlockState());});
            c.runOnClient(client->{client.options.setCameraType(CameraType.FIRST_PERSON);client.player.setYRot(0);client.player.setXRot(0);});
            c.waitFor(client->client.hitResult instanceof BlockHitResult b&&b.getBlockPos().equals(new BlockPos(0,121,-2)));c.getInput().holdKey(v->v.keyAttack);
            c.waitFor(client->client.level.getBlockState(new BlockPos(0,121,-2)).isAir(),160);c.getInput().releaseKey(v->v.keyAttack);
            check(c.computeOnClient(client->MeleeActions.view(client.player).phase()==MeleeView.IDLE),"Axe mining never prepares a combat attack");c.takeScreenshot("c3-native-axe-mining");
            // Locked direction is independent of aim. A real short hop must collide with a wall.
            reset(w,id.get());w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();PlayerStamina.fill(p);for(int y=120;y<=123;y++)s.overworld().setBlockAndUpdate(new BlockPos(1,y,0),Blocks.STONE.defaultBlockState());});
            c.runOnClient(client->{client.player.setYRot(0);client.player.setXRot(0);});c.waitTicks(5);c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(client->Combat.locked(client.player));
            c.runOnClient(client->{client.player.setYRot(90);client.options.setCameraType(CameraType.FIRST_PERSON);});c.waitTicks(3);c.getInput().holdKey(v->v.keyLeft);c.getInput().holdKeyFor(WildcraftBindings.DODGE,1);
            c.waitFor(client->MeleeActions.hopping(client.player));c.takeScreenshot("c3-locked-side-hop-wall");c.getInput().releaseKey(v->v.keyLeft);c.waitTicks(15);
            check(c.computeOnClient(client->client.player.getBoundingBox().maxX<=1.001&&Math.abs(client.player.getYRot()-90)<.1),"Side hop stops at native wall while free aim stays independent");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getX()<.71&&PlayerStamina.get(p).stamina()>=92,"Server accepts bounded collision and one hop cost");for(int y=120;y<=123;y++)s.overworld().setBlockAndUpdate(new BlockPos(1,y,0),Blocks.AIR.defaultBlockState());});
            c.runOnClient(client->{client.options.setCameraType(CameraType.THIRD_PERSON_BACK);client.player.setYRot(0);});c.waitTicks(3);
            double z=c.computeOnClient(client->client.player.getZ());c.getInput().holdKey(v->v.keyDown);c.getInput().holdKeyFor(WildcraftBindings.DODGE,1);c.waitFor(client->MeleeActions.hopping(client.player));c.takeScreenshot("c3-third-person-back-hop");c.getInput().releaseKey(v->v.keyDown);c.waitTicks(12);
            check(c.computeOnClient(client->client.player.getZ()<z-.8),"Back hop creates actual separation from the locked target");
            // Optional direction+jump has one owner; normal jump remains native when disabled.
            c.waitFor(client->client.player.onGround());c.waitTicks(8);c.runOnClient(client->options.jumpDodge=true);
            c.getInput().holdKey(v->v.keyRight);c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(client->MeleeActions.hopping(client.player));c.getInput().releaseKey(v->v.keyRight);c.waitTicks(15);
            c.runOnClient(client->options.jumpDodge=false);c.waitFor(client->client.player.onGround());
            c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(client->!client.player.onGround());check(c.computeOnClient(client->!MeleeActions.hopping(client.player)),"Disabled optional mapping keeps ordinary jump native");c.waitFor(client->client.player.onGround());
            // A held independent shield resumes after a hop without a fresh defense press.
            c.waitTicks(15);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));PlayerStamina.fill(p);p.inventoryMenu.broadcastChanges();});
            c.waitFor(client->client.player.getOffhandItem().is(Items.SHIELD));c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(client->Combat.view(client.player).defending());
            c.getInput().holdKeyFor(WildcraftBindings.DODGE,1);c.waitFor(client->MeleeActions.hopping(client.player));
            check(c.computeOnClient(client->!client.player.isUsingItem()),"Hop temporarily releases actual shield use");
            c.waitFor(client->!MeleeActions.hopping(client.player)&&Combat.view(client.player).defending()&&client.player.isUsingItem());
            c.getInput().releaseKey(WildcraftBindings.DEFEND);c.waitFor(client->!client.player.isUsingItem());
            // Ordinary damage still lands during a hop; no added invulnerability.
            c.waitTicks(15);c.getInput().holdKeyFor(WildcraftBindings.DODGE,1);c.waitFor(client->MeleeActions.hopping(client.player));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setInvulnerableTime(0);float health=p.getHealth();p.hurtServer(s.overworld(),p.damageSources().mobAttack((LivingEntity)s.overworld().getEntity(id.get())),2);check(p.getHealth()<health,"Hop never grants universal invulnerability");});
            c.takeScreenshot("c3-back-hop-real-damage");
            c.waitTicks(15);c.getInput().holdKeyFor(WildcraftBindings.SETTINGS,1);c.waitFor(client->client.gui.screen()!=null);c.setScreen(()->new CombatSettingsScreen(null));c.takeScreenshot("c3-combat-settings-directional-jump");c.setScreen(()->null);
            System.out.println("C3 MELEE native press/release/charge, sub-tick one hit/wear/Fuse, GUI/equipment cancellation, axe mining, real locked hop/wall/back/jump/fees/no invulnerability and both-rig/hand armor masks PASS");
        }finally{
            c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyLeft);c.getInput().releaseKey(v->v.keyRight);c.getInput().releaseKey(v->v.keyDown);c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(WildcraftBindings.DODGE);c.getInput().releaseKey(WildcraftBindings.DEFEND);
            c.runOnClient(client->{var o=CombatOptions.get();o.jumpDodge=oldJump;o.follow=oldFollow;o.hold=oldHold;CombatOptions.save();WildcraftBindings.DODGE.setKey(WildcraftBindings.DODGE.getDefaultKey());KeyMapping.resetMapping();client.options.setCameraType(CameraType.FIRST_PERSON);});
        }
    }
    private static void verifyModels(ClientGameTestContext c){c.runOnClient(client->{
        var state=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);var v=state.getData(MeleePoses.LAYER);check(v!=null&&v.phase()==MeleeView.CHARGE,"Live authoritative phase reaches third-person renderer");
        state.setData(CombatPoses.LAYER,null);state.setData(dev.wildcraft.client.render.CharacterPoses.POSE,null);state.currentSwing=null;state.yRot=50;var skin=state.skin;
        for(boolean slim:new boolean[]{false,true})for(var hand:HumanoidArm.values()){
            state.mainArm=hand;state.skin=PlayerSkin.insecure(skin.body(),null,null,slim?PlayerModelType.SLIM:PlayerModelType.WIDE);
            var model=new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);model.setupAnim(state);
            check((hand==HumanoidArm.RIGHT?model.rightArm:model.leftArm).xRot<-2.2&&Math.abs(model.head.yRot-Math.toRadians(50))<.01,"Axe preparation changes only weapon arm and torso, preserving free head aim");
            var meshes=HumanoidModel.createArmorMeshSet(new CubeDeformation(1),new CubeDeformation(.5F));for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                var armor=new HumanoidModel<AvatarRenderState>(LayerDefinition.create(meshes.get(slot),64,32).bakeRoot());armor.setupAnim(state);
                check(Math.abs(armor.body.yRot-model.body.yRot)<.001&&Math.abs((hand==HumanoidArm.RIGHT?armor.rightArm:armor.leftArm).xRot-(hand==HumanoidArm.RIGHT?model.rightArm:model.leftArm).xRot)<.001,"Native armor and player masks match once: "+slot);
            }
            // Bounded action masks and armor match across swing, side/back hop and hurt overlays.
            state.setData(MeleePoses.PARTIAL,0F);
            for(var action:new MeleeView[]{new MeleeView(MeleeView.SWING,false,false,2,5,(byte)0,0,20,0,0),new MeleeView(MeleeView.SWING,true,true,6,16,(byte)0,0,21,0,0),
                    new MeleeView(MeleeView.HOP,false,false,3,6,(byte)1,0,22,0,0),new MeleeView(MeleeView.HOP,false,false,3,6,(byte)-1,0,23,0,0),new MeleeView(MeleeView.HOP,false,false,3,6,(byte)-2,0,24,0,0),
                    new MeleeView(MeleeView.IDLE,false,false,0,0,(byte)0,0,25,3,90)}){
                state.setData(MeleePoses.LAYER,MeleeView.OFF);model.setupAnim(state);float head=model.head.yRot,leg=model.leftLeg.xRot,body=model.body.zRot;
                state.setData(MeleePoses.LAYER,action);model.setupAnim(state);
                check(Math.abs(model.head.yRot-head)<.001,"All action overlays preserve independent head aim");
                if(action.phase()==MeleeView.HOP)check(Math.abs(model.leftLeg.xRot-leg)>.4,"Hop mask changes lower body");
                if(action.hurt()>0)check(Math.abs(model.body.zRot-body)>.05,"Directional hurt is a small torso overlay");
                var armor=new HumanoidModel<AvatarRenderState>(LayerDefinition.create(meshes.chest(),64,32).bakeRoot());armor.setupAnim(state);
                check(Math.abs(armor.body.zRot-model.body.zRot)<.001&&Math.abs(armor.leftLeg.xRot-model.leftLeg.xRot)<.001,"Action and hurt masks reach armor exactly once");
            }
            state.setData(MeleePoses.LAYER,v);
        }state.skin=skin;
    });}
}
