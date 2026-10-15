package dev.wildcraft.client.animation.rig;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Local adopted geometry and clip sampling. Reference equipment and diagnostic textures are excluded. */
public final class CombatRig {
    public record Mesh(String part,boolean outer,float[] positions,float[] normals,float[] uv,int[] joints,float[] weights,int[] indices){}
    public record Clip(float duration,boolean loop,float[][][] frames,float[][][] contacts,float[][] canopy,float[][][] feet,float distance,float[] pullTimes){}
    public record Data(int schema,String variant,String[] bones,int skinBones,float[][] bindWorld,float[][] inverseBind,Mesh[] meshes,Map<String,Clip> clips){}
    private static final Map<String,Data> CACHE=new HashMap<>();
    private static final Set<String> MISSING=new HashSet<>();
    private CombatRig() { }
    public static Data asset(String variant){if(MISSING.contains(variant))return null;return CACHE.computeIfAbsent(variant,n->{
        try(var stream=CombatRig.class.getResourceAsStream("/assets/wildcraft/animation/"+n+"-combat-rig.json")){
            if(stream==null){MISSING.add(n);dev.wildcraft.Wildcraft.LOGGER.info("Local adopted combat rig {} is absent; native player presentation remains active",n);return null;}
            var data=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Data.class);
            if(data.schema!=1||data.skinBones!=22||data.meshes.length!=12)throw new IllegalStateException("Unexpected combat rig schema");
            validate(data);return data;
        }catch(IOException e){throw new UncheckedIOException(e);}
    });}
    public static Matrix4f matrix(float[] a){return new Matrix4f().translationRotateScale(new Vector3f(a[0],a[1],a[2]),new Quaternionf(a[3],a[4],a[5],a[6]).normalize(),new Vector3f(a[7],a[8],a[9]));}
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
    /** Optional source-only inverse curve; native charge stays authoritative, legacy clips retain linear sampling. */
    public static float pullSample(Clip clip,float nativePull){
        float p=Math.clamp(nativePull,0,1);if(clip.pullTimes==null)return p;
        float at=p*(clip.pullTimes.length-1);int lo=(int)at,hi=Math.min(lo+1,clip.pullTimes.length-1);
        return clip.pullTimes[lo]+(clip.pullTimes[hi]-clip.pullTimes[lo])*(at-lo);
    }
    /** Snapshot skinning keeps deferred draw callbacks independent from later animation frames. */
    public static float[] deform(Mesh mesh,Matrix4f[] transforms,Matrix3f[] normals){
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
    public static boolean visible(Mesh m,AvatarRenderState s){
        if(!m.outer)return true;
        return switch(m.part.replace("outer_","")){case "head"->s.showHat;case "body"->s.showJacket;case "left_arm"->s.showLeftSleeve;case "right_arm"->s.showRightSleeve;case "left_leg"->s.showLeftPants;case "right_leg"->s.showRightPants;default->throw new IllegalStateException("Unknown skin layer "+m.part);};
    }
    private static Vector3f nativePoint(net.minecraft.client.renderer.item.ItemStackRenderState item,net.minecraft.world.entity.HumanoidArm arm,Vector3f point){
        var layers=((dev.wildcraft.mixin.client.FusionStateAccessor)item).wildcraft$layers();
        var layer=(dev.wildcraft.mixin.client.FusionLayerAccessor)layers[0];var pose=new PoseStack();
        layer.wildcraft$transform().apply(arm==net.minecraft.world.entity.HumanoidArm.LEFT,pose.last());pose.mulPose(layer.wildcraft$local());
        return pose.last().pose().transformPosition(point);
    }
    /** Current vanilla hand/item transform for feedback completion and bow-idle entry. */
    static Matrix4f nativeFeedbackReference(net.minecraft.client.model.player.PlayerModel model,AvatarRenderState state,net.minecraft.world.entity.HumanoidArm arm,boolean shield){return nativeFeedbackReference(model,state,arm,shield,false);}
    /** Authored third-person mace anchor is scoped to its transition; all old consumers retain their grip. */
    static Matrix4f nativeFeedbackReference(net.minecraft.client.model.player.PlayerModel model,AvatarRenderState state,net.minecraft.world.entity.HumanoidArm arm,boolean shield,boolean artMace){return nativeFeedbackReference(model,state,arm,shield,artMace,false);}
    static Matrix4f nativeFeedbackReference(net.minecraft.client.model.player.PlayerModel model,AvatarRenderState state,net.minecraft.world.entity.HumanoidArm arm,boolean shield,boolean artMace,boolean artCrossbow){
        var item=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?state.rightHandItemState:state.leftHandItemState;
        var pose=new PoseStack();model.translateToHand(state,arm,pose);
        pose.rotateDegrees(com.mojang.math.Axis.XP,-90);pose.rotateDegrees(com.mojang.math.Axis.YP,180);
        pose.translate(arm==net.minecraft.world.entity.HumanoidArm.LEFT?-.0625F:.0625F,.125F,-.625F);
        var actual=pose.last().pose();
        var held=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?state.rightHandItemStack:state.leftHandItemStack;boolean bow=held.is(net.minecraft.world.item.Items.BOW),crossbow=artCrossbow&&!shield&&held.is(net.minecraft.world.item.Items.CROSSBOW),mace=artMace&&!shield&&held.is(net.minecraft.world.item.Items.MACE),trident=!shield&&held.is(net.minecraft.world.item.Items.TRIDENT),spear=!shield&&dev.wildcraft.combat.MeleeWeapon.of(held)==dev.wildcraft.combat.MeleeWeapon.SPEAR;
        var raw=shield?new Vector3f(0,0,.09375F):trident?new Vector3f(0,1.1F,0):spear?new Vector3f(.70F,.30F,.5F):mace?new Vector3f(.08F,.08F,.5F):crossbow?new Vector3f(.90625F,.09375F,.5F):bow?new Vector3f(.21875F,.46875F,.5F):new Vector3f(.23F,.23F,.5F);
        var anchor=actual.transformPosition(nativePoint(item,arm,raw));
        var blade=crossbow?actual.transformPosition(nativePoint(item,arm,new Vector3f(.90625F,1.09375F,.5F))).sub(anchor).normalize():spear?actual.transformPosition(nativePoint(item,arm,new Vector3f(0,1,.5F))).sub(anchor).normalize():trident?actual.transformPosition(nativePoint(item,arm,new Vector3f(0,0,0))).sub(anchor).normalize():bow?actual.transformDirection(nativePoint(item,arm,new Vector3f(.90625F,.84375F,.5F)).sub(nativePoint(item,arm,new Vector3f(.15625F,.09375F,.5F)))).normalize():actual.transformPosition(nativePoint(item,arm,shield?new Vector3f(0,1,.09375F):new Vector3f(.9F,.9F,.5F))).sub(anchor).normalize();
        var normal=actual.transformPosition(nativePoint(item,arm,shield?new Vector3f(0,0,1.09375F):trident?new Vector3f(0,1.1F,1):spear?new Vector3f(.70F,.30F,1.5F):mace?new Vector3f(.08F,.08F,1.5F):crossbow?new Vector3f(.90625F,.09375F,1.5F):bow?new Vector3f(.21875F,.46875F,1.5F):new Vector3f(.23F,.23F,1.5F))).sub(anchor);
        normal.fma(-normal.dot(blade),blade).normalize();if(shield)blade.negate();if(trident)normal.negate();
        var axes=new Matrix3f().setColumn(0,new Vector3f(blade).cross(normal)).setColumn(1,blade).setColumn(2,normal);
        return new Matrix4f(CombatRigLayer.BASIS).mul(new Matrix4f(axes).setTranslation(anchor));
    }
    /** Preserve item scale and solve its actual hilt against the per-avatar sampled reference. */
    public static Matrix4f heldPose(CombatRigLayer.Frame frame,net.minecraft.client.renderer.item.ItemStackRenderState item,net.minecraft.world.entity.HumanoidArm arm,boolean shield){return heldPoseMatrix(frame,item,arm,shield);}
    private static Matrix4f heldPoseMatrix(CombatRigLayer.Frame frame,net.minecraft.client.renderer.item.ItemStackRenderState item,net.minecraft.world.entity.HumanoidArm arm,boolean shield){
        int reference=Arrays.asList(frame.data().bones()).indexOf((arm==net.minecraft.world.entity.HumanoidArm.RIGHT?"right_":"left_")+(shield?"shield_mount":"sword_reference"));
        var target=new Matrix4f(CombatRigLayer.BASIS).mul(frame.global()[reference]);
        // Native plate z=-2..-1px and handle z=-1..5px share a .09375 block bound center.
        // Adopted shield references are centered at this same point; keep original size and native UV orientation.
        boolean trident=(frame.clip().startsWith("trident_equipment_")||frame.clip().startsWith("trident_attack_"))&&!shield;
        boolean crossbow=frame.clip().startsWith("crossbow_equipment_")&&!shield;
        boolean mace=(frame.clip().startsWith("mace_equipment_")||frame.clip().startsWith("mace_attack_"))&&!shield;
        boolean spear=(frame.clip().startsWith("spear_equipment_")||frame.clip().startsWith("spear_attack_"))&&!shield;
        boolean bow=(frame.clip().startsWith("bow_idle_")||frame.clip().startsWith("bow_equipment_")||frame.clip().startsWith("bow_ground_")||frame.clip().startsWith("bow_air_"))&&!shield;
        // Locked 26.3 bow sprite: wood grip at pixel (3.5,8.5), endpoints (14.5,2.5)/(2.5,14.5).
        var rawAnchor=shield?new Vector3f(0,0,.09375F):trident?new Vector3f(0,1.1F,0):spear?new Vector3f(.70F,.30F,.5F):mace?new Vector3f(.08F,.08F,.5F):crossbow?new Vector3f(.90625F,.09375F,.5F):bow?new Vector3f(.21875F,.46875F,.5F):new Vector3f(.23F,.23F,.5F);
        var anchor=nativePoint(item,arm,rawAnchor);
        var blade=crossbow?nativePoint(item,arm,new Vector3f(.90625F,1.09375F,.5F)).sub(anchor).normalize():spear?nativePoint(item,arm,new Vector3f(0,1,.5F)).sub(anchor).normalize():trident?nativePoint(item,arm,new Vector3f(0,0,0)).sub(anchor).normalize():bow?nativePoint(item,arm,new Vector3f(.90625F,.84375F,.5F)).sub(nativePoint(item,arm,new Vector3f(.15625F,.09375F,.5F))).normalize():nativePoint(item,arm,shield?new Vector3f(0,1,.09375F):new Vector3f(.9F,.9F,.5F)).sub(anchor).normalize();
        var normal=nativePoint(item,arm,shield?new Vector3f(0,0,1.09375F):trident?new Vector3f(0,1.1F,1):spear?new Vector3f(.70F,.30F,1.5F):mace?new Vector3f(.08F,.08F,1.5F):crossbow?new Vector3f(.90625F,.09375F,1.5F):bow?new Vector3f(.21875F,.46875F,1.5F):new Vector3f(.23F,.23F,1.5F)).sub(anchor);normal.fma(-normal.dot(blade),blade).normalize();
        var desiredBlade=target.transformDirection(new Vector3f(0,shield?-1:1,0)).normalize();
        // Native RIGHT-hand bow display reverses the sprite's plane relative to LEFT use.
        // Only derived RIGHT ground/air slices change; existing left/idle consumers retain their axes.
        boolean reversedBow=(frame.clip().startsWith("bow_ground_")||frame.clip().startsWith("bow_air_"))&&arm==net.minecraft.world.entity.HumanoidArm.LEFT;
        var desiredNormal=target.transformDirection(new Vector3f(0,0,(trident||reversedBow)?-1:1));desiredNormal.fma(-desiredNormal.dot(desiredBlade),desiredBlade).normalize();
        var from=new Matrix3f().setColumn(0,new Vector3f(blade).cross(normal)).setColumn(1,blade).setColumn(2,normal);
        var to=new Matrix3f().setColumn(0,new Vector3f(desiredBlade).cross(desiredNormal)).setColumn(1,desiredBlade).setColumn(2,desiredNormal);
        return new Matrix4f().translation(target.getTranslation(new Vector3f())).mul(new Matrix4f(to.mul(from.transpose()))).translate(-anchor.x,-anchor.y,-anchor.z);
    }
    /** Native three-stage nock pixel, on the actual resolved bow model/display transform. */
    static Vector3f nativeBowString(CombatRigLayer.Frame frame,net.minecraft.client.renderer.item.ItemStackRenderState item,int ticks){
        return nativeBowString(frame,item,ticks,net.minecraft.world.entity.HumanoidArm.LEFT);
    }
    /** Actual bow arm selects the real native display transform; legacy left consumers retain their path. */
    static Vector3f nativeBowString(CombatRigLayer.Frame frame,net.minecraft.client.renderer.item.ItemStackRenderState item,int ticks,net.minecraft.world.entity.HumanoidArm arm){
        int stage=ticks>=18?2:ticks>=13?1:0;
        return new Matrix4f(BASIS_INVERSE).mul(heldPoseMatrix(frame,item,arm,false)).transformPosition(nativePoint(item,arm,new Vector3f((10.5F+stage)/16F,1-(9.5F+stage)/16F,.5F)));
    }
    private static final Matrix4f BASIS_INVERSE=new Matrix4f(CombatRigLayer.BASIS).invert();
    public static Matrix4f blend(Matrix4f a,Matrix4f b,float t){
        return new Matrix4f().translationRotateScale(a.getTranslation(new Vector3f()).lerp(b.getTranslation(new Vector3f()),t),a.getUnnormalizedRotation(new Quaternionf()).normalize().slerp(b.getUnnormalizedRotation(new Quaternionf()).normalize(),t),a.getScale(new Vector3f()).lerp(b.getScale(new Vector3f()),t));
    }
    public static void validate(Data d){
        if(d.bones==null||d.bones.length!=28||d.bindWorld.length!=28||d.inverseBind.length!=22||!d.clips.keySet().containsAll(Set.of("idle_R","idle_L","shield_R","shield_L","A_R","A_L","shield_A_R","shield_A_L")))throw new IllegalStateException("Invalid adopted rig dimensions");
        for(var mesh:d.meshes){
            int count=mesh.positions.length/3;
            if(mesh.positions.length%3!=0||mesh.normals.length!=count*3||mesh.uv.length!=count*2||mesh.joints.length!=count*4||mesh.weights.length!=count*4||mesh.indices.length%3!=0)throw new IllegalStateException("Invalid skin mesh dimensions");
            for(float[] values:new float[][]{mesh.positions,mesh.normals,mesh.uv,mesh.weights})for(float v:values)if(!Float.isFinite(v))throw new IllegalStateException("Nonfinite skin mesh");
            for(int i=0;i<count;i++){float sum=0;for(int j=0;j<4;j++){int at=i*4+j;if(mesh.joints[at]<0||mesh.joints[at]>=22||mesh.weights[at]<0)throw new IllegalStateException("Invalid skin weight");sum+=mesh.weights[at];}if(Math.abs(sum-1)>.0001)throw new IllegalStateException("Unnormalized skin weights");}
            for(int index:mesh.indices)if(index<0||index>=count)throw new IllegalStateException("Invalid skin index");
        }
        for(var c:d.clips.values()){
            if(!Float.isFinite(c.duration)||c.duration<=0||c.frames.length<2)throw new IllegalStateException("Invalid clip duration");
            if(c.pullTimes!=null){if(c.pullTimes.length<2)throw new IllegalStateException("Invalid bow pull curve");float previous=-1;for(float t:c.pullTimes){if(!Float.isFinite(t)||t<0||t>1.000001F||t<previous)throw new IllegalStateException("Non-monotone bow source curve");previous=t;}}
            for(var f:c.frames){if(f.length!=28)throw new IllegalStateException("Invalid animation palette");for(var bone:f){if(bone.length!=10)throw new IllegalStateException("Invalid bone transform");for(float v:bone)if(!Float.isFinite(v))throw new IllegalStateException("Nonfinite animation");}}
            if(c.contacts!=null){
                if(c.contacts.length!=c.frames.length)throw new IllegalStateException("Invalid climb contact samples");
                for(var f:c.contacts){if(f.length!=4)throw new IllegalStateException("Four authored climb contacts required");for(var point:f){if(point.length!=4)throw new IllegalStateException("Invalid climb contact");for(float v:point)if(!Float.isFinite(v))throw new IllegalStateException("Nonfinite climb contact");if(point[3]!=0&&point[3]!=1)throw new IllegalStateException("Invalid planted contact flag");}}
            }
            if(c.canopy!=null){if(c.canopy.length!=c.frames.length)throw new IllegalStateException("Invalid canopy samples");for(var p:c.canopy)if(p.length!=10||!matrix(p).isFinite())throw new IllegalStateException("Invalid source canopy matrix");}
            if(c.feet!=null){
                if(c.feet.length!=c.frames.length||!Float.isFinite(c.distance)||c.distance<=0)throw new IllegalStateException("Invalid walk contact samples/distance");
                for(var f:c.feet){if(f.length!=2)throw new IllegalStateException("Two walk feet required");for(var p:f){if(p.length!=4)throw new IllegalStateException("Invalid walk foot");for(float v:p)if(!Float.isFinite(v))throw new IllegalStateException("Nonfinite walk foot");if(p[3]!=0&&p[3]!=1)throw new IllegalStateException("Invalid planted walk flag");}}
            }
        }
        for(var b:d.bindWorld)if(b.length!=10||!matrix(b).isFinite())throw new IllegalStateException("Invalid bind pose");
        for(var b:d.inverseBind)if(b.length!=16||!new Matrix4f().set(b).isFinite())throw new IllegalStateException("Invalid inverse bind");
    }
}
