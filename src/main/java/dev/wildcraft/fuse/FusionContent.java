package dev.wildcraft.fuse;

import com.mojang.serialization.Codec;
import dev.wildcraft.Wildcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.*;

public final class FusionContent {
    // A full snapshot never leaves the server through equipment, inventory or item entity tracking.
    public static final DataComponentType<FusionData> DATA=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Wildcraft.id("fusion"),DataComponentType.<FusionData>builder().persistent(FusionData.CODEC).networkSynchronized(ByteBufCodecs.INT.map(FusionData::projection,FusionData::networkHash)).build());
    public static final DataComponentType<FusionVisual> VIEW=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Wildcraft.id("fusion_view"),DataComponentType.<FusionVisual>builder().persistent(FusionVisual.CODEC).networkSynchronized(FusionVisual.STREAM_CODEC).build());
    public static final DataComponentType<Integer> WEAR=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Wildcraft.id("fusion_wear"),DataComponentType.<Integer>builder().persistent(Codec.intRange(1,32)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    private FusionContent(){}
    public static void initialize(){FusionActions.initialize();FusionCombat.initialize();FusionViews.initialize();}
}
