package dev.wildcraft.test.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A bounded material snapshot; it makes no combat or combined-model claims. */
public record FuseSample(ItemStack material) {
    public static final int MAX_ENCODED_BYTES = 8192;
    public static final Codec<FuseSample> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("material").forGetter(FuseSample::material)
    ).apply(instance, FuseSample::new));

    public FuseSample {
        material = material.copyWithCount(1);
    }

    @Override
    public ItemStack material() {
        return material.copy();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FuseSample sample && ItemStack.matches(material, sample.material);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(material);
    }

    public static boolean tryFuse(ItemStack host, ItemStack material, HolderLookup.Provider registries) {
        if (host == material || !host.is(Items.IRON_SWORD) || host.getCount() != 1
                || host.has(ResearchFixtures.FUSE) || material.isEmpty()
                || material.has(ResearchFixtures.FUSE)) {
            return false;
        }
        FuseSample sample = new FuseSample(material);
        var encoded = CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), sample);
        if (encoded.error().isPresent() || encoded.result().isEmpty()
                || encoded.result().orElseThrow().toString().getBytes(StandardCharsets.UTF_8).length > MAX_ENCODED_BYTES) {
            return false;
        }
        host.set(ResearchFixtures.FUSE, sample);
        material.shrink(1);
        return true;
    }
}
