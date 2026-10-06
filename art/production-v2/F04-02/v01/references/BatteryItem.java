package dev.wildcraft.energy;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;

public final class BatteryItem extends Item {
    public BatteryItem(Properties properties){super(properties);}
    @Override public boolean isBarVisible(ItemStack stack){return true;}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13F*Batteries.energy(stack)/BatteryData.CAPACITY);}
    @Override public int getBarColor(ItemStack stack){return Batteries.energy(stack)<100?0xD8A24D:0x7AAE83;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag){
        lines.accept(Component.translatable("energy.wildcraft.charge",Batteries.energy(stack),BatteryData.CAPACITY));
        lines.accept(Component.translatable("energy.wildcraft.battery_help"));
    }
}
