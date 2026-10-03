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
}
