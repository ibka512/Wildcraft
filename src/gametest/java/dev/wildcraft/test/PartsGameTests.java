package dev.wildcraft.test;

import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

public final class PartsGameTests {
    private static void room(GameTestHelper h) {
        for(int x=0;x<7;x++)for(int z=0;z<7;z++)for(int y=1;y<10;y++)h.setBlock(new BlockPos(x,y,z),y==1?Blocks.STONE:Blocks.AIR);
    }
    private static MachineEntity body(GameTestHelper h,ServerPlayer p,Vec3 pos) {
        var b=h.spawn(MechanicsContent.MACHINE,pos,EntitySpawnReason.COMMAND);b.setOwner(p.getUUID());p.setPos(b.position());return b;
    }
    private static void install(GameTestHelper h,MachineEntity b,ServerPlayer p,int node,ItemStack stack) {
        p.setPos(b.position());p.setItemInHand(InteractionHand.MAIN_HAND,stack);h.assertTrue(b.install(p,node,InteractionHand.MAIN_HAND),"Real test fixture part transfer");
    }
    private static MachineEntity reload(GameTestHelper h,MachineEntity b) {
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());b.saveWithoutId(out);
        var copy=MechanicsContent.MACHINE.create(h.getLevel(),EntitySpawnReason.LOAD);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));return copy;
    }
    @GameTest(maxTicks=110) public void finiteRocketLifecycle(GameTestHelper h) {
        room(h);for(int x=0;x<7;x++)for(int y=2;y<6;y++)h.setBlock(new BlockPos(x,y,4),Blocks.STONE);
        var p=h.makeMockServerPlayerInLevel();var b=body(h,p,new Vec3(2,2,2));var rocket=new ItemStack(MechanicsContent.ROCKET);
        rocket.set(DataComponents.CUSTOM_NAME,Component.literal("Finite named rocket"));install(h,b,p,2,rocket);b.setEnabled(p,true);
        int[] fuel={0};
        h.runAfterDelay(10,() -> {p.setPos(b.position());fuel[0]=b.rocketFuel(2);h.assertTrue(fuel[0]>0 && fuel[0]<80 && b.active(2),"Real world ignition uses finite fuel without battery");b.setEnabled(p,false);h.assertTrue(!b.recover(p,2),"Burning rocket cannot be removed after power off");});
        h.runAfterDelay(15,() -> {h.assertTrue(b.rocketFuel(2)<fuel[0] && !b.enabled(),"Switching off does not extinguish one-shot fuel");var copy=reload(h,b);int before=copy.rocketFuel(2);copy.tick();h.assertTrue(copy.rocketFuel(2)==before-1 && !copy.enabled(),"Reloaded ignition continues without refilling");copy.discard();});
        h.runAfterDelay(90,() -> {p.setPos(b.position());h.assertTrue(b.rocketFuel(2)==0 && b.part(2).is(MechanicsContent.SPENT_ROCKET) && !b.working(),"Exhaustion produces an inert real casing");h.assertTrue(b.recover(p,2),"Spent casing returns once");var stack=p.getMainHandItem();h.assertTrue(stack.is(MechanicsContent.SPENT_ROCKET) && stack.getHoverName().getString().equals("Finite named rocket"),"Conversion preserves original custom components");h.assertTrue(!b.install(p,3,InteractionHand.MAIN_HAND) && stack.getCount()==1,"Spent casing cannot be installed as new fuel");h.assertTrue(Batteries.charge(stack,1000)==0,"Charger API cannot refill rocket fuel");b.discard();p.discard();h.succeed();});
    }
    @GameTest(maxTicks=100) public void springTravelsWithCooldownAndNeedsGroundRearm(GameTestHelper h) {
        room(h);var p=h.makeMockServerPlayerInLevel();var b=body(h,p,new Vec3(2,2,2));install(h,b,p,1,new ItemStack(MechanicsContent.SPRING));
        var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,100);install(h,b,p,0,battery);b.setEnabled(p,true);
        h.runAfterDelay(8,() -> {h.assertTrue(b.energy()==80 && b.springCooldown(1)>0 && b.getY()>h.absolutePos(new BlockPos(2,2,2)).getY(),"Native ground spring launches once for twenty energy");var copy=reload(h,b);h.assertTrue(copy.springCooldown(1)==b.springCooldown(1) && !copy.part(1).get(MechanicsContent.SPRING_DATA).armed(),"Saved spring does not rearm");copy.discard();p.setPos(b.position());b.setEnabled(p,false);h.assertTrue(b.recover(p,1),"Spring cooldown returns with real item");h.assertTrue(p.getMainHandItem().get(MechanicsContent.SPRING_DATA).cooldown()>0,"Recovered spring retains cooldown");h.assertTrue(b.install(p,1,InteractionHand.MAIN_HAND),"Real same spring reinstalls");b.setEnabled(p,true);});
        h.runAfterDelay(52,() -> {h.assertTrue(b.energy()==80 && b.onGround() && b.springCooldown(1)==0,"Landing while enabled cannot repeat spring launch");p.setPos(b.position());b.setEnabled(p,false);});
        h.runAfterDelay(55,() -> {h.assertTrue(b.part(1).get(MechanicsContent.SPRING_DATA).armed(),"Stopped grounded cooled spring rearms");p.setPos(b.position());b.setEnabled(p,true);});
        h.runAfterDelay(60,() -> {h.assertTrue(b.energy()==60 && b.springCooldown(1)>0,"Rearmed launch pays again exactly once");b.discard();p.discard();h.succeed();});
    }
    @GameTest public void stabilizerIsFiniteAndDoesNotCancelGravity(GameTestHelper h) {
        room(h);var p=h.makeMockServerPlayerInLevel();var b=body(h,p,new Vec3(2,6,2));install(h,b,p,4,new ItemStack(MechanicsContent.STABILIZER));
        var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,20);install(h,b,p,0,battery);b.setEnabled(p,true);b.setDeltaMovement(.3,-.2,.2);b.tick();
        h.assertTrue(b.energy()==18 && b.active(4) && b.getDeltaMovement().x<.25 && b.getDeltaMovement().y<-.2,"Paid stabilization damps lateral drift and retains gravity");
        p.setPos(b.position());b.setEnabled(p,false);b.recover(p,0);battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1);install(h,b,p,0,battery);b.setEnabled(p,true);b.setDeltaMovement(.3,-.2,.2);b.tick();
        h.assertTrue(b.energy()==1 && !b.active(4) && b.getDeltaMovement().x>.27,"Insufficient whole fee gives no free stabilization");b.discard();p.discard();h.succeed();
    }
    @GameTest(maxTicks=60) public void buoyancyOnlySupportsBoundedWaterLoad(GameTestHelper h) {
        room(h);for(int x=0;x<7;x++)for(int z=0;z<7;z++)for(int y=2;y<=5;y++)h.setBlock(new BlockPos(x,y,z),x==0||x==6||z==0||z==6?Blocks.STONE:Blocks.WATER);
        var p=h.makeMockServerPlayerInLevel();var b=body(h,p,new Vec3(2,3,2));install(h,b,p,0,new ItemStack(MechanicsContent.BUOYANCY));double start=b.getY();double[] loadedY={0};
        h.runAfterDelay(10,() -> {h.assertTrue(b.getY()>start+.2 && b.active(0) && b.energy()==0,"Native water float rises without electricity");
            for(int n=1;n<6;n++)install(h,b,p,n,new ItemStack(MechanicsContent.WING));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.startRiding(b);b.setDeltaMovement(Vec3.ZERO);loadedY[0]=b.getY();h.assertTrue(b.mass()>1.75,"Six parts and passenger exceed single float capacity");});
        h.runAfterDelay(20,() -> {h.assertTrue(b.getY()<loadedY[0]-.2 && !b.active(0),"Overloaded float cannot support excess weight");p.stopRiding();
            b.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,8,2))));b.setDeltaMovement(Vec3.ZERO);double y=b.getY();b.tick();h.assertTrue(b.getY()<y && !b.active(0),"Air provides no buoyant lift");
            for(int x=1;x<6;x++)for(int z=1;z<6;z++)for(int yy=2;yy<=5;yy++)h.setBlock(new BlockPos(x,yy,z),Blocks.LAVA);
            b.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,3,2))));b.setDeltaMovement(Vec3.ZERO);y=b.getY();b.tick();h.assertTrue(b.getY()<y && !b.active(0),"Lava provides no buoyant lift");b.discard();p.discard();h.succeed();});
    }
    @GameTest public void combinationIsAtomicAndMissingChunksFreezeResources(GameTestHelper h) {
        room(h);var p=h.makeMockServerPlayerInLevel();var b=body(h,p,new Vec3(2,2,2));
        install(h,b,p,2,new ItemStack(MechanicsContent.FAN));install(h,b,p,1,new ItemStack(MechanicsContent.SPRING));install(h,b,p,4,new ItemStack(MechanicsContent.STABILIZER));
        var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,22);install(h,b,p,0,battery);b.setEnabled(p,true);b.setOnGround(true);b.tick();
        h.assertTrue(b.energy()==22 && b.activeMask()==0 && b.springCooldown(1)==0,"Fee 23 cannot consume partial battery 22 or trigger any powered part");
        p.setPos(b.position());b.setEnabled(p,false);b.recover(p,0);battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,23);install(h,b,p,0,battery);b.setEnabled(p,true);b.setOnGround(true);b.tick();
        h.assertTrue(b.energy()==0 && b.active(1) && b.active(2) && b.active(4) && b.springCooldown(1)==40,"Whole 23 pays exactly one combined powered cycle");
        p.setPos(b.position());b.setEnabled(p,false);install(h,b,p,3,new ItemStack(MechanicsContent.ROCKET));b.setEnabled(p,true);
        var absent=new BlockPos(20_000_000,128,20_000_000);h.assertTrue(!h.getLevel().hasChunkAt(absent),"Unloaded fixture");b.setPos(Vec3.atCenterOf(absent));b.tick();
        h.assertTrue(b.rocketFuel(3)==80 && b.springCooldown(1)==40 && !h.getLevel().hasChunkAt(absent),"Missing destination neither burns fuel nor advances cooldown nor force-loads chunks");b.discard();p.discard();h.succeed();
    }
}
