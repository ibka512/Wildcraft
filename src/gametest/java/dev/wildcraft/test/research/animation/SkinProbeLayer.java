package dev.wildcraft.test.research.animation;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.PlayerModelType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Test-only qualification of continuous adopted geometry using the game's real skin and GPU. */
public final class SkinProbeLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    public static volatile boolean enabled;
    public static volatile String clip="idle_R";
    public static volatile float fraction;
    public static int calls,vertices;
    public static int heldCalls;
    public static String skin;
    private record Mesh(String part,boolean outer,float[] positions,float[] normals,float[] uv,int[] joints,float[] weights,int[] indices){}
    private record Clip(float duration,boolean loop,float[][][] frames){}
    private record Data(int schema,String variant,String[] bones,int skinBones,float[][] bindWorld,float[][] inverseBind,Mesh[] meshes,Map<String,Clip> clips){}
    private static final Map<String,Data> CACHE=new HashMap<>();
    public SkinProbeLayer(RenderLayerParent<AvatarRenderState,PlayerModel> parent){super(parent);}
    public static void initialize(){LivingEntityRenderLayerRegistrationCallback.EVENT.register((type,renderer,helper,context)->{if(renderer instanceof AvatarRenderer<?> a)helper.register(new SkinProbeLayer(a));});}
    private static Data asset(String variant){return CACHE.computeIfAbsent(variant,n->{
        try(var stream=SkinProbeLayer.class.getResourceAsStream("/assets/wildcraft-test/animation/"+n+"-combat-rig.json")){
            if(stream==null)throw new IllegalStateException("Missing combat rig "+n);
            var data=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Data.class);
            if(data.schema!=1||data.skinBones!=22||data.meshes.length!=12)throw new IllegalStateException("Unexpected combat rig schema");
            return data;
        }catch(IOException e){throw new UncheckedIOException(e);}
    });}
    private static Matrix4f matrix(float[] a){return new Matrix4f().translationRotateScale(new Vector3f(a[0],a[1],a[2]),new Quaternionf(a[3],a[4],a[5],a[6]).normalize(),new Vector3f(a[7],a[8],a[9]));}
    public static Matrix4f[] sample(String variant,String name,float normalized){
        var data=asset(variant);var c=data.clips.get(name);if(c==null)throw new IllegalArgumentException("Missing clip "+name);
        float at=Math.clamp(normalized,0,1)*(c.frames.length-1);int lo=(int)at,hi=Math.min(lo+1,c.frames.length-1);float f=at-lo;
        var result=new Matrix4f[data.bones.length];
        for(int i=0;i<result.length;i++){
            var a=c.frames[lo][i];var b=c.frames[hi][i];var t=new Vector3f(a[0],a[1],a[2]).lerp(new Vector3f(b[0],b[1],b[2]),f);
            var q=new Quaternionf(a[3],a[4],a[5],a[6]).slerp(new Quaternionf(b[3],b[4],b[5],b[6]),f).normalize();
            var s=new Vector3f(a[7],a[8],a[9]).lerp(new Vector3f(b[7],b[8],b[9]),f);result[i]=new Matrix4f().translationRotateScale(t,q,s);
        }
        return result;
    }
    /** Snapshot skinning keeps deferred draw callbacks independent from later animation frames. */
    private static float[] deform(Mesh mesh,Matrix4f[] transforms,Matrix3f[] normals){
        int count=mesh.positions.length/3;float[] output=new float[count*8];var p=new Vector3f();var n=new Vector3f();var sumP=new Vector3f();var sumN=new Vector3f();
        for(int i=0;i<count;i++){
            sumP.zero();sumN.zero();
            for(int j=0;j<4;j++){
                float weight=mesh.weights[i*4+j];if(weight==0)continue;int bone=mesh.joints[i*4+j];
                transforms[bone].transformPosition(p.set(mesh.positions[i*3],mesh.positions[i*3+1],mesh.positions[i*3+2]));
                normals[bone].transform(n.set(mesh.normals[i*3],mesh.normals[i*3+1],mesh.normals[i*3+2]));sumP.fma(weight,p);sumN.fma(weight,n);
            }
            sumN.normalize();int out=i*8;output[out]=sumP.x;output[out+1]=sumP.y;output[out+2]=sumP.z;output[out+3]=sumN.x;output[out+4]=sumN.y;output[out+5]=sumN.z;output[out+6]=mesh.uv[i*2];output[out+7]=mesh.uv[i*2+1];
        }
        return output;
    }
    private static boolean visible(Mesh m,AvatarRenderState s){
        if(!m.outer)return true;
        return switch(m.part.replace("outer_","")){case "head"->s.showHat;case "body"->s.showJacket;case "left_arm"->s.showLeftSleeve;case "right_arm"->s.showRightSleeve;case "left_leg"->s.showLeftPants;case "right_leg"->s.showRightPants;default->throw new IllegalStateException("Unknown skin layer "+m.part);};
    }
    private static Vector3f nativePoint(net.minecraft.client.renderer.item.ItemStackRenderState item,net.minecraft.world.entity.HumanoidArm arm,Vector3f point){
        var layers=((dev.wildcraft.mixin.client.FusionStateAccessor)item).wildcraft$layers();
        var layer=(dev.wildcraft.mixin.client.FusionLayerAccessor)layers[0];var pose=new PoseStack();
        layer.wildcraft$transform().apply(arm==net.minecraft.world.entity.HumanoidArm.LEFT,pose.last());pose.mulPose(layer.wildcraft$local());
        return pose.last().pose().transformPosition(point);
    }
    /** Preserve the real item model scale; solve orientation and translation around its actual native hilt. */
    public static Matrix4f heldPose(AvatarRenderState state,net.minecraft.client.renderer.item.ItemStackRenderState item,net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.HumanoidArm arm){
        if(dev.wildcraft.combat.MeleeWeapon.of(stack)!=dev.wildcraft.combat.MeleeWeapon.SWORD)return null;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=asset(variant);
        int reference=Arrays.asList(data.bones).indexOf(arm==net.minecraft.world.entity.HumanoidArm.RIGHT?"right_sword_reference":"left_sword_reference");
        var source=sample(variant,clip,fraction)[reference];var target=new Matrix4f().translation(0,1.5F,0).scale(1,-1,-1).mul(source);
        var anchor=nativePoint(item,arm,new Vector3f(.23F,.23F,.5F));
        var blade=nativePoint(item,arm,new Vector3f(.9F,.9F,.5F)).sub(anchor).normalize();
        var normal=nativePoint(item,arm,new Vector3f(.23F,.23F,1.5F)).sub(anchor);normal.fma(-normal.dot(blade),blade).normalize();
        var desiredBlade=target.transformDirection(new Vector3f(0,1,0)).normalize();
        var desiredNormal=target.transformDirection(new Vector3f(0,0,1));desiredNormal.fma(-desiredNormal.dot(desiredBlade),desiredBlade).normalize();
        var from=new Matrix3f().setColumn(0,new Vector3f(blade).cross(normal)).setColumn(1,blade).setColumn(2,normal);
        var to=new Matrix3f().setColumn(0,new Vector3f(desiredBlade).cross(desiredNormal)).setColumn(1,desiredBlade).setColumn(2,desiredNormal);
        var rotated=to.mul(from.transpose());var matrix=new Matrix4f().translation(target.getTranslation(new Vector3f())).mul(new Matrix4f(rotated)).translate(-anchor.x,-anchor.y,-anchor.z);
        if(matrix.transformPosition(new Vector3f(anchor)).distance(target.getTranslation(new Vector3f()))>1e-5F||!matrix.isFinite())throw new AssertionError("Native sword grip projection failed");
        return matrix;
    }
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch){
        if(!enabled||state.isInvisible||state.isSpectator)return;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=asset(variant);var global=sample(variant,clip,fraction);
        var transforms=new Matrix4f[data.skinBones];var normals=new Matrix3f[data.skinBones];
        for(int i=0;i<data.skinBones;i++){transforms[i]=new Matrix4f(global[i]).mul(new Matrix4f().set(data.inverseBind[i]));normals[i]=transforms[i].normal(new Matrix3f());}
        skin=state.skin.body().texturePath().toString();calls++;
        var type=RenderTypes.entityCutout(state.skin.body().texturePath());int overlay=LivingEntityRenderer.getOverlayCoords(state,0);
        poses.pushPose();poses.translate(0,1.5,0);poses.scale(1,-1,-1);
        for(var mesh:data.meshes){
            if(!visible(mesh,state))continue;float[] transformed=deform(mesh,transforms,normals);int[] indices=mesh.indices;vertices+=transformed.length/8;
            collector.submitCustomGeometry(poses,type,(pose,buffer)->{
                for(int triangle=0;triangle<indices.length;triangle+=3)for(int corner:new int[]{0,1,2,2}){
                    int index=indices[triangle+corner]*8;buffer.addVertex(pose,transformed[index],transformed[index+1],transformed[index+2]).setColor(-1).setUv(transformed[index+6],transformed[index+7]).setOverlay(overlay).setLight(light).setNormal(pose,transformed[index+3],transformed[index+4],transformed[index+5]);
                }
            });
        }
        poses.popPose();
    }
}
