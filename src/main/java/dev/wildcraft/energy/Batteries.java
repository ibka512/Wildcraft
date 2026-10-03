package dev.wildcraft.energy;

import net.minecraft.world.item.ItemStack;

/** Server-side inventory transactions; callers must own this actual stack. No client energy packets. */
public final class Batteries {
    private Batteries(){ }
    public static int energy(ItemStack stack){
        if(!stack.is(EnergyContent.BATTERY))return 0;
        var data=stack.get(EnergyContent.BATTERY_DATA);return data==null?0:data.energy();
    }
    public static int charge(ItemStack stack,int offered){
        if(!stack.is(EnergyContent.BATTERY) || offered<=0)return 0;
        int before=energy(stack),accepted=Math.min(offered,BatteryData.CAPACITY-before);
        if(accepted>0)stack.set(EnergyContent.BATTERY_DATA,new BatteryData(1,before+accepted));
        return accepted;
    }
    /** An action which cannot afford its entire cost changes nothing. */
    public static boolean consume(ItemStack stack,int cost){
        if(!stack.is(EnergyContent.BATTERY) || cost<0 || cost>energy(stack))return false;
        if(cost>0)stack.set(EnergyContent.BATTERY_DATA,new BatteryData(1,energy(stack)-cost));
        return true;
    }
}
