package dev.wildcraft.fuse;
import dev.wildcraft.Wildcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
public final class FusionRules{
    public static final int GENERIC=0,ROCK=1,METAL=2,FIRE=3,ICE=4,ELASTIC=5,INACTIVE=6;
    private static final java.util.List<TagKey<Item>> TAGS=java.util.stream.IntStream.range(1,6).mapToObj(n -> TagKey.create(Registries.ITEM,Wildcraft.id("fusion/"+n))).toList();
    private FusionRules(){}
    public static int kind(ItemStack s){for(int n=0;n<5;n++)if(s.is(TAGS.get(n)))return n+1;return 0;}
    public static int bonus(int kind){return kind==ROCK?2:kind==METAL?3:1;}
    public static boolean arrow(ItemStack s){return s.is(Items.ARROW)||s.is(Items.TIPPED_ARROW)||s.is(Items.SPECTRAL_ARROW);}
}
