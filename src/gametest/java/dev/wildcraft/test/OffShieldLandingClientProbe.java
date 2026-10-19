package dev.wildcraft.test;

import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.fuse.*;
import dev.wildcraft.player.*;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.Gliding;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Genuine off-shield use, real glider holstering and natural collision landing; observation only. */
public final class OffShieldLandingClientProbe implements FabricClientGameTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var value=DefaultPlayerSkin.get(new UUID(0,i));if(value.model()==type)return value;}throw new AssertionError("Skin fixture");}
    private static void await(ClientGameTestContext c,String phase,java.util.function.Predicate<Minecraft> predicate){
        try{c.waitFor(v->predicate.test(v));}catch(AssertionError error){c.runOnClient(v->{OffShieldLandingAudit.save("FAILED-"+phase);System.out.println("PROBE_FAILURE offshield phase="+phase+" y="+v.player.getY()+" ground="+v.player.onGround()+" active="+Gliding.active(v.player)+" intent="+Gliding.intent(v.player)+" view="+v.player.getAttached(Gliding.VIEW)+" owner="+v.player.getAttached(BackEquipment.VISUAL)+" camera="+v.options.getCameraType()+" rows="+OffShieldLandingAudit.rowCount());});throw error;}
    }
    @Override public void runTest(ClientGameTestContext c){
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runCommand("weather clear");
            w.getServer().runOnServer(s->{for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());});c.getInput().resizeWindow(1280,960);
            for(var type:PlayerModelType.values())for(var arm:HumanoidArm.values()){
                c.runOnClient(v->OffShieldLandingAudit.enabled=false);c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(v->v.keyUse);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();Gliding.reset(p,false);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})p.setItemSlot(slot,ItemStack.EMPTY);p.setMainArm(arm);p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.setHealth(20);PlayerStamina.fill(p);var shield=new ItemStack(Items.SHIELD);check(FusionItems.attach(shield,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Real shield Fuse fixture");p.setItemInHand(InteractionHand.OFF_HAND,shield);GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));p.inventoryMenu.broadcastChanges();});
                c.waitFor(v->v.player.onGround()&&v.player.getOffhandItem().is(Items.SHIELD)&&v.player.getMainHandItem().isEmpty()&&v.player.getMainArm()==arm&&GliderEquipment.equipped(v.player));c.runOnClient(v->{v.player.setYRot(0);v.player.setXRot(0);RigAudit.fixture=skin(type);RigAudit.cape=false;RigAudit.enabled=true;RigAudit.bodyYaw=null;RigAudit.sleeves=true;v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});c.waitFor(v->ActionInput.tickFrame().gameplay());c.waitTicks(4);
                c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.isUsingItem()&&v.player.getUsedItemHand()==InteractionHand.OFF_HAND&&v.player.getTicksUsingItem()>=8);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var mob=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);mob.setPos(.5,120,7.5);mob.setNoAi(true);mob.setNoGravity(true);s.overworld().addFreshEntity(mob);float health=p.getHealth();p.hurtServer(s.overworld(),p.damageSources().mobAttack(mob),6);check(p.getHealth()==health&&p.getOffhandItem().getDamageValue()==7&&p.getOffhandItem().get(FusionContent.DATA).remaining()==31,"Native offhand shield block once/wear/Fuse");mob.discard();});c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->!v.player.isUsingItem()&&v.player.getAttached(BackEquipment.VISUAL)!=null&&v.player.getAttached(BackEquipment.VISUAL).shield().owner()==2&&v.player.getOffhandItem().getDamageValue()==7);c.waitTicks(4);
                c.runOnClient(v->OffShieldLandingAudit.begin());w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,122.5,.5,Set.of(),0,0,true);});await(c,"air",v->!v.player.onGround()&&v.player.getY()>121);c.waitTicks(2);w.getServer().runOnServer(server->check(!server.getPlayerList().getPlayers().getFirst().onGround(),"Actual server airborne state precedes fresh glide request"));c.getInput().holdKeyFor(v->v.keyJump,1);c.waitTicks(2);await(c,"open",v->Gliding.active(v.player));await(c,"back",v->OffShieldLandingAudit.glidingBackFrames>0);
                c.takeScreenshot("offshield-real-glider-"+type+"-"+arm);await(c,"landing",v->v.player.onGround()&&!Gliding.active(v.player));c.waitTicks(10);c.runOnClient(v->OffShieldLandingAudit.save(type+"-"+arm));c.takeScreenshot("offshield-natural-landing-"+type+"-"+arm);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getMainHandItem().isEmpty()&&p.getOffhandItem().is(Items.SHIELD)&&p.getOffhandItem().getDamageValue()==7&&p.getOffhandItem().get(FusionContent.DATA).remaining()==31&&p.getAttached(BackEquipment.VISUAL).shield().owner()==2,"Landing preserves actual offhand inventory/wear/Fuse and owner2");});
                System.out.println("C6BA passive actual offshield landing "+type+" main="+arm+" groundDrawFrames="+OffShieldLandingAudit.groundDrawFrames+" stationaryDrawFrames="+OffShieldLandingAudit.stationaryGroundDrawFrames+"; no source owner2 implementation");
            }
        }finally{c.getInput().releaseKey(v->v.keyJump);c.getInput().releaseKey(v->v.keyUse);c.runOnClient(v->{OffShieldLandingAudit.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;RigAudit.cape=null;RigAudit.bodyYaw=null;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
