package dev.wildcraft.mechanics;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;

public final class PartItem extends Item {
    private final String help;
    public PartItem(Properties properties,String help){super(properties);this.help=help;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag) {
        var rocket=stack.get(MechanicsContent.ROCKET_DATA);if(rocket!=null)lines.accept(Component.translatable("machine.wildcraft.fuel",rocket.ticks(),RocketData.DURATION));
        var spring=stack.get(MechanicsContent.SPRING_DATA);if(spring!=null)lines.accept(Component.translatable("machine.wildcraft.cooldown",spring.cooldown()));
        lines.accept(Component.translatable("machine.wildcraft.help."+help));
    }
}
