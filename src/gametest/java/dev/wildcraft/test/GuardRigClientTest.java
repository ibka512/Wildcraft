package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
public final class GuardRigClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Skin fixture");}
    @Override public void runTest(ClientGameTestContext c){var id=new AtomicInteger();var opts=CombatOptions.get();boolean hold=opts.hold;int follow=opts.follow;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));p.inventoryMenu.broadcastChanges();var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setNoAi(true);m.setNoGravity(true);m.setPos(.5,120,2);s.overworld().addFreshEntity(m);id.set(m.getId());});
            c.waitFor(v->v.player.getY()>119&&v.level.getEntity(id.get())!=null&&v.player.getOffhandItem().is(Items.SHIELD));c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.hold=false;opts.follow=0;v.player.setYRot(0);v.player.setXRot(0);v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);v.options.guiScale().set(2);});c.waitFor(v->ActionInput.tickFrame().gameplay());c.getInput().holdKeyFor(WildcraftBindings.LOCK_TARGET,1);c.waitFor(v->Combat.locked(v.player));
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values()){
                c.getInput().releaseKey(WildcraftBindings.DEFEND);c.waitTicks(16);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setMainArm(arm);p.setHealth(20);p.setInvulnerableTime(0);p.setYRot(0);p.setYHeadRot(0);PlayerStamina.fill(p);var shield=new ItemStack(Items.SHIELD);shield.set(net.minecraft.core.component.DataComponents.BASE_COLOR,DyeColor.BLUE);shield.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);check(FusionItems.attach(shield,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Real shield Fuse fixture");p.setItemInHand(InteractionHand.OFF_HAND,shield);p.inventoryMenu.broadcastChanges();ParryActions.reset(p);CounterAttackAudit.begin(p);s.overworld().getEntity(id.get()).teleportTo(.5,120,2);});c.waitFor(v->v.player.getMainArm()==arm&&ParryActions.view(v.player).serial()==0);c.runOnClient(v->{RigAudit.fixture=skin(type);RigAudit.begin();GuardRigAudit.begin();});c.waitTicks(5);
                c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->Combat.view(v.player).defending());w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();((Husk)s.overworld().getEntity(id.get())).doHurtTarget(s.overworld(),p);check(ParryActions.view(p).serial()==1&&p.getHealth()==20,"Actual fresh native front block succeeds once");check(p.getOffhandItem().getDamageValue()>0&&p.getOffhandItem().get(FusionContent.DATA).remaining()==31,"Actual shield wear/Fuse settles once");check(CounterAttackAudit.calls==0&&PlayerStamina.get(p).stamina()==100,"Source pulse adds no counterattack or stamina cost");s.overworld().getEntity(id.get()).teleportTo(.5,120,7.5);});
                c.waitFor(v->v.level.getEntity(id.get()).getZ()>6&&state(v).getData(GuardRig.PULSE)!=null&&GuardRigAudit.frames>0&&GuardRigAudit.activeDeviation>.005&&RigAudit.shields>0&&RigAudit.bodies>0);c.takeScreenshot("guard-rig-real-success-"+type+"-"+arm);check(c.computeOnClient(v->RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Actual skin texture reaches GPU");c.waitTicks(10);check(c.computeOnClient(v->state(v).getData(GuardRig.PULSE)==null&&state(v).getData(CombatRigLayer.FRAME)!=null),"Pulse ends after actual eight-tick event and guard body remains");c.takeScreenshot("guard-rig-return-to-current-guard-"+type+"-"+arm);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();int wear=p.getOffhandItem().getDamageValue();s.overworld().getEntity(id.get()).teleportTo(.5,120,2);p.setInvulnerableTime(0);((Husk)s.overworld().getEntity(id.get())).doHurtTarget(s.overworld(),p);check(ParryActions.view(p).serial()==1&&p.getHealth()==20&&p.getOffhandItem().getDamageValue()>wear,"Ordinary held native block keeps wear and does not re-trigger source success");s.overworld().getEntity(id.get()).teleportTo(.5,120,7.5);});c.waitTicks(2);check(c.computeOnClient(v->state(v).getData(GuardRig.PULSE)==null),"Ordinary block does not invent pulse");
            }
            c.getInput().releaseKey(WildcraftBindings.DEFEND);c.waitTicks(16);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getMainHandItem().isEmpty());c.getInput().holdKey(WildcraftBindings.DEFEND);c.waitFor(v->Combat.view(v.player).defending());w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();s.overworld().getEntity(id.get()).teleportTo(.5,120,2);p.setInvulnerableTime(0);((Husk)s.overworld().getEntity(id.get())).doHurtTarget(s.overworld(),p);s.overworld().getEntity(id.get()).teleportTo(.5,120,7.5);});c.waitFor(v->ParryActions.view(v.player).remaining()>0);check(c.computeOnClient(v->state(v).getData(GuardRig.PULSE)==null&&state(v).getData(CombatRigLayer.FRAME)==null),"Unsupported primary hand retains existing actual guard path");c.takeScreenshot("guard-rig-native-empty-primary");
            System.out.println("C6K actual successful native guard, wide/slim left/right actual dyed glint Fuse shield/skin GPU, active-arm-only matrices, eight-tick exit/ordinary block/no attack-cost/unsupported hand PASS");
        }finally{c.getInput().releaseKey(WildcraftBindings.DEFEND);c.runOnClient(v->{GuardRigAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;opts.hold=hold;opts.follow=follow;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
