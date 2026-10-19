package dev.wildcraft.client.animation.rig;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.MeleePoses;
import dev.wildcraft.combat.MeleeWeapon;
import dev.wildcraft.mixin.client.FusionLayerAccessor;
import dev.wildcraft.mixin.client.FusionStateAccessor;
import dev.wildcraft.network.MeleeView;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Items;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Optional adopted camera-space skin arms. This class never owns inputs or combat transactions. */
public final class FirstPersonSword {
    public record Sample(float[][] bones,float[][] grips,float[][] swords,float[][] shields) {}
    public record Clip(float duration,Sample[] frames) {}
    public record Data(int schema,String variant,String[] bones,float[][] pivots,float[] eye,CombatRig.Mesh[] meshes,Map<String,Clip> clips) {}
    public record Frame(Data data,Sample sample,String clip,float sourceTime) {}
    private static final Map<String,Data> CACHE=new HashMap<>();
    private static final Set<String> ABSENT=new HashSet<>();
    private FirstPersonSword() {}

    public static Data asset(String variant) {
        if(ABSENT.contains(variant))return null;
        return CACHE.computeIfAbsent(variant,n->{
            try(var in=FirstPersonSword.class.getResourceAsStream("/assets/wildcraft/animation/"+n+"-first-person-sword.json")){
                if(in==null){ABSENT.add(n);return null;}
                var data=new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Data.class);
                validate(data);return data;
            }catch(IOException e){throw new UncheckedIOException(e);}
        });
    }
    private static void validate(Data d) {
        if(d.schema!=1||d.bones.length!=6||d.pivots.length!=6||d.eye.length!=3||d.meshes.length!=4||!d.clips.keySet().equals(Set.of("A_R","A_L","shield_A_R","shield_A_L")))throw new IllegalStateException("Invalid independent first-person sword schema");
        for(var c:d.clips.values()){
            if(c.duration<2.05F||c.frames.length<2)throw new IllegalStateException("Invalid first-person source timing");
            for(var f:c.frames){
                if(f.bones.length!=6||f.grips.length!=2||f.swords.length!=2||f.shields.length!=2)throw new IllegalStateException("Invalid first-person palette");
                for(var a:f.bones)if(a.length!=7||!matrix(a).isFinite())throw new IllegalStateException("Invalid first-person bone");
            }
        }
        for(var m:d.meshes){
            int count=m.positions().length/3;
            if(count==0||m.normals().length!=count*3||m.uv().length!=count*2||m.joints().length!=count*4||m.weights().length!=count*4)throw new IllegalStateException("Invalid first-person skin dimensions");
            for(int i=0;i<count;i++){float sum=0;for(int k=0;k<4;k++){int j=i*4+k;if(m.joints()[j]<0||m.joints()[j]>=6||m.weights()[j]<0)throw new IllegalStateException("Invalid independent skin weight");sum+=m.weights()[j];}if(Math.abs(sum-1)>.0001F)throw new IllegalStateException("Non-normalized independent skin weight");}
            for(int i:m.indices())if(i<0||i>=count)throw new IllegalStateException("Invalid independent skin index");
        }
    }
    public static Frame frame(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h) {
        var client=Minecraft.getInstance();var p=client.player;
        if(a==null||p==null||a.isInvisible||a.isSpectator||a.isUsingItem||a.isAutoSpinAttack||h.isScoping||h.useItemRemainingTicks>0||!p.onGround()||p.isUsingItem()||client.gui.screen()!=null&&!client.gui.screen().isPauseScreen())return null;
        var v=a.getData(MeleePoses.LAYER);
        if(v==null||v.kind()!=MeleeWeapon.SWORD||v.phase()!=MeleeView.SWING||v.charged()||v.hurt()!=0||MeleeWeapon.of(h.mainHandItem)!=MeleeWeapon.SWORD||MeleeWeapon.of(p.getMainHandItem())!=MeleeWeapon.SWORD||a.mainArm!=p.getMainArm()||h.mainHandRenderState.isEmpty()||!h.offHandItem.isEmpty()&&!h.offHandItem.is(Items.SHIELD))return null;
        // Resolved native render-state inventory must have caught up before borrowing either arm.
        if(!net.minecraft.world.item.ItemStack.isSameItem(p.getOffhandItem(),h.offHandItem)||!net.minecraft.world.item.ItemStack.isSameItem(p.getMainHandItem(),h.mainHandItem))return null;
        if(dev.wildcraft.client.animation.EquipmentHands.holster(a,h,InteractionHand.MAIN_HAND)||dev.wildcraft.client.animation.EquipmentHands.stale(a,h,InteractionHand.MAIN_HAND,h.mainHandItem))return null;
        var d=asset(a.skin.model()==PlayerModelType.SLIM?"slim":"standard");if(d==null)return null;
        String name=(h.offHandItem.is(Items.SHIELD)?"shield_":"")+"A_"+(a.mainArm==HumanoidArm.RIGHT?"R":"L");
        float t=Math.clamp((v.age()+a.getDataOrDefault(MeleePoses.PARTIAL,0F))/Math.max(1,v.duration()),0,1);
        float time=.65F+1.4F*t;return new Frame(d,sample(d.clips.get(name),time),name,time);
    }
    public static Sample sample(Clip c,float seconds) {
        float t=Math.clamp(seconds/c.duration,0,1)*(c.frames.length-1);int lo=(int)t,hi=Math.min(lo+1,c.frames.length-1);float f=t-lo;
        var a=c.frames[lo];var b=c.frames[hi];float[][] bones=new float[6][];
        for(int i=0;i<6;i++){
            var p=vec(a.bones[i]).lerp(vec(b.bones[i]),f);var q=quat(a.bones[i],3).slerp(quat(b.bones[i],3),f).normalize();
            bones[i]=new float[]{p.x,p.y,p.z,q.x,q.y,q.z,q.w};
        }
        float[][] grips=new float[2][],swords=new float[2][],shields=new float[2][];
        for(int i=0;i<2;i++){
            var p=vec(a.grips[i]).lerp(vec(b.grips[i]),f);grips[i]=new float[]{p.x,p.y,p.z};
            swords[i]=blend(a.swords[i],b.swords[i],f);shields[i]=blend(a.shields[i],b.shields[i],f);
        }
        return new Sample(bones,grips,swords,shields);
    }
    private static float[] blend(float[] a,float[] b,float t){var q=quat(a,0).slerp(quat(b,0),t).normalize();return new float[]{q.x,q.y,q.z,q.w};}
    private static Vector3f vec(float[] a){return new Vector3f(a[0],a[1],a[2]);}
    private static Quaternionf quat(float[] a,int i){return new Quaternionf(a[i],a[i+1],a[i+2],a[i+3]).normalize();}
    private static Matrix4f matrix(float[] a){return new Matrix4f().translationRotateScale(vec(a),quat(a,3),new Vector3f(1));}
    /** Source -Y forward, Z up, negative X on screen right. Positive rigid rotation, no UV reflection. */
    public static Matrix4f camera(Data d){return new Matrix4f().setColumn(0,new Vector4f(-1,0,0,0)).setColumn(1,new Vector4f(0,0,1,0)).setColumn(2,new Vector4f(0,1,0,0)).translate(-d.eye[0],-d.eye[1],-d.eye[2]);}
    public static Vector3f grip(Frame f,HumanoidArm arm){return camera(f.data).transformPosition(vec(f.sample.grips[arm==HumanoidArm.RIGHT?0:1]));}
    private static Vector3f nativePoint(ItemStackRenderState item,HumanoidArm arm,Vector3f p){
        var layers=((FusionStateAccessor)item).wildcraft$layers();var layer=(FusionLayerAccessor)layers[0];var pose=new PoseStack();
        layer.wildcraft$transform().apply(arm==HumanoidArm.LEFT,pose.last());pose.mulPose(layer.wildcraft$local());return pose.last().pose().transformPosition(p);
    }
    /** Solve the native model's actual transformed hilt/plate, retaining its original scale and all Fuse layers. */
    public static Matrix4f heldPose(Frame f,ItemStackRenderState item,HumanoidArm arm,boolean shield){
        int side=arm==HumanoidArm.RIGHT?0:1;var q=quat(f.sample.bones[side*3+1],3).mul(quat(shield?f.sample.shields[side]:f.sample.swords[side],0));
        var target=camera(f.data).mul(new Matrix4f().translationRotateScale(vec(f.sample.grips[side]),q,new Vector3f(1)));
        var anchor=nativePoint(item,arm,shield?new Vector3f(0,0,.09375F):new Vector3f(.23F,.23F,.5F));
        var blade=nativePoint(item,arm,shield?new Vector3f(0,1,.09375F):new Vector3f(.9F,.9F,.5F)).sub(anchor).normalize();
        var normal=nativePoint(item,arm,shield?new Vector3f(0,0,1.09375F):new Vector3f(.23F,.23F,1.5F)).sub(anchor);normal.fma(-normal.dot(blade),blade).normalize();
        var desiredBlade=target.transformDirection(new Vector3f(0,0,1)).normalize();var desiredNormal=target.transformDirection(new Vector3f(0,-1,0)).normalize();
        var from=new Matrix3f().setColumn(0,new Vector3f(blade).cross(normal)).setColumn(1,blade).setColumn(2,normal);
        var to=new Matrix3f().setColumn(0,new Vector3f(desiredBlade).cross(desiredNormal)).setColumn(1,desiredBlade).setColumn(2,desiredNormal);
        return new Matrix4f().translation(target.getTranslation(new Vector3f())).mul(new Matrix4f(to.mul(from.transpose()))).translate(-anchor.x,-anchor.y,-anchor.z);
    }
    /** Deferred geometry snapshots are immutable; subsequent hand submissions cannot change them. */
    public static void submitArms(Frame f,AvatarRenderState a,PoseStack poses,SubmitNodeCollector collector,int light){
        var matrices=new Matrix4f[6];var normals=new Matrix3f[6];
        for(int i=0;i<6;i++){var p=f.data.pivots[i];matrices[i]=matrix(f.sample.bones[i]).translate(-p[0],-p[1],-p[2]);normals[i]=matrices[i].normal(new Matrix3f());}
        poses.pushPose();try{
            poses.mulPose(camera(f.data));var type=RenderTypes.entityCutout(a.skin.body().texturePath());
            for(var mesh:f.data.meshes){
                if(!CombatRig.visible(mesh,a))continue;var vertices=CombatRig.deform(mesh,matrices,normals);var indices=mesh.indices();
                collector.submitCustomGeometry(poses,type,(pose,buffer)->{
                    for(int triangle=0;triangle<indices.length;triangle+=3)for(int corner=0;corner<4;corner++){
                        int i=indices[triangle+Math.min(corner,2)]*8;buffer.addVertex(pose,vertices[i],vertices[i+1],vertices[i+2]).setColor(-1).setUv(vertices[i+6],vertices[i+7]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,vertices[i+3],vertices[i+4],vertices[i+5]);
                    }
                });
            }
        }finally{poses.popPose();}
    }
    public static boolean submit(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,InteractionHand hand,PoseStack poses,SubmitNodeCollector collector,int light){
        var f=frame(a,h);if(f==null)return false;
        if(hand==InteractionHand.MAIN_HAND)submitArms(f,a,poses,collector,light);
        var item=hand==InteractionHand.MAIN_HAND?h.mainHandRenderState:h.offHandRenderState;
        if(!item.isEmpty()){
            var arm=hand==InteractionHand.MAIN_HAND?a.mainArm:a.mainArm.getOpposite();poses.pushPose();
            try{poses.mulPose(heldPose(f,item,arm,hand==InteractionHand.OFF_HAND));item.submit(poses,collector,light,OverlayTexture.NO_OVERLAY,a.outlineColor);}finally{poses.popPose();}
        }
        return true;
    }
}
