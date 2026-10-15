package dev.wildcraft.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;

/** Concrete native weapons. Native spears retain the dedicated piercing path; arbitrary piercing items stay vanilla. */
public enum MeleeWeapon {
    SWORD(12,5,10,12,1.5F,.4F), AXE(20,9,16,18,1.8F,.8F),
    TRIDENT(16,7,12,14,1.4F,.3F), MACE(26,12,20,22,1.6F,.6F), SPEAR(18,8,13,16,1.4F,.2F);
    public final int threshold,lightRecovery,strongRecovery;
    public final double cost;
    public final float multiplier,knockback;
    MeleeWeapon(int threshold,int light,int strong,double cost,float multiplier,float knockback){
        this.threshold=threshold;lightRecovery=light;strongRecovery=strong;this.cost=cost;this.multiplier=multiplier;this.knockback=knockback;
    }
    public int recovery(boolean charged){return charged?strongRecovery:lightRecovery;}
    public byte id(){return (byte)ordinal();}
    public static MeleeWeapon byId(byte id){return id>=0&&id<values().length?values()[id]:SWORD;}
    public static boolean nativeSpear(ItemStack stack){return stack.is(Items.WOODEN_SPEAR)||stack.is(Items.STONE_SPEAR)||stack.is(Items.COPPER_SPEAR)||stack.is(Items.IRON_SPEAR)||stack.is(Items.GOLDEN_SPEAR)||stack.is(Items.DIAMOND_SPEAR)||stack.is(Items.NETHERITE_SPEAR);}
    public static MeleeWeapon of(ItemStack stack){
        if(nativeSpear(stack)&&stack.has(DataComponents.PIERCING_WEAPON))return SPEAR;
        if(stack.has(DataComponents.PIERCING_WEAPON))return null;
        if(stack.is(Items.TRIDENT))return TRIDENT;
        if(stack.is(Items.MACE))return MACE;
        if(stack.is(ItemTags.AXES))return AXE;
        return stack.is(ItemTags.SWORDS)?SWORD:null;
    }
}
