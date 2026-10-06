package dev.wildcraft.client.cooking;

import dev.wildcraft.cooking.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Reads the same finite recipes as the pot; no duplicate recipe list or inventory access. */
public final class CookingGuide {
    private CookingGuide(){}
    public static List<Component> lines(int kind){
        var lines=new ArrayList<Component>();
        lines.add(Component.translatable(kind==0?"item.wildcraft.meal.vegetable":"hud.wildcraft.meal.name."+kind));
        for(var recipe:CookingRecipes.ALL)if(recipe.outcome().kind()==kind){
            var meal=recipe.outcome();
            var ingredients=Component.empty();for(var item:recipe.ingredients()){
                if(!ingredients.getString().isEmpty())ingredients.append(" + ");ingredients.append(new ItemStack(item).getHoverName());
            }
            if(kind!=0)lines.add(Component.literal((meal.strength()==1?"I":"II")+" · "+MealHud.time(meal.seconds())));
            lines.add(ingredients);
        }
        lines.add(Component.translatable("cooking.wildcraft.guide.bowl"));
        lines.add(Component.translatable("cooking.wildcraft.guide.heat"));
        return List.copyOf(lines);
    }
}
