package dev.wildcraft.test;

import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

public final class MechanicsGameTests {
    @GameTest public void nativeCreativeInteractionTransfersAllSixNodesOnce(GameTestHelper h) {
        for(int node=0;node<6;node++)for(float yaw:new float[]{0,33,90}) {
            var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,4,2),EntitySpawnReason.COMMAND);body.setYRot(yaw);
            var p=h.makeMockServerPlayerInLevel();body.setOwner(p.getUUID());p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var local=MachineNodes.point(node);var outward=MachineNodes.thrust(node).scale(node==0?-1.2:-2);
            var origin=body.blockPosition();for(int x=-3;x<=3;x++)for(int y=-3;y<=5;y++)for(int z=-3;z<=3;z++)h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
            p.setPos(body.position().add(MachineNodes.rotate(outward,yaw)));
            look(p,body.position().add(MachineNodes.rotate(local,yaw)));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN,2));
            p.interactOn(body,InteractionHand.MAIN_HAND,MachineNodes.rotate(local,yaw));
            h.assertTrue(body.kind(node)==1 && p.getMainHandItem().getCount()==1,"Native creative wrapper transfers one fan at node "+node+" yaw "+yaw+" kind="+body.kind(node)+" count="+p.getMainHandItem().getCount()+" modify="+body.canModify(p)+" ray="+body.verifiedHit(p,MachineNodes.rotate(local,yaw))+" eye="+p.getEyePosition()+" target="+body.position());
            p.interactOn(body,InteractionHand.MAIN_HAND,MachineNodes.rotate(local,yaw));
            h.assertTrue(p.getMainHandItem().getCount()==1,"Same-tick replay cannot consume a second part");
            body.discard();p.discard();
        }
        h.succeed();
    }
    @GameTest public void nativeIntentCannotSelectHiddenNodeOrBypassOwner(GameTestHelper h) {
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,4,2),EntitySpawnReason.COMMAND);
        var p=h.makeMockServerPlayerInLevel();body.setOwner(p.getUUID());p.setPos(body.position().add(0,0,-2));look(p,body.position().add(MachineNodes.point(2)));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN,2));
        p.interactOn(body,InteractionHand.MAIN_HAND,MachineNodes.point(3));
        p.interactOn(body,InteractionHand.MAIN_HAND,new Vec3(Double.NaN,0,0));
        p.interactOn(body,InteractionHand.OFF_HAND,MachineNodes.point(2));
        h.assertTrue(body.fanMask()==0 && p.getMainHandItem().getCount()==2,"Hidden side, malformed hit and offhand cannot mutate nodes");
        var guest=h.makeMockServerPlayerInLevel();guest.setPos(p.position());look(guest,body.position().add(MachineNodes.point(2)));
        guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));
        guest.interactOn(body,InteractionHand.MAIN_HAND,MachineNodes.point(2));
        h.assertTrue(body.fanMask()==0 && guest.getMainHandItem().getCount()==1,"Second actor's real interaction leaves inventory unchanged");
        p.interactOn(body,InteractionHand.MAIN_HAND,MachineNodes.point(2));
        h.assertTrue(body.kind(2)==1 && p.getMainHandItem().getCount()==1,"Creator retains actual installation permission");
        p.setPos(body.position().add(0,0,-4));look(p,body.position().add(MachineNodes.point(3)));
        h.assertTrue(body.verifiedHit(p,MachineNodes.point(3))==null,"Server enforces vanilla interaction reach despite broader packet acceptance");
        guest.discard();p.discard();body.discard();h.succeed();
    }
    private static void look(net.minecraft.server.level.ServerPlayer p,Vec3 target) {
        var d=target.subtract(p.getEyePosition());p.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));
        p.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));
    }
    @GameTest(maxTicks=50) public void nativePanelReplayAndNonRiderIntent(GameTestHelper h) {
        for(int x=0;x<5;x++)for(int z=0;z<5;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,2,2),EntitySpawnReason.COMMAND);
        var p=h.makeMockServerPlayerInLevel();body.setOwner(p.getUUID());
        p.setPos(body.position().add(0,0,-2));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        body.toggleFromRider(p);h.assertTrue(!body.enabled(),"Unmounted player cannot toggle by rider intent");
        h.runAfterDelay(4,() -> {
            p.setPos(body.position().add(0,0,-2));p.setShiftKeyDown(true);
            var panel=new Vec3(0,.65,.42);look(p,body.position().add(panel));
            p.interactOn(body,InteractionHand.MAIN_HAND,panel);p.interactOn(body,InteractionHand.MAIN_HAND,panel);
            h.assertTrue(body.enabled() && !body.working(),"Native panel enables once; same-tick replay cannot switch it off");
        });
        h.runAfterDelay(8,() -> {
            p.setPos(body.position().add(0,0,-2));p.setShiftKeyDown(true);var panel=new Vec3(0,.65,.42);look(p,body.position().add(panel));
            p.interactOn(body,InteractionHand.MAIN_HAND,panel);h.assertTrue(!body.enabled(),"Next distinct panel click stops machinery");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));look(p,body.position().add(MachineNodes.point(2)));
            var eye=p.getEyePosition();var target=body.position().add(MachineNodes.point(2));
            var obstruction=BlockPos.containing(eye.add(target).scale(.5));h.getLevel().setBlockAndUpdate(obstruction,Blocks.STONE.defaultBlockState());
            h.assertTrue(body.verifiedHit(p,MachineNodes.point(2))==null,"Block between eye and selected face rejects hidden installation");
            body.discard();p.discard();h.succeed();
        });
    }
    @GameTest public void wingNeedsSpeedAndPreservesNativeSave(GameTestHelper h) {
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,6,2),EntitySpawnReason.COMMAND);
        var p=h.makeMockServerPlayerInLevel();p.setPos(body.position());body.setOwner(p.getUUID());
        var origin=body.blockPosition();for(int x=-2;x<=2;x++)for(int y=-3;y<=3;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.WING));h.assertTrue(body.install(p,4,InteractionHand.MAIN_HAND),"Wing transfers actual stack");
        body.setDeltaMovement(.3,-.6,0);double start=body.getY();body.tick();
        h.assertTrue(body.getY()>start-.2 && body.getY()<start && body.energy()==0,"Moving unpowered wing slows descent without upward lift or charge");
        body.setDeltaMovement(0,-.3,0);start=body.getY();body.tick();h.assertTrue(body.getY()<start-.3,"Stationary wing cannot hover");
        p.setPos(body.position());h.assertTrue(!body.recoverBody(p),"Wing counts as a real installed part");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());body.saveWithoutId(out);
        var loaded=MechanicsContent.MACHINE.create(h.getLevel(),EntitySpawnReason.LOAD);loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(loaded.kind(4)==3 && loaded.part(4).is(MechanicsContent.WING),"Format1 retains real new part and public kind");
        body.discard();p.discard();h.succeed();
    }
    @GameTest public void wheelRequiresContactAndWholePower(GameTestHelper h) {
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,5,2),EntitySpawnReason.COMMAND);
        var p=h.makeMockServerPlayerInLevel();p.setPos(body.position());body.setOwner(p.getUUID());
        var origin=body.blockPosition();for(int x=-2;x<=2;x++)for(int y=-3;y<=3;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.WHEEL,2));body.install(p,4,InteractionHand.MAIN_HAND);body.install(p,5,InteractionHand.MAIN_HAND);
        var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1);p.setItemInHand(InteractionHand.MAIN_HAND,battery);body.install(p,0,InteractionHand.MAIN_HAND);body.setEnabled(p,true);
        body.setOnGround(false);body.tick();h.assertTrue(body.energy()==1 && !body.working() && body.getDeltaMovement().horizontalDistanceSqr()==0,"Airborne wheels neither burn energy nor drive");
        body.setOnGround(true);body.tick();h.assertTrue(body.energy()==1 && !body.working(),"Two-wheel whole cost rejects single remaining charge");
        body.setEnabled(p,false);p.setPos(body.position());body.recover(p,0);battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,20);p.setItemInHand(InteractionHand.MAIN_HAND,battery);body.install(p,0,InteractionHand.MAIN_HAND);body.setEnabled(p,true);
        body.setOnGround(true);body.tick();h.assertTrue(body.energy()==18 && body.working() && body.getDeltaMovement().z>0,"Actual powered ground cycle drives and spends two");
        body.discard();p.discard();h.succeed();
    }
    @GameTest public void bodyPlacementChecksCollisionAndTransfersOnce(GameTestHelper h) {
        h.setBlock(new BlockPos(1,1,1),Blocks.STONE);
        var p=h.makeMockServerPlayerInLevel();var target=h.absolutePos(new BlockPos(1,1,1));
        p.setPos(target.getX()-2,target.getY()+1,target.getZ()+.5);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.BODY));
        var hit=new net.minecraft.world.phys.BlockHitResult(new Vec3(target.getX()+.5,target.getY()+1,target.getZ()+.5),net.minecraft.core.Direction.UP,target,false);
        var context=new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(MechanicsContent.BODY.useOn(context)!=net.minecraft.world.InteractionResult.FAIL && p.getMainHandItem().isEmpty(),"Body placement consumes actual survival hand once");
        var bodies=h.getLevel().getEntitiesOfClass(MachineEntity.class,new net.minecraft.world.phys.AABB(target).inflate(2));
        h.assertTrue(bodies.size()==1 && bodies.getFirst().owner().equals(p.getUUID()),"Placed body registers actual creator");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.BODY));
        h.assertTrue(MechanicsContent.BODY.useOn(context)==net.minecraft.world.InteractionResult.FAIL && p.getMainHandItem().getCount()==1,"Overlapping placement rejects without item loss");
        bodies.getFirst().discard();p.discard();h.succeed();
    }
    @GameTest public void wholeTickCostAndUnloadedBoundary(GameTestHelper h) {
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(1,2,1),EntitySpawnReason.COMMAND);
        var p=h.makeMockServerPlayerInLevel();p.setPos(body.position());body.setOwner(p.getUUID());
        var b=new ItemStack(EnergyContent.BATTERY);Batteries.charge(b,1);p.setItemInHand(InteractionHand.MAIN_HAND,b);body.install(p,0,InteractionHand.MAIN_HAND);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN,2));body.install(p,2,InteractionHand.MAIN_HAND);body.install(p,3,InteractionHand.MAIN_HAND);
        body.setEnabled(p,true);body.tick();
        h.assertTrue(body.energy()==1 && !body.working(),"Unfunded multi-fan tick spends nothing, even if thrusts cancel");
        p.setPos(body.position());body.setEnabled(p,false);body.recover(p,0);
        var charged=new ItemStack(EnergyContent.BATTERY);Batteries.charge(charged,317);p.setItemInHand(InteractionHand.MAIN_HAND,charged);body.install(p,0,InteractionHand.MAIN_HAND);body.setEnabled(p,true);
        h.assertTrue(!body.install(p,4,InteractionHand.MAIN_HAND) && !body.recover(p,0),"Running assembly cannot be edited");
        var absent=new BlockPos(20_000_000,128,20_000_000);h.assertTrue(!h.getLevel().hasChunkAt(absent),"Missing chunk fixture");
        body.setPos(absent.getX()+.5,absent.getY(),absent.getZ()+.5);body.setDeltaMovement(.3,0,0);var before=body.position();body.tick();
        h.assertTrue(body.energy()==317 && !body.working() && body.position().equals(before) && !h.getLevel().hasChunkAt(absent),"Unloaded boundary neither charges offline cost nor force loads destination");
        body.discard();p.discard();h.succeed();
    }
    @GameTest public void ownershipCapacityReplayAndExactStacks(GameTestHelper h) {
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(1,2,1),EntitySpawnReason.COMMAND);
        var p=h.makeMockServerPlayerInLevel();p.setPos(body.position());body.setOwner(p.getUUID());
        var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,317);battery.set(DataComponents.CUSTOM_NAME,Component.literal("P6 real battery"));
        p.setItemInHand(InteractionHand.MAIN_HAND,battery);
        h.assertTrue(body.install(p,0,InteractionHand.MAIN_HAND) && battery.isEmpty(),"Server hand transfers once");
        var snapshot=body.part(0);Batteries.charge(snapshot,500);
        h.assertTrue(body.energy()==317,"Snapshot cannot alter installed stack");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(EnergyContent.BATTERY));
        h.assertTrue(!body.install(p,1,InteractionHand.MAIN_HAND) && p.getMainHandItem().getCount()==1,"Second battery rejected untouched");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.REDSTONE_BLOCK));
        h.assertTrue(!body.install(p,3,InteractionHand.MAIN_HAND) && p.getMainHandItem().getCount()==1,"Moving redstone is not a power part");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN,2));
        h.assertTrue(body.install(p,2,InteractionHand.MAIN_HAND) && p.getMainHandItem().getCount()==1,"One fan consumed");
        h.assertTrue(!body.install(p,2,InteractionHand.MAIN_HAND) && !body.install(p,6,InteractionHand.MAIN_HAND),"Replay and invalid node rejected");
        var guest=h.makeMockServerPlayerInLevel();guest.setPos(body.position());
        body.setOwner(java.util.UUID.randomUUID()); // setter cannot replace an established owner
        guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));
        h.assertTrue(!body.recover(guest,0) && !body.install(guest,3,InteractionHand.MAIN_HAND) && !body.setEnabled(guest,true),"Other actor cannot take, install or start");
        p.setPos(body.position().add(10,0,0));h.assertTrue(!body.recover(p,0),"Remote mutation rejected");p.setPos(body.position());
        p.setHealth(0);h.assertTrue(!body.recover(p,0) && !body.setEnabled(p,true),"Dead owner cannot transfer or start machinery");p.setHealth(20);
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for(int n=0;n<p.getInventory().getContainerSize();n++)p.getInventory().setItem(n,new ItemStack(Items.STONE,64));
        h.assertTrue(!body.recover(p,0) && body.energy()==317,"Full inventory leaves exact battery installed");
        p.getInventory().setItem(8,ItemStack.EMPTY);h.assertTrue(body.recover(p,0) && !body.recover(p,0),"Recover once only");
        h.assertTrue(Batteries.energy(p.getInventory().getItem(8))==317 && p.getInventory().getItem(8).getHoverName().getString().equals("P6 real battery"),"All original components preserved");
        guest.discard();p.discard();body.discard();h.succeed();
    }
    @GameTest public void rotatedNodesAndSavedMotion(GameTestHelper h) {
        for(int n=0;n<6;n++)for(float yaw:new float[]{0,90,180,270,33})h.assertTrue(MachineNodes.nearest(MachineNodes.rotate(MachineNodes.point(n),yaw),yaw)==n,"Stable local node mapping");
        h.assertTrue(MachineNodes.nearest(new Vec3(Double.NaN,0,0),0)==-1 && MachineNodes.nearest(new Vec3(4,0,0),0)==-1,"Malformed hits rejected");
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(1,2,1),EntitySpawnReason.COMMAND);var p=h.makeMockServerPlayerInLevel();p.setPos(body.position());body.setOwner(p.getUUID());
        var b=new ItemStack(EnergyContent.BATTERY);Batteries.charge(b,37);p.setItemInHand(InteractionHand.MAIN_HAND,b);body.install(p,0,InteractionHand.MAIN_HAND);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));body.install(p,2,InteractionHand.MAIN_HAND);body.setYRot(90);body.setDeltaMovement(.1,0,0);body.setEnabled(p,true);
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());body.saveWithoutId(out);
        var loaded=MechanicsContent.MACHINE.create(h.getLevel(),EntitySpawnReason.LOAD);loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(loaded.energy()==37 && loaded.kind(2)==1 && loaded.enabled() && loaded.owner().equals(p.getUUID()),"Format retains parts, owner and switch without refilling");
        h.assertTrue(loaded.position().equals(body.position()) && loaded.getYRot()==90 && loaded.getDeltaMovement().equals(body.getDeltaMovement()),"Native position, direction and motion retained");
        p.discard();body.discard();h.succeed();
    }
    @GameTest(maxTicks=150) public void nativeCollisionRiderAndFiniteThrust(GameTestHelper h) {
        for(int x=0;x<9;x++)for(int z=0;z<4;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        for(int y=2;y<5;y++)for(int z=0;z<4;z++)h.setBlock(new BlockPos(6,y,z),Blocks.STONE);
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,2,2),EntitySpawnReason.COMMAND);body.setYRot(-90);
        var p=h.makeMockServerPlayerInLevel();p.setPos(body.position());body.setOwner(p.getUUID());
        var b=new ItemStack(EnergyContent.BATTERY);Batteries.charge(b,40);p.setItemInHand(InteractionHand.MAIN_HAND,b);body.install(p,0,InteractionHand.MAIN_HAND);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));body.install(p,2,InteractionHand.MAIN_HAND);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(p.startRiding(body),"Native passenger accepted");body.setEnabled(p,true);double start=body.getX();
        h.runAfterDelay(60,() -> {
            h.assertTrue(body.getX()>start+1 && body.getBoundingBox().maxX<=h.absolutePos(new BlockPos(6,2,2)).getX()+.01,"Fan moves but native wall blocks movement");
            h.assertTrue(body.getY()>=h.absolutePos(new BlockPos(0,2,0)).getY()-.01 && body.onGround(),"Native floor collision");
            h.assertTrue(body.energy()==0 && !body.working(),"Depleted battery provides no more thrust");
            h.assertTrue(p.getVehicle()==body && p.position().distanceTo(body.position())<2,"Passenger follows body");
            body.setEnabled(p,false);p.stopRiding();p.setPos(body.position());
            h.assertTrue(!body.recoverBody(p),"Installed parts prevent body duplication");
            h.assertTrue(body.recover(p,0) && body.recover(p,2),"Stopped assembly can be fully dismantled");
            h.assertTrue(body.recoverBody(p) && !body.recoverBody(p),"Empty body returns exactly once");p.discard();h.succeed();
        });
    }
    @GameTest(maxTicks=100) public void exhaustedLiftFallsAgain(GameTestHelper h) {
        for(int x=0;x<5;x++)for(int z=0;z<5;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,4,2),EntitySpawnReason.COMMAND);var p=h.makeMockServerPlayerInLevel();p.setPos(body.position());body.setOwner(p.getUUID());
        var b=new ItemStack(EnergyContent.BATTERY);Batteries.charge(b,5);p.setItemInHand(InteractionHand.MAIN_HAND,b);body.install(p,0,InteractionHand.MAIN_HAND);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));body.install(p,1,InteractionHand.MAIN_HAND);body.setEnabled(p,true);double initial=body.getY();
        h.runAfterDelay(40,() -> {h.assertTrue(body.energy()==0 && !body.working() && body.getY()<initial && body.onGround(),"Finite upward fan cannot grant permanent no-gravity flight");p.discard();body.discard();h.succeed();});
    }
}
