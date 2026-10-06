package dev.wildcraft.test;
import dev.wildcraft.cooking.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.fabrication.*;
import dev.wildcraft.art.ArtFeedback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;

public final class SecondArtGameTests {
    @GameTest public void snapshotsExcludeInventoriesAndCommittedResult(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,FabricationContent.BLOCK);var machine=h.getBlockEntity(pos,FabricatorEntity.class);
        machine.setItem(0,new ItemStack(Items.COPPER_INGOT,8));machine.setItem(1,new ItemStack(Items.REDSTONE,4));
        var p=h.makeMockServerPlayerInLevel();p.setPos(Vec3.atCenterOf(machine.getBlockPos()).add(0,0,-2));h.assertTrue(machine.start(p),"Actual committed job starts");var pending=machine.pendingResult();
        var snapshot=machine.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(snapshot.getIntOr("ArtState",-1)==2&&!snapshot.contains("Items")&&!snapshot.contains("PendingOutput")&&!snapshot.contains("FabricationTicks"),"World appearance must not expose hidden result, inventory or speculative progress");
        var save=machine.saveWithFullMetadata(h.getLevel().registryAccess());h.assertTrue(!save.contains("WildcraftArtSnapshot")&&save.contains("PendingOutput"),"Art packets do not replace durable job format");
        h.assertTrue(ItemStack.matches(pending,machine.pendingResult())&&machine.getItem(0).getCount()==4,"Building snapshot leaves real output and paid materials unchanged");h.succeed();
    }
    @GameTest public void emptyBatteryIsDifferentFromAbsentBattery(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,EnergyContent.CHARGER);var charger=h.getBlockEntity(pos,ChargerEntity.class);
        var absent=charger.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(!absent.getBooleanOr("ArtBattery",true),"Empty slot is absent");
        charger.setItem(0,new ItemStack(EnergyContent.BATTERY));var empty=charger.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(empty.getBooleanOr("ArtBattery",false)&&empty.getIntOr("ArtEnergy",-1)==0,"Actual empty battery is still present");
        Batteries.charge(charger.getItem(0),999);var used=charger.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(used.getIntOr("ArtEnergy",-1)==999&&!used.contains("Items"),"Snapshot carries exact charge without item components");h.succeed();
    }
    @GameTest public void artFeedbackRoundTripIsBounded(GameTestHelper h){
        var event=new ArtFeedback(h.getLevel().dimension().identifier(),93,java.util.UUID.randomUUID(),new Vec3(1,2,3),11,4);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());try{ArtFeedback.CODEC.encode(buffer,event);h.assertTrue(event.equals(ArtFeedback.CODEC.decode(buffer)),"Real packet codec round trips dimension, sequence, source and bounded event");}finally{buffer.release();}
        boolean refused=false;try{new ArtFeedback(event.dimension(),1,event.source(),event.position(),12,0);}catch(IllegalArgumentException ex){refused=true;}h.assertTrue(refused,"Unknown sound cannot index client cue table");h.succeed();
    }
}
