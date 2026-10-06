package dev.wildcraft.client.art;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.*;
import dev.wildcraft.Wildcraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable approved meshes, with only the existing per-node state driving motion. */
public final class ArtMesh {
    public record Part(String name,String group,float[][] quads,float[][] extendedQuads){}
    public record Window(int index,float[] from,float[] to){}
    private record Export(String texture,float[] pivot,Part[] parts,Window[] windows){}
    private static final Map<String,ArtMesh> MESHES=new java.util.concurrent.ConcurrentHashMap<>();
    private static final RenderType GHOST=RenderType.create("wildcraft_install_preview",RenderSetup.builder(
        RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET).withLocation(Wildcraft.id("pipeline/install_preview"))
            .withShaderDefine("PER_FACE_LIGHTING").withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER1).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false)).withCull(false).build())
        .withTexture("Sampler0",Wildcraft.id("textures/entity/machine.png")).useLightmap().useOverlay().sortOnUpload().createRenderSetup());
    private static final Map<float[],Vector3f> NORMALS=new java.util.concurrent.ConcurrentHashMap<>();
    private final Export data;
    private final Vector3f min=new Vector3f(Float.POSITIVE_INFINITY),max=new Vector3f(Float.NEGATIVE_INFINITY);
    private ArtMesh(Export data){this.data=data;for(var part:data.parts){for(var q:part.quads)NORMALS.put(q,normal(q));if(part.extendedQuads!=null)for(var q:part.extendedQuads)NORMALS.put(q,normal(q));}for(var part:data.parts)for(var q:part.quads)for(int i=0;i<4;i++){var v=new Vector3f(q[i*5],q[i*5+1],q[i*5+2]);min.min(v);max.max(v);}}
    public void extents(java.util.function.Consumer<org.joml.Vector3fc> output,float offset){output.accept(new Vector3f(min).add(offset,offset,offset));output.accept(new Vector3f(max).add(offset,offset,offset));}
    public static ArtMesh get(String name){return MESHES.computeIfAbsent(name,n->{
        try(var in=ArtMesh.class.getResourceAsStream("/assets/wildcraft/geometry/"+n+".json")){
            if(in==null)throw new IllegalStateException("Missing adopted mesh "+n);
            return new ArtMesh(new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Export.class));
        }catch(IOException ex){throw new UncheckedIOException(ex);}
    });}
    public int quadCount(){return Arrays.stream(data.parts).mapToInt(p->p.quads.length).sum();}
    public void submit(PoseStack poses,SubmitNodeCollector collector,int light,boolean ghost,float angle,boolean extended,boolean active,int energy){
        for(var part:data.parts){
            poses.pushPose();
            if(angle!=0&&(part.group.equals("rotor")||part.group.equals("rotating"))){poses.translate(data.pivot[0],data.pivot[1],data.pivot[2]);poses.rotate(com.mojang.math.Axis.ZP,angle);poses.translate(-data.pivot[0],-data.pivot[1],-data.pivot[2]);}
            int color=ghost?0x885DD7B7:part.group.equals("indicator")?(active?0xFF8DD5B7:0xFF607775):0xFFFFFFFF;
            var texture=Wildcraft.id("textures/entity/"+(part.group.equals("indicator")?"machine":data.texture)+".png");
            var type=ghost?GHOST:RenderTypes.entityCutout(texture);
            var quads=extended&&part.extendedQuads!=null?part.extendedQuads:part.quads;
            geometry(poses,collector,type,light,color,quads,part.group.equals("indicator"));poses.popPose();
        }
        if(data.windows!=null)for(var w:data.windows){
            float fraction=(float)Math.clamp(energy/1000.0*3-w.index,0,1);if(fraction<=0)continue;
            float x0=w.from[0],y0=w.from[1],z=w.from[2],x1=w.to[0],y1=y0+(w.to[1]-y0)*fraction;
            float v0=.75f-.25f*fraction;
            float[][] q={{x0,y0,z,.375f,.75f,x1,y0,z,.75f,.75f,x1,y1,z,.75f,v0,x0,y1,z,.375f,v0}};
            geometry(poses,collector,ghost?GHOST:RenderTypes.entityCutout(Wildcraft.id("textures/entity/battery.png")),light,ghost?0x885DD7B7:-1,q,false);
        }
    }
    private static Vector3f normal(float[] q){var n=new Vector3f(q[5]-q[0],q[6]-q[1],q[7]-q[2]).cross(new Vector3f(q[10]-q[0],q[11]-q[1],q[12]-q[2]));return n.lengthSquared()<1e-12F?n:n.normalize();}
    private static void geometry(PoseStack poses,SubmitNodeCollector collector,RenderType type,int light,int color,float[][] quads,boolean white){
        collector.submitCustomGeometry(poses,type,(pose,buffer)->{
            for(float[] q:quads){
                var normal=NORMALS.get(q);if(normal==null)normal=normal(q);
                if(normal.lengthSquared()<1e-12f)continue;
                for(int v=0;v<4;v++){int i=v*5;buffer.addVertex(pose,q[i],q[i+1],q[i+2]).setColor(color).setUv(white?0:q[i+3],white?0:q[i+4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,normal.x,normal.y,normal.z);}
            }
        });
    }
}
