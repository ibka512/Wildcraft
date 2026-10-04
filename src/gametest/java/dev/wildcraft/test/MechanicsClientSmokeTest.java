package dev.wildcraft.test;

import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
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
        System.out.println("WILDCRAFT P6 core tracking/model, native passenger, finite motion across chunk, real world save/reload and API recovery passed; input is a separate check");
    }
    private static MachineEntity find(net.minecraft.world.level.Level level) {
        return level.getEntitiesOfClass(MachineEntity.class,new AABB(-20,110,-20,60,140,20)).stream().findFirst().orElse(null);
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
