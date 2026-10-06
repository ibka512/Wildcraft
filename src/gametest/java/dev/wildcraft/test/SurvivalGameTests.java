package dev.wildcraft.test;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.player.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class SurvivalGameTests {
    @GameTest public void inventoryDiscoversAllNewRecipesWithoutAwardCommand(GameTestHelper h){
        String[] names={"wing","wheel","rocket","spring","stabilizer","buoyancy","fabricator","spent_rocket_recycling"};
        Item[] triggers={Items.LEATHER,Items.IRON_INGOT,Items.FIRE_CHARGE,Items.IRON_INGOT,Items.REDSTONE,Items.OAK_BOAT,Items.REDSTONE_BLOCK,MechanicsContent.SPENT_ROCKET};
        for(int n=0;n<names.length;n++){
            var p=h.makeMockServerPlayerInLevel();p.initInventoryMenu();var key=ResourceKey.<Recipe<?>>create(Registries.RECIPE,Wildcraft.id(names[n]));
            h.assertTrue(!p.getRecipeBook().contains(key),"Fresh recipe starts unknown: "+names[n]);
            p.getInventory().add(new ItemStack(triggers[n]));p.inventoryMenu.broadcastChanges();
            h.assertTrue(p.getRecipeBook().contains(key),"Native inventory advancement reveals recipe: "+names[n]);p.discard();
        }h.succeed();
    }
    @GameTest public void allPackagedCraftsUseNativeMatcherAndFiniteInitialState(GameTestHelper h){
        for(var name:SurvivalRecipes.ALL){
            var pattern=SurvivalRecipes.read(name);var input=CraftingInput.of(3,3,pattern.inputs());
            var recipe=h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow(()->new AssertionError("No native recipe "+name));
            var result=recipe.value().assemble(input);h.assertTrue(result.is(pattern.output())&&result.getCount()==1,"Native packaged output: "+name);
            if(result.is(EnergyContent.BATTERY))h.assertTrue(Batteries.energy(result)==0,"Crafted battery is empty");
            if(result.is(MechanicsContent.ROCKET))h.assertTrue(result.get(MechanicsContent.ROCKET_DATA).equals(RocketData.FRESH),"Crafted rocket has finite fresh fuel");
            if(result.is(MechanicsContent.SPRING))h.assertTrue(result.get(MechanicsContent.SPRING_DATA).equals(SpringData.FRESH),"Crafted spring retains formal initial state");
        }
        var compass=SurvivalRecipes.read("compass");var oldInput=CraftingInput.of(3,3,compass.inputs());
        var original=h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,oldInput,h.getLevel()).orElseThrow();
        h.assertTrue(original.value().assemble(oldInput).is(Items.COMPASS),"Old conflicting arrangement remains native compass");h.succeed();
    }
    @GameTest public void loadedPowerBudgetMeasurements(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();var origin=h.absolutePos(new BlockPos(2,3,2));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        for(var kinds:new int[][]{{6,6},{1},{6,6,1,7},{3}}){
            var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,3,2),EntitySpawnReason.COMMAND);p.setPos(body.position());body.setOwner(p.getUUID());
            var b=new ItemStack(EnergyContent.BATTERY);Batteries.charge(b,1000);p.setItemInHand(InteractionHand.MAIN_HAND,b);h.assertTrue(body.install(p,0,InteractionHand.MAIN_HAND),"Install finite budget");
            for(int i=0;i<kinds.length;i++){Item part=switch(kinds[i]){case 1->MechanicsContent.FAN;case 3->MechanicsContent.WING;case 6->MechanicsContent.WHEEL;default->MechanicsContent.STABILIZER;};p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(part));h.assertTrue(body.install(p,i+1,InteractionHand.MAIN_HAND),"Install measured part");}
            body.setEnabled(p,true);int ticks=0,expected=kinds[0]==3?200:kinds.length==4?200:kinds[0]==1?1000:500;
            while(ticks<expected){body.setPos(Vec3.atBottomCenterOf(origin));body.setOnGround(true);body.setDeltaMovement(Vec3.ZERO);body.tick();ticks++;}
            h.assertTrue(body.energy()==(kinds[0]==3?1000:0),"Actual loaded resource use matches measured duration");
            System.out.println("WILDCRAFT BALANCE "+Arrays.toString(kinds)+" loadedTicks="+ticks+" remaining="+body.energy());body.discard();
        }p.discard();h.succeed();
    }
}
