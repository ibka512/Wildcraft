package dev.wildcraft.client.art;

import com.mojang.serialization.MapCodec;
import dev.wildcraft.cooking.CookingContent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

public record MealKindProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<MealKindProperty> CODEC=MapCodec.unit(new MealKindProperty());
    @Override public float get(ItemStack stack,ClientLevel level,ItemOwner owner,int seed){var meal=stack.get(CookingContent.MEAL_DATA);return meal==null?0:meal.kind();}
    @Override public MapCodec<MealKindProperty> type(){return CODEC;}
}
