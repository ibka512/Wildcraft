package dev.wildcraft.art;

import dev.wildcraft.Wildcraft;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Only committed server events are broadcast; reconnects never reconstruct feedback from saved state. */
public record ArtFeedback(Identifier dimension,long sequence,UUID source,Vec3 position,int sound,int effect) implements CustomPacketPayload {
    public static final Type<ArtFeedback> TYPE=new Type<>(Wildcraft.id("art_feedback"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ArtFeedback> CODEC=StreamCodec.of((b,v)->{
        Identifier.STREAM_CODEC.encode(b,v.dimension);b.writeLong(v.sequence);b.writeUUID(v.source);b.writeDouble(v.position.x);b.writeDouble(v.position.y);b.writeDouble(v.position.z);b.writeVarInt(v.sound);b.writeVarInt(v.effect);
    },b->new ArtFeedback(Identifier.STREAM_CODEC.decode(b),b.readLong(),b.readUUID(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt(),b.readVarInt()));
    public ArtFeedback {if(!position.isFinite()||sound< -1||sound>11||effect< -1||effect>4)throw new IllegalArgumentException("Invalid art feedback");}
    public enum Cue {
        COOK_START("cooking.start",.26F,8,2),COOK_DONE("cooking.complete",.30F,10,3),CHARGE_START("charger.start",.22F,6,2),CHARGE_FULL("charger.full",.25F,8,3),
        INSTALL("mechanic.install",.30F,8,4),REMOVE("mechanic.remove",.27F,8,4),MACHINE_START("mechanic.start",.24F,10,2),MACHINE_STOP("mechanic.stop",.22F,10,2),
        FABRICATE_START("fabricator.start",.26F,10,2),FABRICATE_DONE("fabricator.complete",.30F,12,3),FUSE("fuse.join",.30F,8,4),SPLIT("fuse.split",.28F,8,4);
        public final SoundEvent event;public final float volume;public final int distance,priority;
        Cue(String id,float volume,int distance,int priority){var name=Wildcraft.id(id);event=Registry.register(BuiltInRegistries.SOUND_EVENT,name,SoundEvent.createVariableRangeEvent(name));this.volume=volume;this.distance=distance;this.priority=priority;}
        public SoundSource category(){return this==INSTALL||this==REMOVE||this==FUSE||this==SPLIT?SoundSource.PLAYERS:SoundSource.BLOCKS;}
    }
    private static final AtomicLong SEQUENCE=new AtomicLong();
    public static UUID blockSource(net.minecraft.core.BlockPos pos){return new UUID(0,pos.asLong());}
    public static void initialize(){Cue.values();PayloadTypeRegistry.clientboundPlay().register(TYPE,CODEC);}
    public static void send(ServerLevel level,UUID source,Vec3 point,Cue cue,int effect){
        var event=new ArtFeedback(level.dimension().identifier(),SEQUENCE.incrementAndGet(),source,point,cue==null?-1:cue.ordinal(),effect);
        int distance=effect>=0?16:cue.distance;
        for(var p:level.players())if(p.position().distanceToSqr(point)<=distance*distance&&ServerPlayNetworking.canSend(p,TYPE))ServerPlayNetworking.send(p,event);
    }
    @Override public Type<ArtFeedback> type(){return TYPE;}
}
