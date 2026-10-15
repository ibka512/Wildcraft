package dev.wildcraft.network;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
/** Transient opportunity / active episode, measured in effective real seconds. */
public record ReactionView(byte phase,int target,double remaining,int serial){
    public static final byte IDLE=0,READY=1,ACTIVE=2;
    public static final ReactionView OFF=new ReactionView(IDLE,-1,0,0);
    public static final StreamCodec<ByteBuf,ReactionView> CODEC=new StreamCodec<>(){
        public ReactionView decode(ByteBuf b){return new ReactionView(b.readByte(),b.readInt(),b.readDouble(),b.readInt());}
        public void encode(ByteBuf b,ReactionView v){b.writeByte(v.phase).writeInt(v.target).writeDouble(v.remaining).writeInt(v.serial);}
    };
}
