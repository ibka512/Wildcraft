package dev.wildcraft.mechanics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;

/** Actual one-shot fuel; moving the stack cannot create fresh fuel. */
public record RocketData(int schema, int ticks, boolean ignited) {
    public static final int DURATION=80;
    public static final RocketData FRESH=new RocketData(1,DURATION,false), EMPTY=new RocketData(1,0,true);
    public static final Codec<RocketData> CODEC=RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1,1).fieldOf("schema").forGetter(RocketData::schema),
            Codec.intRange(0,DURATION).fieldOf("ticks").forGetter(RocketData::ticks),
            Codec.BOOL.fieldOf("ignited").forGetter(RocketData::ignited)).apply(i,RocketData::new));
    public static final StreamCodec<ByteBuf,RocketData> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,RocketData::schema,ByteBufCodecs.VAR_INT,RocketData::ticks,ByteBufCodecs.BOOL,RocketData::ignited,RocketData::new);
    public RocketData { if(schema!=1 || ticks<0 || ticks>DURATION)throw new IllegalArgumentException("Invalid rocket fuel"); }
}
