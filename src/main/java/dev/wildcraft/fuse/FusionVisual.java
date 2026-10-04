package dev.wildcraft.fuse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.resources.Identifier;

/** Bounded public projection: no names, containers, custom data or nested items. */
public record FusionVisual(Identifier material,int kind,int uses,boolean glint){
    public static final Codec<FusionVisual> CODEC=RecordCodecBuilder.create(i -> i.group(Identifier.CODEC.fieldOf("material").forGetter(FusionVisual::material),Codec.intRange(0,6).fieldOf("kind").forGetter(FusionVisual::kind),Codec.intRange(1,32).fieldOf("uses").forGetter(FusionVisual::uses),Codec.BOOL.fieldOf("glint").forGetter(FusionVisual::glint)).apply(i,FusionVisual::new));
    public static final StreamCodec<ByteBuf,FusionVisual> STREAM_CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,FusionVisual::material,ByteBufCodecs.VAR_INT,FusionVisual::kind,ByteBufCodecs.VAR_INT,FusionVisual::uses,ByteBufCodecs.BOOL,FusionVisual::glint,FusionVisual::new);
    public FusionVisual{if(material.toString().length()>256||kind<0||kind>6||uses<1||uses>32)throw new IllegalArgumentException("Invalid fusion view");}
}
