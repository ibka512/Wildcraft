package dev.wildcraft.fuse;

import com.mojang.serialization.*;
import com.mojang.datafixers.util.Pair;
import com.google.common.hash.HashCode;
import net.minecraft.util.HashOps;
import java.io.*;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/** Persistent raw material: unknown registry entries survive without becoming substitute items. */
public final class FusionData {
    public static final int MAX_BYTES=8192, MAX_DEPTH=16, MAX_USES=32;
    public static final Codec<FusionData> CODEC=new Codec<>(){
        @Override public <T> DataResult<Pair<FusionData,T>> decode(DynamicOps<T> ops,T input){return CompoundTag.CODEC.decode(ops,input).flatMap(p -> read(p.getFirst()).map(d -> Pair.of(d,p.getSecond())));}
        @SuppressWarnings("unchecked") @Override public <T> DataResult<T> encode(FusionData d,DynamicOps<T> ops,T prefix){
            // Native 26.3 container prediction hashes persistent codecs. Preserve its CRC without exposing raw content.
            if(ops.empty() instanceof HashCode)return ops.mergeToPrimitive(prefix,(T)HashCode.fromInt(d.hash));
            if(d.projection)return DataResult.error(() -> "Client fusion projection cannot be persisted");
            return CompoundTag.CODEC.encode(d.tag(),ops,prefix);
        }
    };
    private final CompoundTag material;
    private final int remaining,hash;
    private final boolean projection;
    private FusionData(CompoundTag material,int remaining){this.material=material.copy();this.remaining=remaining;this.projection=false;this.hash=CompoundTag.CODEC.encodeStart(HashOps.CRC32C_INSTANCE,tag()).getOrThrow().asInt();}
    private FusionData(int hash){this.material=new CompoundTag();this.remaining=0;this.projection=true;this.hash=hash;}
    public static FusionData projection(int hash){return new FusionData(hash);}
    public boolean isProjection(){return projection;}
    public int networkHash(){return hash;}
    public int remaining(){return remaining;}
    public CompoundTag rawMaterial(){return material.copy();}
    public CompoundTag tag(){var t=new CompoundTag();t.putInt("schema",1);t.putInt("remaining",remaining);t.put("material",material.copy());return t;}
    public FusionData used(){if(remaining<=1)throw new IllegalStateException("Exhausted material must be removed");return new FusionData(material,remaining-1);}
    public Optional<ItemStack> material(HolderLookup.Provider registries){
        if(projection)return Optional.empty();
        var decoded=ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE,registries),material);
        return decoded.error().isEmpty()?decoded.result().filter(s -> !s.isEmpty()&&s.getCount()==1):Optional.empty();
    }
    public static DataResult<FusionData> capture(ItemStack stack,HolderLookup.Provider registries){
        if(stack.isEmpty())return DataResult.error(() -> "Empty fusion material");
        int wear=stack.getOrDefault(FusionContent.WEAR,MAX_USES);
        if(wear<1||wear>MAX_USES)return DataResult.error(() -> "Invalid fusion material wear");
        var encoded=ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE,registries),stack.copyWithCount(1));
        if(encoded.error().isPresent()||encoded.result().isEmpty()||!(encoded.result().get() instanceof CompoundTag raw))return DataResult.error(() -> "Material cannot be encoded losslessly");
        var t=new CompoundTag();t.putInt("schema",1);t.putInt("remaining",wear);t.put("material",raw);return read(t);
    }
    private static DataResult<FusionData> read(CompoundTag t){
        if(t.getIntOr("schema",0)!=1||t.getIntOr("remaining",0)<1||t.getIntOr("remaining",0)>MAX_USES||!(t.get("material") instanceof CompoundTag raw))return DataResult.error(() -> "Invalid fusion schema or material");
        if(!bounded(t))return DataResult.error(() -> "Fusion material exceeds byte/depth budget or contains nested fusion");
        if(raw.getStringOr("id","").isEmpty()||raw.getIntOr("count",0)!=1)return DataResult.error(() -> "Fusion requires one identified material");
        return DataResult.success(new FusionData(raw,t.getIntOr("remaining",0)));
    }
    private record Node(Tag tag,int depth){}
    private static boolean bounded(CompoundTag tag){
        var queue=new ArrayDeque<Node>();queue.add(new Node(tag,0));int visited=0;
        while(!queue.isEmpty()){
            var n=queue.removeFirst();if(n.depth()>MAX_DEPTH||++visited>MAX_BYTES)return false;
            if(n.tag() instanceof CompoundTag c){for(String key:c.keySet()){if(key.equals("wildcraft:fusion"))return false;queue.add(new Node(c.get(key),n.depth()+1));}}
            else if(n.tag() instanceof ListTag list)for(Tag child:list)queue.add(new Node(child,n.depth()+1));
        }
        try{NbtIo.write(tag,new DataOutputStream(new OutputStream(){int size;public void write(int b)throws IOException{if(++size>MAX_BYTES)throw new IOException("Fusion budget");}public void write(byte[] b,int off,int len)throws IOException{size+=len;if(size>MAX_BYTES)throw new IOException("Fusion budget");}}));return true;}catch(IOException|RuntimeException e){return false;}
    }
    @Override public boolean equals(Object o){return o instanceof FusionData d&&projection==d.projection&&hash==d.hash&&remaining==d.remaining&&material.equals(d.material);}
    @Override public int hashCode(){return hash;}
}
