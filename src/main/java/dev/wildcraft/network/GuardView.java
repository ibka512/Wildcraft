package dev.wildcraft.network;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
public record GuardView(int remaining,int serial){
    public static final GuardView OFF=new GuardView(0,0);
    public static final StreamCodec<ByteBuf,GuardView> CODEC=new StreamCodec<>(){
        public GuardView decode(ByteBuf b){return new GuardView(b.readInt(),b.readInt());}
        public void encode(ByteBuf b,GuardView v){b.writeInt(v.remaining);b.writeInt(v.serial);}
    };
}
