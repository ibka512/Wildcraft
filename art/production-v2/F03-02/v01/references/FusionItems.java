package dev.wildcraft.fuse;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;

/** Validates completely before changing either actual owned stack. */
public final class FusionItems {
    private FusionItems(){}
    public static boolean host(ItemStack s){return s.is(ItemTags.SWORDS)||s.is(ItemTags.AXES)||s.is(Items.SHIELD)||s.is(Items.ARROW)||s.is(Items.TIPPED_ARROW)||s.is(Items.SPECTRAL_ARROW);}
    public static boolean attach(ItemStack host,ItemStack material,HolderLookup.Provider registries){
        if(host==material||host.isEmpty()||host.getCount()!=1||!host(host)||host.has(FusionContent.DATA)||host.has(FusionContent.VIEW)||material.isEmpty()||material.has(FusionContent.DATA)||material.has(FusionContent.VIEW))return false;
        var capture=FusionData.capture(material,registries);if(capture.error().isPresent()||capture.result().isEmpty())return false;
        var d=capture.result().get();host.set(FusionContent.DATA,d);host.set(FusionContent.VIEW,new FusionVisual(BuiltInRegistries.ITEM.getKey(material.getItem()),FusionRules.kind(material),d.remaining(),material.hasFoil()));material.shrink(1);return true;
    }
    public static java.util.Optional<ItemStack> splitMaterial(ItemStack host,HolderLookup.Provider registries){
        var data=host.get(FusionContent.DATA);if(data==null)return java.util.Optional.empty();
        return data.material(registries).map(s -> {if(data.remaining()<FusionData.MAX_USES)s.set(FusionContent.WEAR,data.remaining());return s;});
    }
}
