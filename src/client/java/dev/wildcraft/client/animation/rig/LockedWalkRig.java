package dev.wildcraft.client.animation.rig;

import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Actual cardinal locked steps on qualified flat support; combat/terrain otherwise stays native. */
public final class LockedWalkRig {
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion {
        Vec3 position;int tick;long stamp;Object dimension;PlayerModelType model;
        String direction,clip,equipment,side;float age,join,exit=-1;boolean moving;double distance;Matrix4f[] last,from;
    }
    private LockedWalkRig(){}
    static void clear(){MOTION.clear();}
    static void cancel(Player p){MOTION.remove(p);}
    private static boolean reject(Player p){MOTION.remove(p);return false;}
    static boolean extract(Player p,AvatarRenderState state,float partial){
        var client=Minecraft.getInstance();var v=MeleeActions.view(p);
        boolean guard=p.isUsingItem()&&p.getUsedItemHand()==net.minecraft.world.InteractionHand.OFF_HAND&&p.getUseItem().is(Items.SHIELD);
        if(!Combat.compatible(p)||!Combat.locked(p)||!p.onGround()||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||p.isUsingItem()&&!guard
            ||v.phase()!=MeleeView.IDLE||v.hurt()!=0||ParryActions.view(p).remaining()!=0&&!(guard&&GuardRig.available(p,state))||state.scale!=1
            ||MeleeWeapon.of(p.getMainHandItem())!=MeleeWeapon.SWORD||!p.getOffhandItem().isEmpty()&&!p.getOffhandItem().is(Items.SHIELD)
            ||state.showCape&&state.skin.cape()!=null||p==client.player&&client.gui.screen()!=null&&!client.gui.screen().isPauseScreen())return reject(p);
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return reject(p);
        var settles=state.getData(BackTransitions.SETTLES);if(settles!=null)for(var s:settles)if(s!=null)return reject(p);
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);if(data==null)return reject(p);
        String equipment=guard?"guard":p.getOffhandItem().isEmpty()?"empty":"carry",side=p.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT?"R":"L";
        var m=MOTION.get(p);if(m!=null&&(m.dimension!=p.level().dimension()||m.model!=state.skin.model()||!equipment.equals(m.equipment)||!side.equals(m.side)||m.position.distanceToSqr(p.position())>1))return reject(p);
        if(m==null){m=new Motion();m.position=p.position();m.tick=p.tickCount;m.dimension=p.level().dimension();m.model=state.skin.model();m.equipment=equipment;m.side=side;MOTION.put(p,m);}
        long now=System.nanoTime();float dt=m.stamp==0||client.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;
        Vec3 moved=p.position().subtract(m.position);m.position=p.position();
        double yaw=Math.toRadians(state.bodyRot),forward=-Math.sin(yaw)*moved.x+Math.cos(yaw)*moved.z,lateral=Math.cos(yaw)*moved.x+Math.sin(yaw)*moved.z;
        if(!client.isPaused()){
            boolean changedTick=m.tick!=p.tickCount;m.tick=p.tickCount;boolean actual=moved.horizontalDistanceSqr()>.000025;
            if(actual){
                if(Math.min(Math.abs(forward),Math.abs(lateral))>Math.max(Math.abs(forward),Math.abs(lateral))*.15||Math.abs(moved.y)>.02)return reject(p);
                String direction=Math.abs(forward)>=Math.abs(lateral)?forward>0?"forward":"backward":lateral>0?"left":"right";
                if(!direction.equals(m.direction)||!m.moving){m.direction=direction;m.age=0;m.distance=0;m.join=0;m.from=m.last;m.exit=-1;}else m.age+=dt;
                m.distance+=Math.abs(direction.equals("forward")||direction.equals("backward")?forward:lateral);m.moving=true;
            }else if(changedTick&&m.moving){m.moving=false;m.exit=0;m.from=m.last;m.join=0;}
            else if(m.moving)m.age+=dt;
            if(!m.moving&&m.exit>=0)m.exit+=dt;
            m.join+=dt;
        }
        if(m.direction==null)return false;
        String phase=m.moving?(m.age<(m.direction.equals("left")||m.direction.equals("right")?.24F:.18F)?"enter":"cycle"):"exit";
        String name="walk_"+m.direction+"_"+phase+"_"+equipment+"_"+side;var clip=data.clips().get(name);if(clip==null||clip.feet()==null)return reject(p);
        if(!m.moving&&m.exit>=clip.duration()){m.exit=-1;m.clip=null;m.last=null;m.from=null;return false;}
        if(!m.moving&&m.exit<0)return false;
        float time;
        if(phase.equals("cycle")){
            var pending=p.position().subtract(p.getPosition(partial));double along=m.direction.equals("forward")||m.direction.equals("backward")?-Math.sin(yaw)*pending.x+Math.cos(yaw)*pending.z:Math.cos(yaw)*pending.x+Math.sin(yaw)*pending.z;
            time=(float)((Math.max(0,m.distance-Math.abs(along))/clip.distance())%1);
        }else time=Math.clamp((m.moving?m.age:m.exit)/clip.duration(),0,1);
        if(!name.equals(m.clip)){m.clip=name;m.from=m.last;m.join=0;}
        var global=CombatRig.sample(variant,name,time);float join=Math.clamp(m.join/(phase.equals("exit")?clip.duration():.12F),0,1);join=join*join*(3-2*join);
        if(m.from!=null&&join<1)for(int i=0;i<global.length;i++)global[i]=CombatRig.blend(m.from[i],global[i],join);
        var feet=feet(clip,time);Vec3 position=p.getPosition(partial);
        for(var foot:feet)if(foot[3]==1){
            Vec3 sole=worldFoot(position,state.bodyRot,foot);var hit=p.level().clip(new ClipContext(sole.add(0,.16,0),sole.add(0,-.16,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection()!=net.minecraft.core.Direction.UP||Math.abs(hit.getLocation().y-position.y)>.012)return reject(p);
        }
        m.last=Arrays.stream(global).map(Matrix4f::new).toArray(Matrix4f[]::new);
        var reference=CombatRigLayer.reference(state.skin.model());reference.setupAnim(state);int head=Arrays.asList(data.bones()).indexOf("head");
        var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(reference.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();
        global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        if(guard){GuardRig.mark(p,state,partial);GuardRig.apply(p,state,data,global,1);}
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,name,time,1));return true;
    }
    public static float[][] feet(CombatRig.Clip clip,float time){
        float at=Math.clamp(time,0,1)*(clip.feet().length-1);int a=(int)at,b=Math.min(a+1,clip.feet().length-1);float f=at-a;var result=new float[2][4];
        for(int i=0;i<2;i++){for(int j=0;j<3;j++)result[i][j]=Mth.lerp(f,clip.feet()[a][i][j],clip.feet()[b][i][j]);result[i][3]=clip.feet()[a][i][3]==1&&clip.feet()[b][i][3]==1?1:0;}return result;
    }
    /** Authored ankle center .125 above the sole, in the existing native Avatar transform. */
    public static Vec3 worldFoot(Vec3 position,float bodyYaw,float[] point){double yaw=Math.toRadians(bodyYaw);return position.add(.9375*(Math.cos(yaw)*point[0]-Math.sin(yaw)*point[2]),.9375*(point[1]-.125+.001),.9375*(Math.sin(yaw)*point[0]+Math.cos(yaw)*point[2]));}
}
