package dev.wildcraft.fuse;
import net.minecraft.world.item.ItemStack;
public final class FusionRepair{
 private FusionRepair(){}
 public static boolean fused(ItemStack s){return s.has(FusionContent.DATA)||s.has(FusionContent.VIEW);}
 public static boolean two(ItemStack a,ItemStack b){return fused(a)&&fused(b);}
 public static void keep(ItemStack result,ItemStack a,ItemStack b){if(result.isEmpty())return;var source=fused(a)?a:b;var d=source.get(FusionContent.DATA);var v=source.get(FusionContent.VIEW);if(d!=null)result.set(FusionContent.DATA,d);if(v!=null)result.set(FusionContent.VIEW,v);}
}
