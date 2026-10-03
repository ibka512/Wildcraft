package dev.wildcraft.cooking;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class MealItem extends Item {
    public MealItem(Properties properties) { super(properties); }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
        var meal=stack.get(CookingContent.MEAL_DATA);
        var result=super.finishUsingItem(stack,level,entity);
        if(entity instanceof ServerPlayer p && meal!=null)CookingEffects.eat(p,meal);
        return result;
    }
    @Override public Component getName(ItemStack stack) {
        var meal=stack.get(CookingContent.MEAL_DATA);
        return meal==null?super.getName(stack):Component.translatable(meal.nameKey());
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,
                                          Consumer<Component> lines,TooltipFlag flag) {
        var meal=stack.get(CookingContent.MEAL_DATA);
        if(meal!=null && meal.kind()!=MealData.NONE) {
            lines.accept(Component.translatable("meal.wildcraft.details",meal.strength(),meal.seconds()));
            lines.accept(Component.translatable("meal.wildcraft.help."+meal.kind()));
        }
    }
}
