package dev.wildcraft.test;

import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Uses real tracked entities and disk worlds; input acceptance is verified separately. */
public final class MechanicsClientSmokeTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save;
        try(TestSingleplayerContext world=c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
                for(int x=-3;x<=60;x++)for(int z=-4;z<=4;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                p.teleportTo(level,14.5,120,.5,Set.of(),0,30,false);
                var body=MechanicsContent.MACHINE.create(level,EntitySpawnReason.COMMAND);body.setPos(14.5,120,2.5);body.setYRot(-90);body.setOwner(p.getUUID());level.addFreshEntity(body);
                var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,60);p.setItemInHand(InteractionHand.MAIN_HAND,battery);check(body.install(p,0,InteractionHand.MAIN_HAND),"Real battery installed");
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));check(body.install(p,2,InteractionHand.MAIN_HAND),"Real fan installed");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client -> find(client.level)!=null && find(client.level).energy()==60 && find(client.level).kind(2)==1 && client.player.getY()>119);
            c.waitTicks(20);c.takeScreenshot("p6-core-installed-model");
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),12.2,120,2.5,Set.of(),-90,25,false);});
            c.waitFor(client -> client.player.getX()<12.3);c.waitTicks(5);c.takeScreenshot("p6-core-fan-model");
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();var body=find(server.overworld());check(p.startRiding(body),"Native ride starts");body.setEnabled(p,true);});
            c.waitFor(client -> client.player.getVehicle() instanceof MachineEntity && find(client.level).working());
            c.takeScreenshot("p6-core-riding");
            c.waitFor(client -> find(client.level).energy()==0 && !find(client.level).working(),250);
            world.getServer().runOnServer(server -> {
                var body=find(server.overworld());var p=server.getPlayerList().getPlayers().getFirst();
                check(body.getX()>16,"Body crosses a real chunk boundary");check(p.getVehicle()==body && p.position().distanceTo(body.position())<2,"Passenger follows across chunk");
                body.setEnabled(p,false);p.stopRiding();p.teleportTo(body.getX()-2,120,body.getZ()-2);body.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            });
            c.waitFor(client -> client.player.getVehicle()==null && !find(client.level).enabled());c.waitTicks(5);save=world.getWorldSave();
        }
        try(TestSingleplayerContext world=save.open()) {
            world.getConnection().waitForChunksRender();
            c.waitFor(client -> find(client.level)!=null && find(client.level).energy()==0 && find(client.level).kind(2)==1 && !find(client.level).enabled());
            c.takeScreenshot("p6-core-reloaded");
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();var body=find(server.overworld());p.teleportTo(body.getX()-1,body.getY(),body.getZ());
                check(body.owner().equals(p.getUUID()) && body.getX()>16,"Disk world retains owner and position");
                check(body.recover(p,0) && !body.recover(p,0),"Battery returns once after reload");check(body.recover(p,2),"Fan returns after reload");
                int battery=0,fan=0;for(int n=0;n<36;n++){var stack=p.getInventory().getItem(n);if(stack.is(EnergyContent.BATTERY)){battery+=stack.getCount();check(Batteries.energy(stack)==0,"No refill after disk reload");}if(stack.is(MechanicsContent.FAN))fan+=stack.getCount();}
                check(battery==1 && fan==1 && body.recoverBody(p),"Whole assembly conserves items on recovery");p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client -> find(client.level)==null);c.takeScreenshot("p6-core-recovered");
        }
        nativeInput(c);
        System.out.println("WILDCRAFT P6 core tracking/model, native passenger, finite motion across chunk, real world save/reload and API recovery passed; input is a separate check");
    }
    private static MachineEntity find(net.minecraft.world.level.Level level) {
        return level.getEntitiesOfClass(MachineEntity.class,new AABB(-20,110,-20,60,140,20)).stream().findFirst().orElse(null);
    }
    private static void nativeInput(ClientGameTestContext c) {
        TestWorldSave save;
        int[] remaining={0};
        try(TestSingleplayerContext world=c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
                for(int x=-8;x<=40;x++)for(int z=-8;z<=40;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                p.teleportTo(level,.5,120,.5,Set.of(),0,30,false);p.getInventory().clearContent();
                p.getInventory().setItem(0,new ItemStack(MechanicsContent.BODY));p.getInventory().setItem(1,new ItemStack(MechanicsContent.FAN,2));
                var b=new ItemStack(EnergyContent.BATTERY);Batteries.charge(b,80);p.getInventory().setItem(2,b);p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client -> client.player.getY()>119 && client.player.getMainHandItem().is(MechanicsContent.BODY));
            c.runOnClient(client -> client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(.5,120,2.5),Direction.UP,new BlockPos(0,119,2),false)));
            c.waitFor(client -> find(client.level)!=null && client.player.getMainHandItem().isEmpty());
            sneakClick(c,new net.minecraft.world.phys.Vec3(0,.65,.42));
            c.waitFor(client -> find(client.level).enabled() && !find(client.level).working());
            sneakClick(c,new net.minecraft.world.phys.Vec3(0,.65,.42));
            c.waitFor(client -> !find(client.level).enabled());c.takeScreenshot("p6-control-panel");
            select(c,1);aim(c,MachineNodes.point(2));
            c.waitFor(client -> dev.wildcraft.client.mechanics.MachinePresentation.selectedNode(find(client.level))==2);
            c.takeScreenshot("p6-fan-snap-preview");click(c);
            c.waitFor(client -> find(client.level).kind(2)==1 && client.player.getInventory().getItem(1).getCount()==1);
            click(c);c.waitTicks(5);check(c.computeOnClient(client -> client.player.getInventory().getItem(1).getCount()==1),"Repeated real click cannot consume on an occupied node");
            select(c,2);aim(c,MachineNodes.point(0));c.takeScreenshot("p6-battery-preview");click(c);
            c.waitFor(client -> find(client.level).energy()==80 && client.player.getMainHandItem().isEmpty());
            // Forge a rear-side coordinate while looking at the top. Server uses its own eye ray.
            select(c,1);
            c.runOnClient(client -> client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundInteractPacket(
                    find(client.level).getId(),InteractionHand.MAIN_HAND,new net.minecraft.world.phys.Vec3(Double.NaN,0,0),false)));
            c.waitTicks(5);check(c.computeOnClient(client -> find(client.level).fanMask()==4 && client.player.getMainHandItem().getCount()==1),"Malformed packet changes neither node nor hand");
            select(c,0);aim(c,MachineNodes.point(2));click(c);
            c.waitFor(client -> client.player.getVehicle() instanceof MachineEntity);c.waitTicks(5);
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);
            c.waitFor(client -> find(client.level).working());
            c.getInput().holdKeyFor(o -> o.keyRight,8);
            check(c.computeOnClient(client -> find(client.level).getYRot()>5),"Real rider key input steers on server");
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);
            c.waitFor(client -> !find(client.level).enabled());c.takeScreenshot("p6-native-rider-control");
            c.getInput().holdKeyFor(o -> o.keyShift,1);c.waitFor(client -> client.player.getVehicle()==null);
            world.getServer().runOnServer(server -> {var body=find(server.overworld());remaining[0]=body.energy();check(remaining[0]>0 && remaining[0]<80,"Real ride paid finite energy");body.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);});
            save=world.getWorldSave();
        }
        try(TestSingleplayerContext world=save.open()) {
            world.getConnection().waitForChunksRender();c.waitFor(client -> find(client.level)!=null && find(client.level).energy()==remaining[0]);
            world.getServer().runOnServer(server -> {
                var body=find(server.overworld());var p=server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(server.overworld(),body.getX(),120,body.getZ()-2,Set.of(),body.getYRot(),30,false);
                for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.inventoryMenu.broadcastChanges();
            });c.waitFor(client -> client.player.getInventory().getItem(0).is(Items.STONE));select(c,0);
            aim(c,MachineNodes.point(0));sneakClick(c,MachineNodes.point(0));
            c.waitTicks(5);check(c.computeOnClient(client -> find(client.level).batteryNode()==0 && find(client.level).energy()==remaining[0]),"Full occupied hand/inventory leaves battery installed");
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});
            c.waitFor(client -> client.player.getMainHandItem().isEmpty());aim(c,MachineNodes.point(0));
            sneakClick(c,MachineNodes.point(0));
            c.waitFor(client -> find(client.level).batteryNode()==-1 && Batteries.energy(client.player.getMainHandItem())==remaining[0]);
            aim(c,MachineNodes.point(0));click(c);
            c.waitFor(client -> find(client.level).energy()==remaining[0] && client.player.getMainHandItem().isEmpty());
            sneakClick(c,MachineNodes.point(0));
            c.waitFor(client -> find(client.level).batteryNode()==-1 && Batteries.energy(client.player.getMainHandItem())==remaining[0]);
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(1,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});select(c,1);
            c.waitFor(client -> client.player.getMainHandItem().isEmpty());
            // Move to the actual rear face after steering rotated the body.
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();var body=find(server.overworld());var pos=body.position().add(MachineNodes.rotate(new net.minecraft.world.phys.Vec3(0,0,-2),body.getYRot()));p.teleportTo(server.overworld(),pos.x,120,pos.z,Set.of(),body.getYRot(),30,false);});
            c.waitTicks(5);aim(c,MachineNodes.point(2));sneakClick(c,MachineNodes.point(2));
            c.waitFor(client -> find(client.level).fanMask()==0 && client.player.getMainHandItem().is(MechanicsContent.FAN));
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(2,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});select(c,2);
            c.waitFor(client -> client.player.getMainHandItem().isEmpty());aim(c,MachineNodes.point(2));
            c.getInput().holdKey(o -> o.keyShift);c.waitTicks(3);aim(c,MachineNodes.point(2));c.getInput().holdKeyFor(o -> o.keyAttack,1);c.getInput().releaseKey(o -> o.keyShift);
            c.waitFor(client -> find(client.level)==null && client.player.getInventory().getItem(2).is(MechanicsContent.BODY));
            c.takeScreenshot("p6-native-recovered");
        }
        System.out.println("WILDCRAFT P6 native placement, snap preview, actual clicks/replay, malformed intent, riding/steering/R, disk reload and native recovery passed");
    }
    private static void select(ClientGameTestContext c,int slot){c.runOnClient(client -> client.player.getInventory().setSelectedSlot(slot));c.waitTicks(3);}
    private static void aim(ClientGameTestContext c,net.minecraft.world.phys.Vec3 local){
        c.runOnClient(client -> {var b=find(client.level);var delta=b.position().add(MachineNodes.rotate(local,b.getYRot())).subtract(client.player.getEyePosition());
            client.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));client.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.horizontalDistanceSqr()))));});
        c.waitTicks(5);c.waitFor(client -> client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity()==find(client.level));
    }
    private static void sneakClick(ClientGameTestContext c,net.minecraft.world.phys.Vec3 local){c.getInput().holdKey(o -> o.keyShift);c.waitTicks(3);aim(c,local);click(c);c.getInput().releaseKey(o -> o.keyShift);c.waitTicks(3);}
    private static void click(ClientGameTestContext c){c.getInput().holdKeyFor(o -> o.keyUse,1);c.waitTicks(4);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
