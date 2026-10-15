package dev.wildcraft.network;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
/** Transient authoritative enemy motion; never decides damage from a rendered frame. */
public record ThreatView(byte phase,int age,int duration){
    public static final byte IDLE=0,WINDUP=1,SWING=2,STAGGER=3;
    public static final ThreatView OFF=new ThreatView(IDLE,0,0);
    public static final StreamCodec<ByteBuf,ThreatView> CODEC=new StreamCodec<>(){
        public ThreatView decode(ByteBuf b){return new ThreatView(b.readByte(),b.readInt(),b.readInt());}
        public void encode(ByteBuf b,ThreatView v){b.writeByte(v.phase);b.writeInt(v.age);b.writeInt(v.duration);}
    };
}
