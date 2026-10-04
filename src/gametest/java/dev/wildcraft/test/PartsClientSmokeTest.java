package dev.wildcraft.test;

import static dev.wildcraft.test.MechanicsClientSmokeTest.*;
import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class PartsClientSmokeTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save;int[] charge={0};
        try(TestSingleplayerContext world=c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
                for(int x=-8;x<=8;x++)for(int z=-8;z<=30;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                for(int x=-4;x<=4;x++){level.setBlockAndUpdate(new BlockPos(x,120,7),Blocks.STONE.defaultBlockState());for(int y=120;y<=123;y++)level.setBlockAndUpdate(new BlockPos(x,y,12),Blocks.STONE.defaultBlockState());}
                p.teleportTo(level,.5,120,.5,Set.of(),0,30,false);p.getInventory().clearContent();
                p.getInventory().setItem(0,new ItemStack(MechanicsContent.WING,2));p.getInventory().setItem(1,new ItemStack(MechanicsContent.WHEEL,2));
                var b=new ItemStack(EnergyContent.BATTERY);Batteries.charge(b,240);p.getInventory().setItem(2,b);p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();
                var body=MechanicsContent.MACHINE.create(level,EntitySpawnReason.COMMAND);body.setPos(.5,120,2.5);body.setOwner(p.getUUID());level.addFreshEntity(body);
            });
            c.waitFor(client -> find(client.level)!=null && client.player.getMainHandItem().is(MechanicsContent.WING));
            aim(c,MachineNodes.point(2));c.takeScreenshot("p7-wing-preview");click(c);
            c.waitFor(client -> find(client.level).kind(2)==3 && client.player.getMainHandItem().getCount()==1);
            select(c,1);
            for(int node:new int[]{4,5}) {
                world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),node==4?-1.5:2.5,120,2.5,Set.of(),node==4?-90:90,25,false);});
                c.waitTicks(5);aim(c,MachineNodes.point(node));click(c);c.waitFor(client -> find(client.level).kind(node)==6);
            }
            check(c.computeOnClient(client -> client.player.getInventory().getItem(1).isEmpty()),"Native wheel installation transfers two real items");
            world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,120,.5,Set.of(),0,30,false);});
            select(c,2);aim(c,MachineNodes.point(0));click(c);c.waitFor(client -> find(client.level).energy()==240);
            select(c,3);aim(c,MachineNodes.point(2));click(c);c.waitFor(client -> client.player.getVehicle() instanceof MachineEntity);
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(client -> find(client.level).enabled());
            c.getInput().holdKey(o -> o.keyUp);c.waitFor(client -> find(client.level).getY()>120.8,100);c.takeScreenshot("p7-native-one-block-step");
            c.waitTicks(25);c.getInput().releaseKey(o -> o.keyUp);c.waitTicks(5);
            world.getServer().runOnServer(server -> {var body=find(server.overworld());check(body.getZ()>7 && body.getBoundingBox().maxZ<=12.01,"Real wheels climb one block and stop at a tall wall");charge[0]=body.energy();check(charge[0]>0 && charge[0]<240,"Actual ground driving pays finite charge");});
            c.waitTicks(8);world.getServer().runOnServer(server -> check(find(server.overworld()).energy()==charge[0],"Idle rider does not burn wheel energy"));
            double z=c.computeOnClient(client -> find(client.level).getZ());c.getInput().holdKeyFor(o -> o.keyDown,12);c.waitTicks(4);
            check(c.computeOnClient(client -> find(client.level).getZ()<z-.4),"Real S input reverses the vehicle");
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(client -> !find(client.level).enabled());
            c.getInput().holdKeyFor(o -> o.keyShift,1);c.waitFor(client -> client.player.getVehicle()==null);
            world.getServer().runOnServer(server -> {var body=find(server.overworld());body.setDeltaMovement(Vec3.ZERO);charge[0]=body.energy();});save=world.getWorldSave();
        }
        try(TestSingleplayerContext world=save.open()) {
            world.getConnection().waitForChunksRender();c.waitFor(client -> find(client.level)!=null && find(client.level).kind(2)==3 && find(client.level).kind(4)==6 && find(client.level).kind(5)==6 && find(client.level).energy()==charge[0]);
            world.getServer().runOnServer(server -> {
                var body=find(server.overworld());var p=server.getPlayerList().getPlayers().getFirst();
                check(body.owner().equals(p.getUUID()),"Real disk save retains new parts and owner");body.setPos(.5,135,2.5);body.setDeltaMovement(.3,-.5,0);
                p.teleportTo(server.overworld(),.5,135,2.5,Set.of(),0,15,false);check(p.startRiding(body),"Saved machine accepts passenger");
            });
            c.waitFor(client -> client.player.getVehicle() instanceof MachineEntity);c.waitTicks(20);
            world.getServer().runOnServer(server -> {var body=find(server.overworld());check(body.getY()>129 && body.getY()<135,"Actual passive wing descends slowly with a passenger and load");check(body.energy()==charge[0],"Airborne passive glide consumes no wheel charge");});
            c.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));c.waitTicks(3);c.takeScreenshot("p7-wing-wheel-loaded-glide");
        }
        otherParts(c);
        System.out.println("WILDCRAFT P7 wing/wheel actual input, preview, W/S driving, one-block step/tall wall, idle cost, real disk reload and loaded passive glide passed");
    }
    private static void otherParts(ClientGameTestContext c) {
        c.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        TestWorldSave save;int[] fuel={0};
        try(TestSingleplayerContext world=c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
                for(int x=-8;x<=8;x++)for(int z=-8;z<=18;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                // A small ledge supports the box while its lower centre remains reachable.
                for(int y=120;y<=122;y++)level.setBlockAndUpdate(new BlockPos(1,y,2),Blocks.STONE.defaultBlockState());
                p.teleportTo(level,.5,120,2.5,Set.of(),0,-60,false);p.getInventory().clearContent();
                p.getInventory().setItem(0,new ItemStack(MechanicsContent.SPRING));
                var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,200);p.getInventory().setItem(1,battery);
                var rocket=new ItemStack(MechanicsContent.ROCKET);rocket.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Native finite rocket"));
                p.getInventory().setItem(2,rocket);p.getInventory().setItem(4,new ItemStack(MechanicsContent.STABILIZER));p.getInventory().setItem(5,new ItemStack(MechanicsContent.BUOYANCY));
                p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();
                var b=MechanicsContent.MACHINE.create(level,EntitySpawnReason.COMMAND);b.setPos(.5,123,2.5);b.setOwner(p.getUUID());level.addFreshEntity(b);
            });
            c.waitFor(client -> find(client.level)!=null && client.player.getMainHandItem().is(MechanicsContent.SPRING));
            aim(c,MachineNodes.point(1));c.takeScreenshot("p7-spring-bottom-preview");click(c);c.waitFor(client -> find(client.level).kind(1)==5 && client.player.getMainHandItem().isEmpty());
            world.getServer().runOnServer(server -> {var b=find(server.overworld());b.setPos(.5,120,2.5);b.setDeltaMovement(Vec3.ZERO);var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,120,.5,Set.of(),0,30,false);});
            c.waitTicks(5);select(c,1);aim(c,MachineNodes.point(0));click(c);c.waitFor(client -> find(client.level).energy()==200);
            select(c,3);aim(c,MachineNodes.point(2));click(c);c.waitFor(client -> client.player.getVehicle() instanceof MachineEntity);
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);
            c.waitFor(client -> find(client.level).springCooldown(1)>0 && find(client.level).energy()==180 && find(client.level).getY()>120.2);c.takeScreenshot("p7-native-spring-launch");
            c.waitTicks(45);check(c.computeOnClient(client -> find(client.level).energy()==180 && find(client.level).springCooldown(1)==0),"One native R launch cannot repeat while power stays on");
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(client -> !find(client.level).enabled());
            c.getInput().holdKeyFor(o -> o.keyShift,1);c.waitFor(client -> client.player.getVehicle()==null);
            placePlayer(world,c,4);select(c,4);aim(c,MachineNodes.point(4));click(c);c.waitFor(client -> find(client.level).kind(4)==7);
            placePlayer(world,c,3);select(c,5);aim(c,MachineNodes.point(3));c.takeScreenshot("p7-buoyancy-front-preview");click(c);c.waitFor(client -> find(client.level).kind(3)==8);
            placePlayer(world,c,2);select(c,2);aim(c,MachineNodes.point(2));c.takeScreenshot("p7-rocket-rear-preview");click(c);c.waitFor(client -> find(client.level).kind(2)==4 && find(client.level).rocketFuel(2)==80);
            // Keep the powered fixture at the wall so the real owner stays in reach.
            world.getServer().runOnServer(server -> {var level=server.overworld();for(int x=-3;x<=3;x++)for(int y=120;y<=126;y++)level.setBlockAndUpdate(new BlockPos(x,y,4),Blocks.STONE.defaultBlockState());});
            select(c,3);aim(c,MachineNodes.point(2));click(c);c.waitFor(client -> client.player.getVehicle() instanceof MachineEntity);
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(client -> find(client.level).enabled() && find(client.level).rocketFuel(2)<80 && find(client.level).active(4));
            c.waitTicks(5);c.takeScreenshot("p7-rocket-spring-stabilizer-powered");
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(client -> !find(client.level).enabled());
            c.getInput().holdKeyFor(o -> o.keyShift,1);c.waitFor(client -> client.player.getVehicle()==null);
            world.getServer().runOnServer(server -> {var b=find(server.overworld());b.setPos(.5,120,3.2);b.setDeltaMovement(Vec3.ZERO);});placePlayer(world,c,2);
            aim(c,MachineNodes.point(2));sneakClick(c,MachineNodes.point(2));
            check(c.computeOnClient(client -> find(client.level).kind(2)==4 && client.player.getMainHandItem().isEmpty()),"Actual native recovery refuses a burning rocket even with switch off");
            fuel[0]=c.computeOnClient(client -> find(client.level).rocketFuel(2));check(fuel[0]>0 && fuel[0]<80,"Native ignition leaves finite remainder");c.takeScreenshot("p7-native-rocket-burning-off");save=world.getWorldSave();
        }
        try(TestSingleplayerContext world=save.open()) {
            world.getConnection().waitForChunksRender();c.waitFor(client -> find(client.level)!=null && find(client.level).kind(1)==5 && find(client.level).kind(4)==7 && find(client.level).kind(3)==8);
            check(c.computeOnClient(client -> find(client.level).rocketFuel(2)<fuel[0] && !find(client.level).enabled()),"Real disk reload never restores rocket fuel or power");
            c.waitFor(client -> find(client.level).rocketFuel(2)==0 && !find(client.level).working(),150);
            placePlayer(world,c,2);select(c,3);sneakClick(c,MachineNodes.point(2));
            c.waitFor(client -> find(client.level).kind(2)==0 && client.player.getInventory().getItem(0).is(MechanicsContent.SPENT_ROCKET));
            select(c,0);
            check(c.computeOnClient(client -> client.player.getMainHandItem().getHoverName().getString().equals("Native finite rocket")),"Real exhausted casing retains its original name");
            click(c);c.waitTicks(5);check(c.computeOnClient(client -> find(client.level).kind(2)==0 && client.player.getMainHandItem().is(MechanicsContent.SPENT_ROCKET)),"Spent casing cannot be installed again by native click");
            // Swim fixture uses the actual installed float and finite battery after a real reload.
            world.getServer().runOnServer(server -> {
                var level=server.overworld();var b=find(level);var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(level,1.5,124,2.5,Set.of(),-90,35,false);
                for(int x=1;x<=7;x++)for(int z=-1;z<=6;z++)for(int y=120;y<=123;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),x==1||x==7||z==-1||z==6?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState());
                b.setPos(3.5,121,2.5);b.setDeltaMovement(Vec3.ZERO);
                check(b.mass()<=1.75,"Installed native fixture is within one float capacity");
            });
            c.waitFor(client -> find(client.level).getX()>3 && find(client.level).getY()>120.5);
            double before=c.computeOnClient(client -> find(client.level).getY());int energy=c.computeOnClient(client -> find(client.level).energy());
            c.waitTicks(15);check(c.computeOnClient(client -> find(client.level).getY()>before+.1 && find(client.level).active(3) && find(client.level).energy()==energy),"Real unpowered float supports the assembled load in water without draining battery");
            c.waitTicks(60);c.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));aim(c,MachineNodes.point(0));c.takeScreenshot("p7-water-float-loaded-parts");
            world.getServer().runOnServer(server -> {var b=find(server.overworld());check(b.owner().equals(server.getPlayerList().getPlayers().getFirst().getUUID()),"Real new part disk world retains owner");});
        }
        System.out.println("WILDCRAFT P7 actual native bottom spring/rocket/stabilizer/float install, finite spring launch, irreversible burning, disk reload, named casing recovery, spent rejection and loaded passive water float passed");
    }
    private static void placePlayer(TestSingleplayerContext world,ClientGameTestContext c,int node) {
        world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();var b=find(server.overworld());Vec3 offset=switch(node){case 4 -> new Vec3(-2,0,0);case 5 -> new Vec3(2,0,0);case 3 -> new Vec3(0,0,2);default -> new Vec3(0,0,-2);};var pos=b.position().add(MachineNodes.rotate(offset,b.getYRot()));p.teleportTo(server.overworld(),pos.x,120,pos.z,Set.of(),b.getYRot(),30,false);});c.waitTicks(5);
    }

}
