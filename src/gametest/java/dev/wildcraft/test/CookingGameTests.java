package dev.wildcraft.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.wildcraft.cooking.CookingRecipes;
import dev.wildcraft.cooking.MealData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CookingGameTests {
    @GameTest public void exactRecipesAndSlotOrder(GameTestHelper h) {
        h.assertTrue(CookingRecipes.ALL.size()==7,"Complete initial recipe batch");
        for (var recipe : CookingRecipes.ALL) {
            var slots=new ArrayList<ItemStack>();recipe.ingredients().forEach(i -> slots.add(new ItemStack(i,64)));
            while(slots.size()<3)slots.add(ItemStack.EMPTY);
            var expected=recipe.outcome();
            for(int i=0;i<6;i++) {
                Collections.rotate(slots,1);if(i==3)Collections.reverse(slots);
                h.assertTrue(expected.equals(CookingRecipes.match(slots)),"Ingredient arrangement and stack size never change the cooked outcome");
            }
        }
        h.assertTrue(CookingRecipes.match(List.of(new ItemStack(Items.CARROT),new ItemStack(Items.POTATO),new ItemStack(Items.DIAMOND)))==null,"Extra invalid ingredient cannot be silently consumed");
        h.assertTrue(CookingRecipes.match(List.of(new ItemStack(Items.CARROT),new ItemStack(Items.CARROT),ItemStack.EMPTY))==null,"Duplicate slot cannot stand in for a required ingredient");
        h.assertTrue(CookingRecipes.match(List.of(ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY))==null,"Empty input has no free output");
        h.succeed();
    }
    @GameTest public void savedMealRulesAreBoundedAndStable(GameTestHelper h) {
        for(var recipe : CookingRecipes.ALL) {
            var original=recipe.outcome();var json=MealData.CODEC.encodeStart(JsonOps.INSTANCE,original).getOrThrow();
            h.assertTrue(MealData.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow().equals(original),"Transfer/save cannot reroll meal content");
        }
        for(String raw : List.of("{\"schema\":2,\"kind\":1,\"strength\":1,\"seconds\":120}",
                "{\"schema\":1,\"kind\":0,\"strength\":2,\"seconds\":120}",
                "{\"schema\":1,\"kind\":1,\"strength\":0,\"seconds\":0}",
                "{\"schema\":1,\"kind\":1,\"strength\":1,\"seconds\":2147483647}")) {
            h.assertTrue(MealData.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(raw)).error().isPresent(),"Unsupported/malformed outcomes return a codec error instead of loading an unlimited buff");
        }
        h.succeed();
    }

    @GameTest public void cookingTransactionHeatBowlOutputAndReload(GameTestHelper h) {
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));var level=h.getLevel();
        level.setBlockAndUpdate(pos,dev.wildcraft.cooking.CookingContent.POT.defaultBlockState());
        var pot=(dev.wildcraft.cooking.CookingPotEntity)level.getBlockEntity(pos);
        pot.setItem(0,new ItemStack(Items.BEETROOT,2));pot.setItem(1,new ItemStack(Items.POTATO,2));
        for(int i=0;i<120;i++)dev.wildcraft.cooking.CookingPotEntity.tick(level,pos,pot.getBlockState(),pot);
        h.assertTrue(pot.progress()==0 && pot.getItem(0).getCount()==2,"No heat or bowl cannot consume ingredients");
        pot.setItem(3,new ItemStack(Items.BOWL,2));level.setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
        for(int i=0;i<40;i++)dev.wildcraft.cooking.CookingPotEntity.tick(level,pos,pot.getBlockState(),pot);
        h.assertTrue(pot.progress()==40 && pot.getItem(4).isEmpty(),"No early payment or output");
        level.setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT,false));
        dev.wildcraft.cooking.CookingPotEntity.tick(level,pos,pot.getBlockState(),pot);
        h.assertTrue(pot.progress()==40,"Extinguished heat pauses progress");
        var saved=pot.saveWithFullMetadata(level.registryAccess());
        var loaded=(dev.wildcraft.cooking.CookingPotEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,pot.getBlockState(),saved,level.registryAccess());
        h.assertTrue(loaded!=null && loaded.progress()==40 && loaded.getItem(0).getCount()==2,"Saved unfinished batch has unpaid ingredients and fixed progress");
        loaded.setLevel(level);level.setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
        for(int i=0;i<60;i++)dev.wildcraft.cooking.CookingPotEntity.tick(level,pos,loaded.getBlockState(),loaded);
        var output=loaded.getItem(4);h.assertTrue(output.is(dev.wildcraft.cooking.CookingContent.MEAL) && output.get(dev.wildcraft.cooking.CookingContent.MEAL_DATA).kind()==MealData.WARMTH,"A completed batch has a saved warming component");
        h.assertTrue(loaded.getItem(0).getCount()==1 && loaded.getItem(1).getCount()==1 && loaded.getItem(3).getCount()==1,"Only one ingredient per occupied slot and one real bowl charged");
        for(int i=0;i<120;i++)dev.wildcraft.cooking.CookingPotEntity.tick(level,pos,loaded.getBlockState(),loaded);
        h.assertTrue(loaded.getItem(0).getCount()==1 && loaded.getItem(4).getCount()==1,"Occupied output blocks new payment");
        var lastIngredient=loaded.removeItemNoUpdate(0);
        dev.wildcraft.cooking.CookingPotEntity.tick(level,pos,loaded.getBlockState(),loaded);
        h.assertTrue(loaded.status()==4,"Finished meal remains the primary prompt after ingredients run out");
        loaded.setItem(0,lastIngredient);loaded.removeItem(4,1);loaded.setItem(2,new ItemStack(Items.CARROT));
        dev.wildcraft.cooking.CookingPotEntity.tick(level,pos,loaded.getBlockState(),loaded);
        h.assertTrue(loaded.progress()==1,"Changing recipe resets prior progress");
        h.succeed();
    }
    @GameTest public void effectsMergeSaveRecoveryAndNativeFreezing(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();
        var strong=new MealData(1,MealData.WARMTH,2,180);var weak=new MealData(1,MealData.WARMTH,1,120);
        var effects=dev.wildcraft.cooking.MealEffects.EMPTY.eat(strong).elapse(175000);
        h.assertTrue(effects.eat(weak).equals(effects),"A weaker meal cannot extend or weaken a strong effect");
        effects=effects.eat(new MealData(1,MealData.COOLING,1,120)).eat(new MealData(1,MealData.RECOVERY,2,90));
        h.assertTrue(effects.warmth().millis()==5000 && effects.cooling().strength()==1 && effects.recovery().strength()==2,"Independent kinds coexist");
        h.assertTrue(effects.elapse(5000).warmth().strength()==0 && effects.elapse(5000).recovery().millis()==85000,"Expiry clears only the expired kind");
        p.setAttached(dev.wildcraft.cooking.CookingEffects.DATA,effects);
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());p.saveWithoutId(out);
        var restored=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),p.getGameProfile(),p.clientInformation());
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(dev.wildcraft.cooking.CookingEffects.get(restored).equals(effects),"Player reload preserves remaining milliseconds without refreshing");
        var stamina=new dev.wildcraft.player.StaminaData(1,0,50,0);
        h.assertTrue(stamina.tick(true,dev.wildcraft.cooking.CookingEffects.recoveryMultiplier(p)).stamina()==52,"Recovery II doubles eligible recovery");
        h.assertTrue(stamina.tick(false,2).stamina()==50 && stamina.tick(true,2).highestLevel()==0,"Airborne does not recover or increase capacity");
        p.setTicksFrozen(0);
        for(int i=1;i<=30;i++){p.tickCount=i;net.minecraft.world.entity.InsideBlockEffectType.FREEZE.effect().accept(p);}
        h.assertTrue(p.getTicksFrozen()==10,"Real native FREEZE callback is throttled to one in three, never reset");
        p.setTicksFrozen(p.getTicksRequiredToFreeze());p.tickCount=31;net.minecraft.world.entity.InsideBlockEffectType.FREEZE.effect().accept(p);
        h.assertTrue(p.isFullyFrozen(),"Warmth never clears fully frozen state");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(Items.LEATHER_BOOTS));p.setTicksFrozen(0);
        net.minecraft.world.entity.InsideBlockEffectType.FREEZE.effect().accept(p);h.assertTrue(p.getTicksFrozen()==0,"Native leather protection preserved");
        p.discard();restored.discard();h.succeed();
    }

    @GameTest public void fullInventoryAndNativeBreakKeepExactItems(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var level=h.getLevel();var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));
        level.setBlockAndUpdate(pos,dev.wildcraft.cooking.CookingContent.POT.defaultBlockState());
        var pot=(dev.wildcraft.cooking.CookingPotEntity)level.getBlockEntity(pos);
        pot.setItem(0,new ItemStack(Items.BEETROOT,3));pot.setItem(3,new ItemStack(Items.BOWL,2));
        var original=dev.wildcraft.cooking.CookingContent.meal(new MealData(1,MealData.WARMTH,2,180));pot.setItem(4,original.copy());
        var menu=(dev.wildcraft.cooking.CookingMenu)pot.createMenu(3,p.getInventory(),p);
        for(int n=0;n<36;n++)p.getInventory().setItem(n,new ItemStack(Items.STONE,64));
        h.assertTrue(menu.quickMoveStack(p,4).isEmpty() && ItemStack.matches(pot.getItem(4),original),"Full inventory cannot delete or duplicate cooked output");
        p.getInventory().setItem(8,ItemStack.EMPTY);menu.quickMoveStack(p,4);
        h.assertTrue(pot.getItem(4).isEmpty() && ItemStack.matches(p.getInventory().getItem(8),original),"Successful real menu move preserves exact component once");
        pot.setItem(4,original.copy());
        var tag=pot.saveWithFullMetadata(level.registryAccess());
        var loaded=(dev.wildcraft.cooking.CookingPotEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,pot.getBlockState(),tag,level.registryAccess());
        h.assertTrue(ItemStack.matches(loaded.getItem(4),original),"Saved output keeps all real stack components");
        level.destroyBlock(pos,true);
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
        int beet=0,bowl=0,meal=0,body=0;
        for(var entity:drops){var stack=entity.getItem();if(stack.is(Items.BEETROOT))beet+=stack.getCount();if(stack.is(Items.BOWL))bowl+=stack.getCount();if(stack.is(dev.wildcraft.cooking.CookingContent.MEAL)){meal+=stack.getCount();h.assertTrue(ItemStack.matches(stack,original),"Native dropped meal retains effect");}if(stack.is(dev.wildcraft.cooking.CookingContent.POT_ITEM))body+=stack.getCount();}
        h.assertTrue(beet==3 && bowl==2 && meal==1 && body==1,"Native block removal drops ingredients, output and pot exactly once");
        level.destroyBlock(pos,true);h.assertTrue(drops.size()==level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).size(),"Repeated removal cannot duplicate drops");
        p.discard();h.succeed();
    }
}
