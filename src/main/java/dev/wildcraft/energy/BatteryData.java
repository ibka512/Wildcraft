package dev.wildcraft.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;

/** Charge lives on the actual item, not on its current inventory position. */
public record BatteryData(int schema, int energy) {
    public static final int CAPACITY=1000;
    public static final BatteryData EMPTY=new BatteryData(1,0);
    public static final Codec<BatteryData> CODEC=RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1,1).fieldOf("schema").forGetter(BatteryData::schema),
            Codec.intRange(0,CAPACITY).fieldOf("energy").forGetter(BatteryData::energy)).apply(i,BatteryData::new));
    public static final StreamCodec<ByteBuf,BatteryData> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,BatteryData::schema,ByteBufCodecs.VAR_INT,BatteryData::energy,BatteryData::new);
    public BatteryData {
        if(schema!=1 || energy<0 || energy>CAPACITY)throw new IllegalArgumentException("Invalid battery charge");
    }
}
