package dev.wildcraft.client.animation.rig;

import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.traversal.Gliding;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Adopted glider skin and a shared canopy transform; all flight and inventory remain native. */
public final class GlideRig {
    public static final RenderStateDataKey<Canopy> CANOPY=RenderStateDataKey.create(()->"wildcraft:glide_rig_canopy");
    public record Canopy(Matrix4f matrix,float bank){}
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion {long stamp;Object dimension;PlayerModelType model;float age,exit=-1,bank,target,fromBank,bankAge,yaw;boolean wasOpen;Matrix4f[] last,from;}
    private GlideRig(){}
    static void clear(){MOTION.clear();}
    static void cancel(Player p){MOTION.remove(p);}
    private static boolean reject(Player p){MOTION.remove(p);return false;}
    static boolean extract(Player p,AvatarRenderState state,float partial){
        state.setData(CANOPY,null);var client=Minecraft.getInstance();boolean open=Gliding.active(p);var m=MOTION.get(p);
        if(!open&&m==null)return false;
        if(!dev.wildcraft.traversal.Climbing.eligible(p)||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||p.hurtTime>0||p.isUsingItem()||state.currentSwing!=null
            ||state.scale!=1||state.showCape&&state.skin.cape()!=null
            ||p==client.player&&client.gui.screen()!=null&&!client.gui.screen().isPauseScreen()
            ||!open&&(!p.getMainHandItem().isEmpty()||!p.getOffhandItem().isEmpty()||dev.wildcraft.traversal.Climbing.active(p)||dev.wildcraft.traversal.Climbing.intent(p).held()))return reject(p);
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return reject(p);
        var settles=state.getData(BackTransitions.SETTLES);if(settles!=null)for(var s:settles)if(s!=null)return reject(p);
        if(open&&!Gliding.eligible(p))return reject(p);
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);
        if(data==null||!data.clips().keySet().containsAll(Set.of("glide_enter","glide_hold","glide_exit")))return reject(p);
        if(m==null||m.dimension!=p.level().dimension()||m.model!=state.skin.model()){
            if(!open)return reject(p);m=new Motion();m.dimension=p.level().dimension();m.model=state.skin.model();m.yaw=p.getYRot();MOTION.put(p,m);
        }
        long now=System.nanoTime();float dt=m.stamp==0||client.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;
        if(open){if(!m.wasOpen){m.age=0;m.exit=-1;m.from=m.last;}else m.age+=dt;}
        else if(m.wasOpen){m.exit=0;m.from=m.last;}
        else m.exit+=dt;
        m.wasOpen=open;if(!open&&m.exit>=.2F)return reject(p);
        if(open&&!client.isPaused()){
            double yaw=Math.toRadians(p.getYRot());var velocity=p.getDeltaMovement();var wind=dev.wildcraft.weather.WindSystem.local(p);
            double lateral=(velocity.x/.8-wind.x())*Math.cos(yaw)+(velocity.z/.8-wind.z())*Math.sin(yaw);
            float yawDelta=Mth.wrapDegrees(p.getYRot()-m.yaw);m.yaw=p.getYRot();
            float target=Math.abs(lateral)>.08?(float)Math.copySign(8,lateral):dt>0&&Math.abs(yawDelta)/dt>15?Math.copySign(8,-yawDelta):0;
            if(target!=m.target){m.fromBank=m.bank;m.target=target;m.bankAge=0;}m.bankAge+=dt;
            float t=Math.clamp(m.bankAge/.3F,0,1);m.bank=Mth.lerp(t*t*(3-2*t),m.fromBank,m.target);
        }
        String clip=open?(m.age<.3F?"glide_enter":"glide_hold"):"glide_exit";
        float time=open?(m.age<.3F?Math.clamp(m.age/.3F,0,1):(m.age-.3F)%data.clips().get(clip).duration()/data.clips().get(clip).duration()):Math.clamp(m.exit/.2F,0,1);
        var global=CombatRig.sample(variant,clip,time);
        if(open){
            var bank=new Matrix4f().translation(0,1.6875F,.3375F).rotateZ((float)Math.toRadians(-m.bank)).translate(0,-1.6875F,-.3375F);
            for(int i=1;i<global.length;i++)global[i]=new Matrix4f(bank).mul(global[i]); // Root/physics stays unchanged.
            var canopy=canopy(data.clips().get(clip),time);if(canopy==null)return reject(p);
            state.setData(CANOPY,new Canopy(new Matrix4f(CombatRigLayer.BASIS).mul(bank).mul(canopy).mul(CombatRigLayer.BASIS),m.bank));
        }else if(m.from!=null){
            float t=Math.clamp(m.exit/.2F,0,1);t=t*t*(3-2*t);
            for(int i=0;i<global.length;i++)global[i]=CombatRig.blend(m.from[i],global[i],t);
        }
        m.last=Arrays.stream(global).map(Matrix4f::new).toArray(Matrix4f[]::new);
        var reference=CombatRigLayer.reference(state.skin.model());reference.setupAnim(state);
        int head=Arrays.asList(data.bones()).indexOf("head");var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(reference.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();
        if(open)aim=new Quaternionf().rotationZ((float)Math.toRadians(-m.bank)).mul(aim);
        global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,clip,time,1));return true;
    }
    private static Matrix4f canopy(CombatRig.Clip clip,float time){
        if(clip.canopy()==null)return null;float at=Math.clamp(time,0,1)*(clip.canopy().length-1);int a=(int)at,b=Math.min(a+1,clip.canopy().length-1);
        return CombatRig.blend(CombatRig.matrix(clip.canopy()[a]),CombatRig.matrix(clip.canopy()[b]),at-a);
    }
}
