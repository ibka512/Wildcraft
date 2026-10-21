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

/** Locally adopted additions: real bottom ghost/install/recovery and native state-driven display. */
public final class BottomAdditionsClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static MachineEntity body(net.minecraft.world.level.Level level){return MechanicsClientSmokeTest.find(level);}
    private static MachineRenderer.State render(net.minecraft.client.Minecraft c){var b=body(c.level);return (MachineRenderer.State)c.getEntityRenderDispatcher().getRenderer(b).createRenderState(b,1);}
    private static void look(ClientGameTestContext c,Vec3 local){
        c.runOnClient(v->{var b=body(v.level);var d=b.position().add(MachineNodes.rotate(local,b.getYRot())).subtract(v.player.getEyePosition());v.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));v.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));});c.waitTicks(2);
    }
    private static void checkResources(){
        for(String name:new String[]{"battery_bottom","rocket_bottom","spent_rocket_bottom","wheel_bottom","stabilizer_bottom","machine_bottom_battery","machine_bottom_rocket","machine_bottom_wheel","machine_bottom_stabilizer"}){
            try(var in=BottomAdditionsClientTest.class.getResourceAsStream("/assets/wildcraft/geometry/"+name+".json")){
                check(in!=null,"Mounted geometry exists: "+name);var data=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                String texture=data.get("texture").getAsString();try(var image=BottomAdditionsClientTest.class.getResourceAsStream("/assets/wildcraft/textures/entity/"+texture+".png")){check(image!=null,"Actual native mounted texture exists: "+name+"/"+texture);}
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        }
    }
    private int initialEnergy=500;
    @Override public void runTest(ClientGameTestContext c){
        try{
            for(var item:new Item[]{EnergyContent.BATTERY,MechanicsContent.ROCKET,MechanicsContent.WHEEL,MechanicsContent.STABILIZER})run(c,item);
            for(int energy:new int[]{0,1000}){initialEnergy=energy;run(c,EnergyContent.BATTERY);}
        }finally{c.getInput().releaseKey(v->v.keyUse);c.getInput().releaseKey(v->v.keyShift);c.getInput().releaseKey(v->v.keyUp);c.getInput().releaseKey(v->v.keyDown);c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));}
        System.out.println("BOTTOM_ADDITIONS_PASS native ghost/install/once-recovery / battery0-500-1000 / rocket80-spent / wheel forward-reverse signed phase / stabilizer active / unchanged native cameras");
    }
    private void run(ClientGameTestContext c,Item item){
        int kind=item==EnergyContent.BATTERY?2:item==MechanicsContent.ROCKET?4:item==MechanicsContent.WHEEL?6:7;String name=BottomArt.name(kind)+(kind==2?"-"+initialEnergy:"");
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();c.runOnClient(v->checkResources());w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-25;x<=25;x++)for(int z=-25;z<=25;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.overworld().setBlockAndUpdate(new BlockPos(-1,122,0),Blocks.STONE.defaultBlockState());if(kind==4)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,126,z),Blocks.STONE.defaultBlockState());p.teleportTo(s.overworld(),.5,120,-1.5,Set.of(),0,0,false);p.getInventory().clearContent();var supplied=new ItemStack(item);if(kind==2)Batteries.charge(supplied,initialEnergy);p.setItemInHand(InteractionHand.MAIN_HAND,supplied);p.inventoryMenu.broadcastChanges();var b=MechanicsContent.MACHINE.create(s.overworld(),EntitySpawnReason.COMMAND);b.setPos(.5,123,.5);b.setOwner(p.getUUID());s.overworld().addFreshEntity(b);});
            c.waitFor(v->v.player.getMainHandItem().is(item)&&body(v.level)!=null&&body(v.level).onGround());c.getInput().resizeWindow(1280,960);c.runOnClient(v->{v.options.setCameraType(CameraType.FIRST_PERSON);v.options.guiScale().set(2);});
            MechanicsClientSmokeTest.aim(c,MachineNodes.point(1).add(0,.001,0));c.waitFor(v->MachinePresentation.selectedNode(body(v.level))==1&&render(v).ghost==kind);
            c.takeScreenshot("bottom-"+name+"-native-ghost");MechanicsClientSmokeTest.click(c);
            c.waitFor(v->body(v.level).kind(1)==kind&&v.player.getMainHandItem().isEmpty());
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var b=body(s.overworld());check(b.kind(1)==kind&&p.getMainHandItem().isEmpty(),"Native bottom install transfers real item once");p.setNoGravity(true);p.teleportTo(s.overworld(),-2,123,-2,Set.of(),0,0,false);});c.waitFor(v->v.player.getX()< -1);
            look(c,new Vec3(0,.325,0));c.takeScreenshot("bottom-"+name+"-installed-clearance");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120.2,-2);});c.waitFor(v->v.player.getY()<120.4);look(c,new Vec3(0,.04,0));c.takeScreenshot("bottom-"+name+"-actual-underside");
            w.getServer().runOnServer(s->{var b=body(s.overworld());b.setPos(.5,120,.5);b.setDeltaMovement(Vec3.ZERO);b.setOnGround(true);var p=s.getPlayerList().getPlayers().getFirst();p.setNoGravity(false);p.teleportTo(s.overworld(),-2,120,-2,Set.of(),0,0,false);});c.waitFor(v->body(v.level).getY()<120.1&&v.player.getY()<120.1);c.waitTicks(4);look(c,new Vec3(0,.2,0));
            c.takeScreenshot("bottom-"+name+"-flat-ground");
            w.getServer().runOnServer(s->{body(s.overworld()).setYRot(45);});c.waitTicks(4);look(c,new Vec3(0,.2,0));c.takeScreenshot("bottom-"+name+"-yaw45");
            w.getServer().runOnServer(s->{s.overworld().setBlockAndUpdate(new BlockPos(0,120,0),Blocks.STONE_SLAB.defaultBlockState());var b=body(s.overworld());b.setPos(.5,120.5,.5);b.setDeltaMovement(Vec3.ZERO);});c.waitFor(v->body(v.level).getY()>120.4);c.waitTicks(3);look(c,new Vec3(0,.2,0));c.takeScreenshot("bottom-"+name+"-half-slab-edge");
            // Original battery and existing energy/thrust paths; no new activation behavior is introduced.
            w.getServer().runOnServer(s->{s.overworld().setBlockAndUpdate(new BlockPos(-1,122,0),Blocks.STONE.defaultBlockState());s.overworld().setBlockAndUpdate(new BlockPos(0,120,0),Blocks.AIR.defaultBlockState());var b=body(s.overworld());b.setPos(.5,120,.5);b.setDeltaMovement(Vec3.ZERO);var p=s.getPlayerList().getPlayers().getFirst();p.setNoGravity(false);p.teleportTo(-2,120,-2);var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1000);p.setItemInHand(InteractionHand.MAIN_HAND,battery);if(kind!=2)check(b.install(p,0,InteractionHand.MAIN_HAND),"Original finite battery installs once");else p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.waitFor(v->body(v.level).batteryNode()==(kind==2?1:0)&&body(v.level).onGround());
            w.getServer().runOnServer(s->{var b=body(s.overworld());check(b.setEnabled(s.getPlayerList().getPlayers().getFirst(),true),"Existing toggle activates");});
            if(kind==7)c.waitFor(v->body(v.level).active(1));else c.waitTicks(3);
            if(kind==2)c.runOnClient(v->check(body(v.level).energy()==initialEnergy,"Bottom battery keeps its real charge"));
            if(kind==4){
                c.waitFor(v->body(v.level).rocketFuel(1)>0&&body(v.level).active(1));look(c,new Vec3(0,.325,0));c.takeScreenshot("bottom-rocket-burning-authored-flame");
                w.getServer().runOnServer(s->{var b=body(s.overworld());var p=s.getPlayerList().getPlayers().getFirst();p.setNoGravity(true);p.teleportTo(b.getX()-2,b.getY(),b.getZ()-2);b.setEnabled(p,false);check(!b.recover(p,1),"Native burning rocket cannot be recovered");});
                c.waitFor(v->body(v.level).rocketFuel(1)==0,150);c.takeScreenshot("bottom-rocket-native-spent");
            }

            look(c,new Vec3(0,.325,0));c.takeScreenshot("bottom-"+name+"-native-state-after-activation");
            w.getServer().runOnServer(s->{var b=body(s.overworld());var p=s.getPlayerList().getPlayers().getFirst();b.setEnabled(p,false);b.setPos(.5,120,.5);b.setDeltaMovement(Vec3.ZERO);check(p.startRiding(b),"Native rider mounts unchanged body");});c.waitFor(v->v.player.getVehicle()!=null);
            c.runOnClient(v->{v.options.setCameraType(CameraType.FIRST_PERSON);v.player.setYRot(0);v.player.setXRot(0);});c.waitTicks(3);c.takeScreenshot("bottom-"+name+"-rider-first");c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.waitTicks(3);c.takeScreenshot("bottom-"+name+"-rider-third");
            if(kind==6){
                w.getServer().runOnServer(s->{var b=body(s.overworld());b.setYRot(0);check(b.setEnabled(s.getPlayerList().getPlayers().getFirst(),true),"Wheel enabled");});
                c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.getInput().holdKey(v->v.keyUp);c.waitFor(v->body(v.level).getDeltaMovement().z>.12);float forward=c.computeOnClient(v->render(v).bottomWheelAngle);c.waitTicks(2);float forwardEnd=c.computeOnClient(v->render(v).bottomWheelAngle);check(Math.IEEEremainder(forwardEnd-forward,Math.PI*2)>0,"Real forward travel rolls around body +X");c.takeScreenshot("bottom-wheel-native-forward");c.getInput().releaseKey(v->v.keyUp);
                c.getInput().holdKey(v->v.keyDown);c.waitFor(v->body(v.level).getDeltaMovement().z<-.12,90);float reverse=c.computeOnClient(v->render(v).bottomWheelAngle);c.waitTicks(2);float reverseEnd=c.computeOnClient(v->render(v).bottomWheelAngle);check(Math.IEEEremainder(reverseEnd-reverse,Math.PI*2)<0,"Real reverse travel reverses phase");c.takeScreenshot("bottom-wheel-native-reverse");c.getInput().releaseKey(v->v.keyDown);
                w.getServer().runOnServer(s->body(s.overworld()).setEnabled(s.getPlayerList().getPlayers().getFirst(),false));
            }
            w.getServer().runOnServer(s->{var b=body(s.overworld());var p=s.getPlayerList().getPlayers().getFirst();p.stopRiding();b.setEnabled(p,false);b.setPos(.5,123,.5);b.setYRot(0);b.setDeltaMovement(Vec3.ZERO);p.setNoGravity(true);p.teleportTo(.5,120.2,-2);for(int n=2;n<6;n++){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));check(b.install(p,n,InteractionHand.MAIN_HAND),"Four native neighbor mounts retain nodes");}if(kind==7)check(b.setEnabled(p,true),"Native active stabilizer indicator with adjacent parts");p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getVehicle()==null&&body(v.level).kind(5)==1&&(kind!=7||body(v.level).active(1)));c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));look(c,new Vec3(0,.04,0));c.takeScreenshot("bottom-"+name+"-native-neighbor-assembly");
            // Recover underneath the edge support using native sneak/right-click, then verify once-only ownership.
            w.getServer().runOnServer(s->{var b=body(s.overworld());var p=s.getPlayerList().getPlayers().getFirst();p.stopRiding();b.setEnabled(p,false);b.setPos(.5,123,.5);b.setYRot(0);b.setDeltaMovement(Vec3.ZERO);p.setNoGravity(false);p.teleportTo(s.overworld(),.5,120,-1.5,Set.of(),0,0,false);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));c.waitFor(v->v.player.getVehicle()==null&&v.player.getY()<120.1&&body(v.level).getY()>122.9);MechanicsClientSmokeTest.sneakClick(c,MachineNodes.point(1).add(0,.001,0));c.waitFor(v->body(v.level).kind(1)==0&&v.player.getMainHandItem().is(kind==4?MechanicsContent.SPENT_ROCKET:item));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var b=body(s.overworld());check(!b.recover(p,1),"Empty bottom cannot be recovered twice");int count=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(kind==4?MechanicsContent.SPENT_ROCKET:item))count+=p.getInventory().getItem(i).getCount();check(count==1,"One actual part returned");if(kind==2)check(Batteries.energy(p.getMainHandItem())==initialEnergy,"Bottom battery recovery preserves charge");});c.takeScreenshot("bottom-"+name+"-native-recovered");
        }
    }
}
