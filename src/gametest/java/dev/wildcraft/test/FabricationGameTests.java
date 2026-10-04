package dev.wildcraft.test;

import dev.wildcraft.fabrication.*;
import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

public final class FabricationGameTests {
    private static FabricatorEntity setup(GameTestHelper h){h.setBlock(new BlockPos(2,1,2),Blocks.STONE);h.setBlock(new BlockPos(2,2,2),FabricationContent.BLOCK);return h.getBlockEntity(new BlockPos(2,2,2),FabricatorEntity.class);}
    private static ServerPlayer user(GameTestHelper h,FabricatorEntity b){var p=h.makeMockServerPlayerInLevel();p.setPos(Vec3.atCenterOf(b.getBlockPos()).add(0,0,-2));return p;}
    private static void supply(FabricatorEntity b){b.setItem(0,new ItemStack(Items.COPPER_INGOT,8));b.setItem(1,new ItemStack(Items.REDSTONE,4));}
    private static void ticks(GameTestHelper h,FabricatorEntity b,int count){for(int n=0;n<count;n++)FabricatorEntity.tick(h.getLevel(),b.getBlockPos(),b.getBlockState(),b);}
    @GameTest public void repeatedAndTwoPlayersCommitOneJob(GameTestHelper h){
        var b=setup(h);supply(b);var p=user(h,b);var q=user(h,b);var a=(FabricatorMenu)b.createMenu(1,p.getInventory(),p);var c=(FabricatorMenu)b.createMenu(2,q.getInventory(),q);
        h.assertTrue(a.clickMenuButton(p,0),"Live shared menu accepts first real resource commit");var result=b.pendingResult();
        h.assertTrue(!a.clickMenuButton(p,0)&&!c.clickMenuButton(q,0)&&!a.clickMenuButton(p,-1),"Repeated and second-player requests cannot consume or reroll twice");
        h.assertTrue(b.getItem(0).getCount()==4&&b.getItem(1).getCount()==2&&ItemStack.matches(result,b.pendingResult()),"Single debit and exact immutable pending result");
        var external=b.pendingResult();external.shrink(1);h.assertTrue(b.hasJob(),"Read copy cannot change actual committed job");ticks(h,b,100);h.assertTrue(ItemStack.matches(result,b.getItem(2))&&!b.hasJob(),"One saved output becomes collectable");ticks(h,b,100);h.assertTrue(b.getItem(2).getCount()==1&&!a.clickMenuButton(p,0),"No automatic duplication and old output blocks another job");
        h.assertTrue(!a.slots.get(0).mayPlace(new ItemStack(Items.STONE))&&!a.slots.get(2).mayPlace(result),"Native slots reject wrong input and output insertion");p.setPos(20_000_000,128,20_000_000);h.assertTrue(!a.clickMenuButton(p,0),"Remote menu cannot start");p.discard();q.discard();h.succeed();
    }
    @GameTest public void fullOutputInventoryAndSavedJobStayExact(GameTestHelper h){
        var b=setup(h);supply(b);var p=user(h,b);h.assertTrue(b.start(p),"Begin once");ticks(h,b,37);var expected=b.pendingResult();
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());b.saveWithoutMetadata(out);var loaded=new FabricatorEntity(b.getBlockPos(),b.getBlockState());loaded.setLevel(h.getLevel());loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(loaded.progress()==37&&ItemStack.matches(expected,loaded.pendingResult())&&loaded.getItem(0).getCount()==4,"Persisted paid job never redraws");loaded.setItem(2,new ItemStack(Items.STONE));ticks(h,loaded,100);h.assertTrue(loaded.progress()==100&&loaded.hasJob()&&loaded.getItem(2).is(Items.STONE),"Occupied output retains exact pending result");loaded.setItem(2,ItemStack.EMPTY);ticks(h,loaded,1);h.assertTrue(ItemStack.matches(expected,loaded.getItem(2))&&!loaded.hasJob(),"Cleared output commits once");
        for(int n=0;n<36;n++)p.getInventory().setItem(n,new ItemStack(Items.STONE,64));var m=(FabricatorMenu)loaded.createMenu(1,p.getInventory(),p);h.assertTrue(m.quickMoveStack(p,2).isEmpty()&&ItemStack.matches(expected,loaded.getItem(2)),"Full player inventory keeps output in machine");p.getInventory().setItem(0,ItemStack.EMPTY);h.assertTrue(!m.quickMoveStack(p,2).isEmpty()&&loaded.getItem(2).isEmpty(),"One real free slot gets one output");p.discard();h.succeed();
    }
    @GameTest public void nativeLootAndPlacementCarryOneTask(GameTestHelper h){
        var b=setup(h);supply(b);var p=user(h,b);b.start(p);ticks(h,b,24);var expected=b.pendingResult();var pos=b.getBlockPos();
        var drops=Block.getDrops(b.getBlockState(),h.getLevel(),pos,b);h.assertTrue(drops.size()==1&&drops.getFirst().is(FabricationContent.ITEM)&&drops.getFirst().has(DataComponents.BLOCK_ENTITY_DATA)&&drops.getFirst().has(DataComponents.CONTAINER),"Actual loot modifier packages one job and inventory");
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(1)).isEmpty(),"Native removal does not scatter a second inventory");
        p.setItemInHand(InteractionHand.MAIN_HAND,drops.getFirst());var hit=new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos.below(),false);p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit));
        var placed=(FabricatorEntity)h.getLevel().getBlockEntity(pos);h.assertTrue(placed!=null&&p.getMainHandItem().isEmpty()&&placed.progress()==24&&ItemStack.matches(expected,placed.pendingResult())&&placed.getItem(0).getCount()==4&&placed.getItem(1).getCount()==2,"Native BlockItem restores same task and actual inventory without clearing or rerolling");ticks(h,placed,76);h.assertTrue(ItemStack.matches(expected,placed.getItem(2)),"Same carried result completes after remaining ticks");p.discard();h.succeed();
    }
    @GameTest public void freshPoolAndMalformedSaveAreBounded(GameTestHelper h){
        h.assertTrue(FabricationPool.PARTS.size()==8&&new java.util.HashSet<>(FabricationPool.PARTS).size()==8,"Eight unique equal entries");
        for(int n=0;n<8;n++){var s=FabricationPool.result(n);h.assertTrue(FabricationPool.valid(s),"Single valid fresh part");if(s.is(EnergyContent.BATTERY))h.assertTrue(Batteries.energy(s)==0,"Factory cannot manufacture charged energy");if(s.is(MechanicsContent.ROCKET))h.assertTrue(s.get(MechanicsContent.ROCKET_DATA).equals(RocketData.FRESH),"Fresh finite rocket");if(s.is(MechanicsContent.SPRING))h.assertTrue(s.get(MechanicsContent.SPRING_DATA).equals(SpringData.FRESH),"Fresh finite spring");}
        var b=setup(h);var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());b.saveWithoutMetadata(out);var bad=out.buildResult();bad.putBoolean("HasJob",true);boolean rejected=false;try{b.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),bad));}catch(IllegalArgumentException e){rejected=true;}h.assertTrue(rejected,"Corrupt committed result explicitly refuses to reroll");h.succeed();
    }
}
