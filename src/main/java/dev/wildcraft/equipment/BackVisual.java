package dev.wildcraft.equipment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Session-only visual ownership. Tokens identify references without disclosing inventory slots or data. */
public record BackVisual(Entry melee, Entry shield, Entry ranged) {
    public record Entry(long reference, int owner) {
        static final Entry EMPTY = new Entry(0, 0);
        static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Entry::reference, ByteBufCodecs.VAR_INT, Entry::owner, Entry::new);
    }
    public static final BackVisual EMPTY = new BackVisual(Entry.EMPTY, Entry.EMPTY, Entry.EMPTY);
    public static final StreamCodec<RegistryFriendlyByteBuf, BackVisual> CODEC = StreamCodec.composite(
            Entry.CODEC, BackVisual::melee, Entry.CODEC, BackVisual::shield, Entry.CODEC, BackVisual::ranged, BackVisual::new);
    public Entry item(int category) { return category == 0 ? melee : category == 1 ? shield : ranged; }
}
