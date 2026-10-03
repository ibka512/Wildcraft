package dev.wildcraft.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.wildcraft.energy.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class EnergyGameTests {
    @GameTest public void finiteChargeSpendAndCodec(GameTestHelper h) {
        var battery=new ItemStack(EnergyContent.BATTERY);
        h.assertTrue(Batteries.energy(battery)==0,"Crafted/default battery starts empty");
        h.assertTrue(Batteries.charge(battery,1500)==1000 && Batteries.charge(battery,20)==0,"Charging cannot exceed capacity");
        h.assertTrue(!Batteries.consume(battery,1001) && Batteries.energy(battery)==1000,"Unfunded action changes nothing");
        h.assertTrue(Batteries.consume(battery,731) && Batteries.energy(battery)==269,"Exact finite cost");
        h.assertTrue(Batteries.charge(battery,-5)==0 && !Batteries.consume(battery,-3),"Invalid negative transfer cannot generate energy");
        int added=0,spent=0,start=Batteries.energy(battery);
        for(int n=0;n<1000;n++){int amount=n%37;added+=Batteries.charge(battery,amount);int cost=n%31;if(Batteries.consume(battery,cost))spent+=cost;h.assertTrue(Batteries.energy(battery)==start+added-spent,"Energy conservation across 1000 operations");}
        var original=battery.get(EnergyContent.BATTERY_DATA);var encoded=BatteryData.CODEC.encodeStart(JsonOps.INSTANCE,original).getOrThrow();
        h.assertTrue(BatteryData.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow().equals(original),"Charge component roundtrip stable");
        for(String raw:new String[]{"{\"schema\":2,\"energy\":10}","{\"schema\":1,\"energy\":-1}","{\"schema\":1,\"energy\":1001}"})h.assertTrue(BatteryData.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(raw)).error().isPresent(),"Malformed/future charge rejected");
        h.assertTrue(Batteries.charge(new ItemStack(Items.REDSTONE_BLOCK),20)==0,"Carried redstone block is not a battery");
        var absent=new BlockPos(20_000_000,128,20_000_000);h.assertTrue(!h.getLevel().hasChunkAt(absent),"Fixture chunk absent");
        h.assertTrue(FixedEnergy.allocate(h.getLevel(),absent,20)==0 && !h.getLevel().hasChunkAt(absent),"Energy API never loads a missing caller chunk");h.succeed();
    }
    @GameTest public void sharedFixedSourceAndFairNativeTicks(GameTestHelper h) {
        var level=h.getLevel();var source=h.absolutePos(new BlockPos(2,2,2));var aPos=source.east();var bPos=source.west();
        level.setBlockAndUpdate(source,Blocks.REDSTONE_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(aPos,EnergyContent.CHARGER.defaultBlockState());level.setBlockAndUpdate(bPos,EnergyContent.CHARGER.defaultBlockState());
        var a=(ChargerEntity)level.getBlockEntity(aPos);var b=(ChargerEntity)level.getBlockEntity(bPos);
        a.setItem(0,new ItemStack(EnergyContent.BATTERY));b.setItem(0,new ItemStack(EnergyContent.BATTERY));
        int x=FixedEnergy.allocate(level,aPos,20),y=FixedEnergy.allocate(level,bPos,20);
        h.assertTrue(x+y==20,"Two adjacent chargers share one capped source");
        var selected=x>0?a:b;var other=x>0?bPos:aPos;
        var selectedPos=x>0?aPos:bPos;var second=selectedPos.above();level.setBlockAndUpdate(second,Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertTrue(FixedEnergy.allocate(level,selectedPos,20)==0,"Multiple sources and repeated calls cannot exceed one charger's tick budget");
        level.setBlockAndUpdate(second,Blocks.AIR.defaultBlockState());
        selected.getItem(0).set(EnergyContent.BATTERY_DATA,new BatteryData(1,1000));
        h.assertTrue(FixedEnergy.allocate(level,other,20)==0,"Changing eligible candidates cannot claim the same source twice in one tick");
        a.setItem(0,new ItemStack(EnergyContent.BATTERY));b.setItem(0,new ItemStack(EnergyContent.BATTERY));long start=level.getGameTime();
        h.runAfterDelay(12,() -> {
            int ea=Batteries.energy(a.getItem(0)),eb=Batteries.energy(b.getItem(0));
            h.assertTrue(ea>0 && eb>0 && Math.abs(ea-eb)<=40,"Real world ticks rotate supply fairly");
            h.assertTrue(ea+eb<=20*(level.getGameTime()-start+1),"Total real output stays under the source tick budget");
            level.setBlockAndUpdate(source,Blocks.REDSTONE_TORCH.defaultBlockState());
            h.assertTrue(!FixedEnergy.hasSource(level,aPos) && FixedEnergy.allocate(level,aPos,20)==0,"Redstone signal is not stored energy");
            int before=Batteries.energy(a.getItem(0));ChargerEntity.tick(level,aPos,a.getBlockState(),a);
            h.assertTrue(Batteries.energy(a.getItem(0))==before,"Removing the fixed source stops charging immediately");h.succeed();
        });
    }
    @GameTest public void saveTakeAndBreakPreserveFiniteStack(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(pos,EnergyContent.CHARGER.defaultBlockState());
        var charger=(ChargerEntity)level.getBlockEntity(pos);var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,317);charger.setItem(0,battery);
        var loaded=(ChargerEntity)BlockEntity.loadStatic(pos,charger.getBlockState(),charger.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        h.assertTrue(Batteries.energy(loaded.getItem(0))==317,"Charger reload does not refill charge");
        var p=h.makeMockServerPlayerInLevel();var menu=(ChargerMenu)charger.createMenu(4,p.getInventory(),p);
        for(int n=0;n<36;n++)p.getInventory().setItem(n,new ItemStack(Items.STONE,64));
        h.assertTrue(menu.quickMoveStack(p,0).isEmpty() && Batteries.energy(charger.getItem(0))==317,"Full inventory preserves installed battery");
        p.getInventory().setItem(8,ItemStack.EMPTY);menu.quickMoveStack(p,0);
        h.assertTrue(charger.getItem(0).isEmpty() && Batteries.energy(p.getInventory().getItem(8))==317,"Menu transfer preserves same energy once");
        charger.setItem(0,p.getInventory().removeItemNoUpdate(8));level.destroyBlock(pos,true);
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));int batteries=0,body=0;
        for(var e:drops){if(e.getItem().is(EnergyContent.BATTERY)){batteries+=e.getItem().getCount();h.assertTrue(Batteries.energy(e.getItem())==317,"Native dropped battery preserves charge");}if(e.getItem().is(EnergyContent.CHARGER_ITEM))body+=e.getItem().getCount();}
        h.assertTrue(batteries==1 && body==1,"Native break drops contents and charger exactly once");p.discard();h.succeed();
    }
}
