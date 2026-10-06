package dev.wildcraft.network;

import dev.wildcraft.weather.WindRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Private transient local wind, including a dimension guard for client prediction. */
public record WindView(Identifier dimension,float x,float z,float precipitation,boolean outdoors){
    public WindView {
        if(dimension==null||!Float.isFinite(x)||!Float.isFinite(z)||!Float.isFinite(precipitation))throw new IllegalArgumentException("Invalid wind view");
        double speed=Math.hypot(x,z);if(speed>WindRules.MAX_SPEED){x*=WindRules.MAX_SPEED/speed;z*=WindRules.MAX_SPEED/speed;}
        precipitation=Math.clamp(precipitation,0,1);
        if(!outdoors){x=0;z=0;precipitation=0;}
    }
    public static final StreamCodec<ByteBuf,WindView> CODEC=StreamCodec.of((b,v)->{
        Identifier.STREAM_CODEC.encode(b,v.dimension);b.writeFloat(v.x);b.writeFloat(v.z);b.writeFloat(v.precipitation);b.writeBoolean(v.outdoors);
    },b->new WindView(Identifier.STREAM_CODEC.decode(b),b.readFloat(),b.readFloat(),b.readFloat(),b.readBoolean()));
}
