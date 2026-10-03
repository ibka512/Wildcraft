package dev.wildcraft.cooking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A finite, order-independent first batch. One item from each occupied ingredient slot is used. */
public final class CookingRecipes {
    public record Recipe(List<Item> ingredients, MealData outcome) { }
    public static final List<Recipe> ALL = List.of(
            recipe(MealData.NONE, 0, 0, Items.CARROT, Items.POTATO),
            recipe(MealData.WARMTH, 1, 120, Items.BEETROOT, Items.POTATO),
            recipe(MealData.WARMTH, 2, 180, Items.BEETROOT, Items.POTATO, Items.CARROT),
            recipe(MealData.COOLING, 1, 120, Items.APPLE, Items.MELON_SLICE),
            recipe(MealData.COOLING, 2, 180, Items.APPLE, Items.MELON_SLICE, Items.SWEET_BERRIES),
            recipe(MealData.RECOVERY, 1, 60, Items.RED_MUSHROOM, Items.SUGAR),
            recipe(MealData.RECOVERY, 2, 90, Items.RED_MUSHROOM, Items.BROWN_MUSHROOM, Items.SUGAR));
    private CookingRecipes() { }
    private static Recipe recipe(int kind, int strength, int seconds, Item... ingredients) {
        return new Recipe(List.of(ingredients), new MealData(1, kind, strength, seconds));
    }
    public static MealData match(List<ItemStack> slots) {
        if (slots.size() != 3) return null;
        var supplied = new ArrayList<Item>();
        for (ItemStack stack : slots) if (!stack.isEmpty()) supplied.add(stack.getItem());
        Comparator<Item> order = Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString());
        supplied.sort(order);
        for (Recipe recipe : ALL) {
            var needed = new ArrayList<>(recipe.ingredients()); needed.sort(order);
            if (needed.equals(supplied)) return recipe.outcome();
        }
        return null;
    }
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && ALL.stream().anyMatch(r -> r.ingredients().contains(stack.getItem()));
    }
}
