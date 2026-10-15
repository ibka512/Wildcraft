package dev.wildcraft.network;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
/** Transient presentation. Hurt overlay never locks inputs or owns damage. */
public record MeleeView(byte phase,byte weapon,boolean charged,int age,int duration,byte direction,float yaw,int serial,int hurt,float hurtYaw) {
    public MeleeView(byte phase,boolean axe,boolean charged,int age,int duration,byte direction,float yaw,int serial,int hurt,float hurtYaw){this(phase,(byte)(axe?1:0),charged,age,duration,direction,yaw,serial,hurt,hurtYaw);}
    public boolean axe(){return weapon==1;}
    public dev.wildcraft.combat.MeleeWeapon kind(){return dev.wildcraft.combat.MeleeWeapon.byId(weapon);}
    public static final byte IDLE=0,CHARGE=1,SWING=2,HOP=3,AIR_SWING=4,DROP_SWING=5;
    public static final MeleeView OFF=new MeleeView(IDLE,false,false,0,0,(byte)0,0,0,0,0);
    public static final StreamCodec<ByteBuf,MeleeView> CODEC=new StreamCodec<>() {
        @Override public MeleeView decode(ByteBuf b){return new MeleeView(b.readByte(),b.readByte(),b.readBoolean(),b.readInt(),b.readInt(),b.readByte(),b.readFloat(),b.readInt(),b.readByte(),b.readFloat());}
        @Override public void encode(ByteBuf b,MeleeView v){b.writeByte(v.phase).writeByte(v.weapon).writeBoolean(v.charged).writeInt(v.age).writeInt(v.duration).writeByte(v.direction).writeFloat(v.yaw).writeInt(v.serial).writeByte(v.hurt).writeFloat(v.hurtYaw);}
    };
}
