package dev.wildcraft.fabrication;

import dev.wildcraft.mechanics.MechanicsContent;
import dev.wildcraft.energy.EnergyContent;
import java.util.List;
import net.minecraft.world.item.*;

/** Fixed equal weights; fresh stacks preserve each part's native defaults. */
public final class FabricationPool {
    public static final List<Item> PARTS=List.of(MechanicsContent.WING,MechanicsContent.FAN,MechanicsContent.ROCKET,EnergyContent.BATTERY,MechanicsContent.SPRING,MechanicsContent.WHEEL,MechanicsContent.STABILIZER,MechanicsContent.BUOYANCY);
    private FabricationPool(){ }
    public static ItemStack result(int index){return new ItemStack(PARTS.get(index));}
    public static boolean valid(ItemStack stack){return stack.getCount()==1&&PARTS.contains(stack.getItem());}
}
