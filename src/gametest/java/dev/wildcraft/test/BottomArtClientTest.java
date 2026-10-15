package dev.wildcraft.test;
import dev.wildcraft.client.mechanics.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.mechanics.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/** Real bottom ray/click/recovery, source-scale geometry and actual terrain-dependent spring clearance. */
public final class BottomArtClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static MachineEntity body(net.minecraft.world.level.Level level){return MechanicsClientSmokeTest.find(level);}
    private static MachineRenderer.State render(net.minecraft.client.Minecraft c){var b=body(c.level);return (MachineRenderer.State)c.getEntityRenderDispatcher().getRenderer(b).createRenderState(b,1);}
    private static void look(ClientGameTestContext c,Vec3 local){
        c.runOnClient(v->{var b=body(v.level);var d=b.position().add(MachineNodes.rotate(local,b.getYRot())).subtract(v.player.getEyePosition());v.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));v.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));});c.waitTicks(2);
    }
    @Override public void runTest(ClientGameTestContext c){
        try{
            for(var item:new Item[]{MechanicsContent.SPRING,MechanicsContent.BUOYANCY,MechanicsContent.WING,MechanicsContent.FAN})run(c,item);
        }finally{c.getInput().releaseKey(v->v.keyUse);c.getInput().releaseKey(v->v.keyShift);c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));}
        System.out.println("BOTTOM ART native bottom ghost/click/installed/once-recovery, four original-scale pocket meshes, ground/air/yaw/actual spring shape clearance, water/rotor/ride cameras PASS");
    }
    private static void run(ClientGameTestContext c,Item item){
        int kind=item==MechanicsContent.SPRING?5:item==MechanicsContent.BUOYANCY?8:item==MechanicsContent.WING?3:1;String name=BottomArt.name(kind);
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.overworld().setBlockAndUpdate(new BlockPos(-1,122,0),Blocks.STONE.defaultBlockState());p.teleportTo(s.overworld(),.5,120,-1.5,Set.of(),0,0,false);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));p.inventoryMenu.broadcastChanges();var b=MechanicsContent.MACHINE.create(s.overworld(),EntitySpawnReason.COMMAND);b.setPos(.5,123,.5);b.setOwner(p.getUUID());s.overworld().addFreshEntity(b);});
            c.waitFor(v->v.player.getMainHandItem().is(item)&&body(v.level)!=null&&body(v.level).onGround());c.getInput().resizeWindow(1280,960);c.runOnClient(v->{v.options.setCameraType(CameraType.FIRST_PERSON);v.options.guiScale().set(2);});
            MechanicsClientSmokeTest.aim(c,MachineNodes.point(1).add(0,.001,0));c.waitFor(v->MachinePresentation.selectedNode(body(v.level))==1&&render(v).ghost==kind);
            c.takeScreenshot("bottom-"+name+"-native-ghost");MechanicsClientSmokeTest.click(c);
            c.waitFor(v->body(v.level).kind(1)==kind&&v.player.getMainHandItem().isEmpty());
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var b=body(s.overworld());check(b.kind(1)==kind&&p.getMainHandItem().isEmpty(),"Native bottom install transfers real item once");p.setNoGravity(true);p.teleportTo(s.overworld(),-2,123,-2,Set.of(),0,0,false);});c.waitFor(v->v.player.getX()< -1);
            look(c,new Vec3(0,.325,0));c.takeScreenshot("bottom-"+name+"-installed-clearance");
            if(kind==5)c.runOnClient(v->check(BottomArt.spring(body(v.level),1,0,true)>.999,"True swept spring footprint clears distant floor and edge support"));
            w.getServer().runOnServer(s->{var b=body(s.overworld());b.setPos(.5,120,.5);b.setDeltaMovement(Vec3.ZERO);b.setOnGround(true);var p=s.getPlayerList().getPlayers().getFirst();p.setNoGravity(false);p.teleportTo(s.overworld(),-2,120,-2,Set.of(),0,0,false);});c.waitFor(v->body(v.level).getY()<120.1&&v.player.getY()<120.1);c.waitTicks(4);look(c,new Vec3(0,.2,0));
            if(kind==5)c.runOnClient(v->{float f=BottomArt.spring(body(v.level),1,0,true);check(f>.045&&f<.055,"True collision surface caps .20 travel to authored .01 ground clearance: "+f);});
            c.takeScreenshot("bottom-"+name+"-flat-ground");
            w.getServer().runOnServer(s->{var b=body(s.overworld());b.setYRot(45);});c.waitTicks(4);look(c,new Vec3(0,.2,0));if(kind==5)c.runOnClient(v->{float f=BottomArt.spring(body(v.level),1,0,true);check(f>.045&&f<.055,"Rotated real spring footprint remains capped on flat floor");});c.takeScreenshot("bottom-"+name+"-yaw45");
            if(kind==5){
                w.getServer().runOnServer(s->{var l=s.overworld();l.setBlockAndUpdate(new BlockPos(0,120,0),Blocks.STONE_SLAB.defaultBlockState());var b=body(l);b.setPos(.5,120.5,.5);b.setDeltaMovement(Vec3.ZERO);b.setOnGround(true);});c.waitFor(v->body(v.level).getY()>120.4);c.waitTicks(2);c.runOnClient(v->{float f=BottomArt.spring(body(v.level),1,0,true);check(f>.045&&f<.055,"Actual half-slab shape is used instead of integer block occupancy: "+f);});look(c,new Vec3(0,.2,0));c.takeScreenshot("bottom-spring-half-slab");
            }
            if(kind==8){
                w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=120;y<=122;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState());var b=body(l);b.setPos(.5,121,.5);b.setDeltaMovement(Vec3.ZERO);var p=s.getPlayerList().getPlayers().getFirst();p.setNoGravity(true);p.teleportTo(-3,123,-3);});c.waitFor(v->body(v.level).isInWater());look(c,new Vec3(0,.325,0));c.takeScreenshot("bottom-buoyancy-actual-water-static");w.getServer().runOnServer(s->{for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=120;y<=122;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());});
            }
            // Original battery and existing energy/thrust paths; no new activation behavior is introduced.
            w.getServer().runOnServer(s->{s.overworld().setBlockAndUpdate(new BlockPos(-1,122,0),Blocks.STONE.defaultBlockState());s.overworld().setBlockAndUpdate(new BlockPos(0,120,0),Blocks.AIR.defaultBlockState());var b=body(s.overworld());b.setPos(.5,120,.5);b.setDeltaMovement(Vec3.ZERO);var p=s.getPlayerList().getPlayers().getFirst();p.setNoGravity(false);p.teleportTo(-2,120,-2);var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1000);p.setItemInHand(InteractionHand.MAIN_HAND,battery);check(b.install(p,0,InteractionHand.MAIN_HAND),"Original finite battery installs once");p.inventoryMenu.broadcastChanges();});c.waitFor(v->body(v.level).batteryNode()==0&&body(v.level).onGround());
            w.getServer().runOnServer(s->{var b=body(s.overworld());check(b.setEnabled(s.getPlayerList().getPlayers().getFirst(),true),"Existing toggle activates");});
            if(kind==5)c.waitFor(v->body(v.level).springCooldown(1)>30);else c.waitTicks(3);
            look(c,new Vec3(0,.325,0));c.takeScreenshot("bottom-"+name+"-active-existing-motion");
            w.getServer().runOnServer(s->{var b=body(s.overworld());var p=s.getPlayerList().getPlayers().getFirst();b.setEnabled(p,false);b.setPos(.5,120,.5);b.setDeltaMovement(Vec3.ZERO);check(p.startRiding(b),"Native rider mounts unchanged body");});c.waitFor(v->v.player.getVehicle()!=null);
            c.runOnClient(v->{v.options.setCameraType(CameraType.FIRST_PERSON);v.player.setYRot(0);v.player.setXRot(0);});c.waitTicks(3);c.takeScreenshot("bottom-"+name+"-rider-first");c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.waitTicks(3);c.takeScreenshot("bottom-"+name+"-rider-third");
            // Recover underneath the edge support using native sneak/right-click, then verify once-only ownership.
            w.getServer().runOnServer(s->{var b=body(s.overworld());var p=s.getPlayerList().getPlayers().getFirst();p.stopRiding();b.setPos(.5,123,.5);b.setYRot(0);b.setDeltaMovement(Vec3.ZERO);p.setNoGravity(false);p.teleportTo(s.overworld(),.5,120,-1.5,Set.of(),0,0,false);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));c.waitFor(v->v.player.getVehicle()==null&&v.player.getY()<120.1&&body(v.level).getY()>122.9);MechanicsClientSmokeTest.sneakClick(c,MachineNodes.point(1).add(0,.001,0));c.waitFor(v->body(v.level).kind(1)==0&&v.player.getMainHandItem().is(item));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var b=body(s.overworld());check(!b.recover(p,1),"Empty bottom cannot be recovered twice");int count=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))count+=p.getInventory().getItem(i).getCount();check(count==1,"One actual part returned");});c.takeScreenshot("bottom-"+name+"-native-recovered");
        }
    }
}
