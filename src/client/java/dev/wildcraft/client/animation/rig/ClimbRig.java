package dev.wildcraft.client.animation.rig;

import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.ClimbSurface;
import dev.wildcraft.traversal.Gliding;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Adopted empty-hand climb skin on qualified planar walls; entity motion remains native. */
public final class ClimbRig {
    private static final float SCALE=.9375F;
    private static final Set<String> REQUIRED=Set.of("climb_enter","climb_enter_left","climb_enter_right","climb_up","climb_left","climb_right","climb_idle_up","climb_idle_left","climb_idle_right","climb_settle_up","climb_settle_left","climb_settle_right");
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final Map<Player,Direction> FACES=new WeakHashMap<>();
    private static final Set<Player> CORNERS=Collections.newSetFromMap(new WeakHashMap<>());
    private static final class Motion {
        Vec3 position;long stamp;Object dimension;PlayerModelType model;Direction face;
        String mode,clip,lastDirection="up";float age,join,weight;double up,left,right;
        Matrix4f[] from,last;
    }
    private ClimbRig(){}
    static void clear(){MOTION.clear();FACES.clear();CORNERS.clear();}
    static void cancel(Player p){MOTION.remove(p);}
    private static boolean reject(Player p){MOTION.remove(p);return false;}
    static boolean extract(Player p,AvatarRenderState state,float partial){
        var client=Minecraft.getInstance();var visual=p.getAttached(Climbing.VISUAL);
        if(visual==null||!visual.active()){FACES.remove(p);CORNERS.remove(p);return reject(p);}
        if(visual.face()<2||visual.face()>5)return reject(p);Direction face=Direction.values()[visual.face()];
        Direction previous=FACES.put(p,face);if(previous!=null&&previous!=face)CORNERS.add(p);
        if(CORNERS.contains(p))return reject(p);
        if(visual==null||!visual.active()||!Climbing.eligible(p)||Gliding.active(p)||p.isInvisible()||p.isCurrentlyGlowing()
            ||!p.getMainHandItem().isEmpty()||!p.getOffhandItem().isEmpty()||p.isUsingItem()||state.currentSwing!=null
            ||MeleeActions.view(p).phase()!=MeleeView.IDLE||visual.motion()==2||visual.motion()==5
            ||state.scale!=1||state.showCape&&state.skin.cape()!=null
            ||p==client.player&&client.gui.screen()!=null&&!client.gui.screen().isPauseScreen())return reject(p);
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return reject(p);
        var settles=state.getData(BackTransitions.SETTLES);if(settles!=null)for(var s:settles)if(s!=null)return reject(p);
        var contact=ClimbSurface.find(p,face,true,Climbing.intent(p).vertical()>0,Climbing.intent(p).sideways());
        if(contact==null||contact.face()!=face||contact.cornerFrom()!=null||contact.mantle())return reject(p);
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);
        if(data==null||!data.clips().keySet().containsAll(REQUIRED))return reject(p); // dev.30/local-source-free builds retain old traversal.
        Vec3 position=p.getPosition(partial);var normal=new Vec3(face.getStepX(),0,face.getStepZ());
        var origin=position.add(0,.9,0);var hit=ray(p,origin,origin.add(normal.scale(.5)));
        if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection()!=face.getOpposite())return reject(p);
        double wallDistance=hit.getLocation().subtract(origin).dot(normal);if(wallDistance<.26||wallDistance>.4)return reject(p);
        var m=MOTION.get(p);
        if(m!=null&&(m.dimension!=p.level().dimension()||m.model!=state.skin.model()||m.face!=face||m.position.distanceToSqr(position)>.25)){
            // Face changes/corrections retain the established corner path until a subsequent fresh grip.
            return reject(p);
        }
        boolean fresh=m==null;if(fresh){m=new Motion();m.position=p.position();m.dimension=p.level().dimension();m.model=state.skin.model();m.face=face;MOTION.put(p,m);}
        long now=System.nanoTime();float dt=m.stamp==0||client.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;
        // Accumulate each actual entity displacement once; independent render partials cannot count the same step twice.
        Vec3 moved=p.position().subtract(m.position);m.position=p.position();
        if(!client.isPaused()){
            m.up+=Math.max(0,moved.y);double side=moved.x*face.getStepZ()-moved.z*face.getStepX();
            m.left+=Math.max(0,side);m.right+=Math.max(0,-side);
        }
        // Historical visual codes 3/4 name the server tangent sign, opposite to the actual A/D input.
        String direction=switch(visual.motion()){case 1->"up";case 3->"right";case 4->"left";default->null;};
        String mode=direction==null?"idle_"+m.lastDirection:direction;
        if(!mode.equals(m.mode)){m.mode=mode;m.age=0;m.from=m.last;m.join=0;if(direction!=null)m.lastDirection=direction;}
        else m.age+=dt;
        m.join+=dt;m.weight=Math.min(1,m.weight+dt/.12F);
        String clip=direction==null?(m.age<.3F?"climb_settle_":"climb_idle_")+m.lastDirection
            :m.age<.3F?(direction.equals("up")?"climb_enter":"climb_enter_"+direction):"climb_"+direction;
        float time;
        if(clip.startsWith("climb_enter")||clip.startsWith("climb_settle"))time=Math.clamp(m.age/.3F,0,1);
        else if(direction==null)time=(m.age-.3F)%data.clips().get(clip).duration()/data.clips().get(clip).duration();
        else {
            Vec3 pending=p.position().subtract(position);double pendingSide=pending.x*face.getStepZ()-pending.z*face.getStepX();
            double distance=direction.equals("up")?m.up-Math.max(0,pending.y):direction.equals("left")?m.left-Math.max(0,pendingSide):m.right-Math.max(0,-pendingSide);
            time=(float)((Math.max(0,distance)/(direction.equals("up")?.43:.32))%1);
        }
        if(!clip.equals(m.clip)){m.from=m.last;m.join=0;m.clip=clip;}
        var global=CombatRig.sample(variant,clip,time);float join=Math.clamp(m.join/.12F,0,1);join=join*join*(3-2*join);
        if(m.from!=null&&join<1)for(int i=0;i<global.length;i++)global[i]=CombatRig.blend(m.from[i],global[i],join);
        m.last=Arrays.stream(global).map(Matrix4f::new).toArray(Matrix4f[]::new);
        var points=contacts(data.clips().get(clip),time);if(points==null)return reject(p);
        double depth=0;int planted=0;for(var point:points)if(point[3]==1){depth+=point[2];planted++;}
        // Entry has no planted endpoints; the authored settled hand/foot depth is .290/.295.
        float offset=(float)(wallDistance/SCALE-(planted==0?.2925:depth/planted));if(Math.abs(offset)>.13)return reject(p);
        int touching=0;
        for(var point:points){
            Vec3 at=worldContact(position,face,point,offset);boolean near=Math.abs(at.subtract(hit.getLocation()).dot(normal))<.012;
            if(point[3]!=1&&!near)continue;
            var support=ray(p,at.subtract(normal.scale(.12)),at.add(normal.scale(.12)));
            if(support.getType()!=HitResult.Type.BLOCK||support.getDirection()!=face.getOpposite()
                ||Math.abs(support.getLocation().subtract(hit.getLocation()).dot(normal))>.012)return reject(p);
            if(near)touching++;
        }
        // A short authored release interval is not a planted flag; still require actual near-wall support.
        if(!clip.startsWith("climb_enter")&&touching<2)return reject(p);
        var reference=CombatRigLayer.reference(state.skin.model());float yaw=state.yRot,pitch=state.xRot;
        state.yRot=Math.clamp(yaw,-20,20);state.xRot=Math.clamp(pitch,-8,8);
        try{reference.setupAnim(state);}finally{state.yRot=yaw;state.xRot=pitch;}
        for(int i=0;i<global.length;i++){
            var part=CombatRigLayer.part(reference,data.bones()[i]);var nativeBone=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(part)).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
            global[i]=CombatRig.blend(nativeBone,global[i],m.weight);global[i].m32(global[i].m32()+offset*m.weight);
        }
        int head=Arrays.asList(data.bones()).indexOf("head");var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(reference.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();
        global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.bodyRot=face.toYRot();state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,clip,time,m.weight));return true;
    }
    private static BlockHitResult ray(Player p,Vec3 from,Vec3 to){return p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));}
    /** Authored sole/palm contact points, distinct from decorative equipment references. */
    public static float[][] contacts(CombatRig.Clip c,float normalized){
        if(c.contacts()==null)return null;float at=Math.clamp(normalized,0,1)*(c.contacts().length-1);int a=(int)at,b=Math.min(a+1,c.contacts().length-1);float f=at-a;float[][] result=new float[4][4];
        for(int i=0;i<4;i++){for(int j=0;j<3;j++)result[i][j]=Mth.lerp(f,c.contacts()[a][i][j],c.contacts()[b][i][j]);result[i][3]=c.contacts()[a][i][3]==1&&c.contacts()[b][i][3]==1?1:0;}return result;
    }
    /** Native Avatar .9375 scale and .001 model-origin lift, for real wall-shape qualification. */
    public static Vec3 worldContact(Vec3 position,Direction face,float[] point,float offset){return position.add(SCALE*(point[0]*face.getStepZ()+(point[2]+offset)*face.getStepX()),SCALE*(point[1]+.001F),SCALE*(-point[0]*face.getStepX()+(point[2]+offset)*face.getStepZ()));}
}
