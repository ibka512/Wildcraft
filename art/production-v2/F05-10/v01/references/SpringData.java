package dev.wildcraft.mechanics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;

/** Cooldown and rearm permission travel with the actual spring. */
public record SpringData(int schema, int cooldown, boolean armed) {
    public static final int COOLDOWN=40;
    public static final SpringData FRESH=new SpringData(1,0,true);
    public static final Codec<SpringData> CODEC=RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1,1).fieldOf("schema").forGetter(SpringData::schema),
            Codec.intRange(0,COOLDOWN).fieldOf("cooldown").forGetter(SpringData::cooldown),
            Codec.BOOL.fieldOf("armed").forGetter(SpringData::armed)).apply(i,SpringData::new));
    public static final StreamCodec<ByteBuf,SpringData> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,SpringData::schema,ByteBufCodecs.VAR_INT,SpringData::cooldown,ByteBufCodecs.BOOL,SpringData::armed,SpringData::new);
    public SpringData { if(schema!=1 || cooldown<0 || cooldown>COOLDOWN)throw new IllegalArgumentException("Invalid spring state"); }
}
