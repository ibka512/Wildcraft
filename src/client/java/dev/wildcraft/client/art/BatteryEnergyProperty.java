package dev.wildcraft.client.art;
import com.mojang.serialization.MapCodec;
import dev.wildcraft.energy.Batteries;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
public record BatteryEnergyProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<BatteryEnergyProperty> CODEC=MapCodec.unit(new BatteryEnergyProperty());
    @Override public float get(ItemStack stack,ClientLevel level,ItemOwner owner,int seed){return Batteries.energy(stack);}
    @Override public MapCodec<BatteryEnergyProperty> type(){return CODEC;}
}
