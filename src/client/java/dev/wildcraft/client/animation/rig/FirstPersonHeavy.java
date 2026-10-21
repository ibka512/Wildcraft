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
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Locally adopted heavy sword/axe skin only; native combat and item submissions stay authoritative. */
public final class FirstPersonHeavy {
    public record Resource(FirstPersonSword.Data data,Map<String,float[]> supportWeights,float[] placement){}
    public record Frame(FirstPersonSword.Frame pose,MeleeWeapon kind,float supportWeight){}
    private static final Map<String,Optional<Resource>> CACHE=new HashMap<>();
    private FirstPersonHeavy(){}
    public static Resource asset(String variant,MeleeWeapon kind){
        String key=variant+"-first-person-"+(kind==MeleeWeapon.AXE?"axe":"heavy-sword");
        return CACHE.computeIfAbsent(key,n->{
            try(var in=FirstPersonHeavy.class.getResourceAsStream("/assets/wildcraft/animation/"+n+".json")){
                if(in==null)return Optional.empty();
                var r=new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Resource.class);var d=r.data;
                if(d.schema()!=1||d.bones().length!=6||d.pivots().length!=6||d.meshes().length!=4||d.clips().size()!=(kind==MeleeWeapon.AXE?16:12))throw new IllegalStateException("Invalid independent heavy skin resource");
                for(var c:d.clips().entrySet()){
                    if(c.getValue().duration()<=0||c.getValue().frames().length<2||r.supportWeights.get(c.getKey()).length!=c.getValue().frames().length)throw new IllegalStateException("Invalid heavy source timeline");
                    for(var f:c.getValue().frames())for(var bone:f.bones()){if(bone.length!=7)throw new IllegalStateException("Invalid heavy palette");for(float value:bone)if(!Float.isFinite(value))throw new IllegalStateException("Nonfinite heavy pose");}
                }
                // A common local display root, derived from both palms at FOV60; camera and model scale are untouched.
                var eye=d.eye().clone();eye[1]+=r.placement[0];eye[2]-=r.placement[1];
                var placed=new FirstPersonSword.Data(d.schema(),d.variant(),d.bones(),d.pivots(),eye,d.meshes(),d.clips());
                return Optional.of(new Resource(placed,r.supportWeights,r.placement));
            }catch(IOException e){throw new UncheckedIOException(e);}
        }).orElse(null);
    }
    public static Frame frame(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h){
        var c=Minecraft.getInstance();var p=c.player;
        if(a==null||p==null||a.isInvisible||a.isSpectator||a.isUsingItem||a.isAutoSpinAttack||h.isScoping||h.useItemRemainingTicks>0||!p.onGround()||p.isUsingItem()||c.gui.screen()!=null&&!c.gui.screen().isPauseScreen())return null;
        var v=a.getData(MeleePoses.LAYER);if(v==null||v.hurt()!=0||v.kind()!=MeleeWeapon.SWORD&&v.kind()!=MeleeWeapon.AXE||v.phase()!=MeleeView.CHARGE&&v.phase()!=MeleeView.SWING||v.kind()==MeleeWeapon.SWORD&&v.phase()==MeleeView.SWING&&!v.charged())return null;
        if(MeleeWeapon.of(h.mainHandItem)!=v.kind()||MeleeWeapon.of(p.getMainHandItem())!=v.kind()||a.mainArm!=p.getMainArm()||h.mainHandRenderState.isEmpty()||!h.offHandItem.isEmpty()&&!h.offHandItem.is(Items.SHIELD))return null;
        if(!net.minecraft.world.item.ItemStack.isSameItem(p.getOffhandItem(),h.offHandItem)||!net.minecraft.world.item.ItemStack.isSameItem(p.getMainHandItem(),h.mainHandItem)||dev.wildcraft.client.animation.EquipmentHands.holster(a,h,InteractionHand.MAIN_HAND)||dev.wildcraft.client.animation.EquipmentHands.stale(a,h,InteractionHand.MAIN_HAND,h.mainHandItem))return null;
        var r=asset(a.skin.model()==PlayerModelType.SLIM?"slim":"standard",v.kind());if(r==null)return null;
        float age=v.age()+a.getDataOrDefault(MeleePoses.PARTIAL,0F);String phase=v.phase()==MeleeView.CHARGE?(age>=v.duration()?"hold":"charge"):v.charged()?"release":"light";
        String name=(h.offHandItem.is(Items.SHIELD)?"shield_":"")+phase+"_"+(a.mainArm==HumanoidArm.RIGHT?"R":"L");var clip=r.data.clips().get(name);float time=phase.equals("hold")?Math.clamp((age-v.duration())/20F,0,clip.duration()):clip.duration()*Math.clamp(age/Math.max(1,v.duration()),0,1);
        var pose=new FirstPersonSword.Frame(r.data,FirstPersonSword.sample(clip,time),name,time);float[] weights=r.supportWeights.get(name);float at=Math.clamp(time/clip.duration(),0,1)*(weights.length-1);int lo=(int)at;float weight=weights[lo]+(weights[Math.min(lo+1,weights.length-1)]-weights[lo])*(at-lo);
        if(v.kind()==MeleeWeapon.AXE&&h.offHandItem.isEmpty()&&weight>0){pose=support(pose,h.mainHandRenderState,a.mainArm,weight);if(pose==null)return null;}
        return new Frame(pose,v.kind(),weight);
    }
    private static Vector3f vec(float[] a){return new Vector3f(a[0],a[1],a[2]);}
    private static Quaternionf quat(float[] a){return new Quaternionf(a[3],a[4],a[5],a[6]);}
    private static float[] bone(Vector3f p,Quaternionf q){return new float[]{p.x,p.y,p.z,q.x,q.y,q.z,q.w};}
    /** Actual native handle point; the original item scale, mesh and all Fuse layers remain intact. */
    public static Vector3f nativePoint(ItemStackRenderState item,HumanoidArm arm,Vector3f point){
        var layer=(FusionLayerAccessor)((FusionStateAccessor)item).wildcraft$layers()[0];var pose=new PoseStack();layer.wildcraft$transform().apply(arm==HumanoidArm.LEFT,pose.last());pose.mulPose(layer.wildcraft$local());return pose.last().pose().transformPosition(point);
    }
    /** Preserve both segment lengths and the authored elbow pole while meeting the real secondary handle. */
    private static FirstPersonSword.Frame support(FirstPersonSword.Frame f,ItemStackRenderState item,HumanoidArm main,float weight){
        int side=main==HumanoidArm.RIGHT?0:1,off=1-side,at=off*3;var sample=f.sample();var bones=sample.bones().clone();var grips=sample.grips().clone();var root=FirstPersonSword.camera(f.data());
        var pose=FirstPersonSword.heldPose(f,item,main,false);var target=pose.transformPosition(nativePoint(item,main,new Vector3f(.08F,.08F,.5F)));root.invert().transformPosition(target);
        var direction=quat(bones[side*3+1]).mul(new Quaternionf(sample.swords()[side][0],sample.swords()[side][1],sample.swords()[side][2],sample.swords()[side][3])).transform(new Vector3f(0,0,-.14F));var authored=vec(grips[side]).add(direction);var goal=vec(grips[off]).add(target.sub(authored).mul(weight));
        var shoulder=vec(bones[at]);var oldElbow=vec(bones[at+1]);var oldPalm=vec(grips[off]);var ray=new Vector3f(goal).sub(shoulder);float distance=ray.length();if(distance<=.133F||distance>=.617F)return null;ray.div(distance);
        var pole=new Vector3f(oldElbow).sub(shoulder);pole.fma(-pole.dot(ray),ray);if(pole.lengthSquared()<1e-8F)return null;pole.normalize();float along=(.375F*.375F-.2425F*.2425F+distance*distance)/(2*distance);var elbow=new Vector3f(shoulder).fma(along,ray).fma((float)Math.sqrt(Math.max(0,.375F*.375F-along*along)),pole);
        var upper=new Quaternionf().rotationTo(new Vector3f(oldElbow).sub(shoulder).normalize(),new Vector3f(elbow).sub(shoulder).normalize()).mul(quat(bones[at])).normalize();var lower=new Quaternionf().rotationTo(new Vector3f(oldPalm).sub(oldElbow).normalize(),new Vector3f(goal).sub(elbow).normalize()).mul(quat(bones[at+1])).normalize();bones[at]=bone(shoulder,upper);bones[at+1]=bone(elbow,lower);bones[at+2]=bone(elbow,new Quaternionf(upper).slerp(lower,.5F).normalize());grips[off]=new float[]{goal.x,goal.y,goal.z};
        return new FirstPersonSword.Frame(f.data(),new FirstPersonSword.Sample(bones,grips,sample.swords(),sample.shields()),f.clip(),f.sourceTime());
    }
    public static boolean submit(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,InteractionHand hand,PoseStack poses,SubmitNodeCollector collector,int light){
        var frame=frame(a,h);if(frame==null)return false;var f=frame.pose;
        if(hand==InteractionHand.MAIN_HAND)FirstPersonSword.submitArms(f,a,poses,collector,light);
        var item=hand==InteractionHand.MAIN_HAND?h.mainHandRenderState:h.offHandRenderState;
        if(!item.isEmpty()){var arm=hand==InteractionHand.MAIN_HAND?a.mainArm:a.mainArm.getOpposite();poses.pushPose();try{poses.mulPose(FirstPersonSword.heldPose(f,item,arm,hand==InteractionHand.OFF_HAND));item.submit(poses,collector,light,OverlayTexture.NO_OVERLAY,a.outlineColor);}finally{poses.popPose();}}
        return true;
    }
}
