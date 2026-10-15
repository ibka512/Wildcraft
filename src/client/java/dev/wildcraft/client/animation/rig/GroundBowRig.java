package dev.wildcraft.client.animation.rig;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Adopted left-bow and derived right-bow draw/hold follows actual native use. No shot, inventory or skill clock is owned here. */
public final class GroundBowRig {
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion {long stamp;float weight,holdAge;int ticks;Object dimension;PlayerModelType model;InteractionHand hand;HumanoidArm arm;}
    private GroundBowRig(){}
    static void clear(){MOTION.clear();}static void cancel(Player p){MOTION.remove(p);}
    private static boolean reject(Player p){cancel(p);return false;}
    static boolean extract(Player p,AvatarRenderState state,float partial){
        var c=Minecraft.getInstance();var v=MeleeActions.view(p);
        var screen=c.gui.screen();boolean pauseScreen=screen!=null&&screen.isPauseScreen();
        boolean localReleased=p==c.player&&!c.options.keyUse.isDown()&&!c.isPaused()&&!pauseScreen;
        if(!Combat.compatible(p)||!p.isUsingItem()||!p.getUseItem().is(Items.BOW)||!p.onGround()||p.getPose()!=Pose.STANDING||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||state.scale!=1
            ||p.getDeltaMovement().horizontalDistanceSqr()>=.000025||v.phase()!=MeleeView.IDLE||v.hurt()!=0||ParryActions.view(p).remaining()!=0||state.showCape&&state.skin.cape()!=null
            ||localReleased||p==c.player&&screen!=null&&!pauseScreen)return reject(p);
        var hand=p.getUsedItemHand();var arm=hand==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite();
        var other=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        var bowItem=arm==HumanoidArm.LEFT?state.leftHandItemState:state.rightHandItemState;
        var bowStack=arm==HumanoidArm.LEFT?state.leftHandItemStack:state.rightHandItemStack;
        if(!other.isEmpty()||p.getUseItem()!=p.getItemInHand(hand)||!p.getItemInHand(hand).is(Items.BOW)||bowItem.isEmpty()||!bowStack.is(Items.BOW))return reject(p);
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return reject(p);
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);if(data==null)return reject(p);
        String suffix=arm==HumanoidArm.LEFT?"L":"R",bowSide=arm==HumanoidArm.LEFT?"left":"right",drawingSide=arm==HumanoidArm.LEFT?"right":"left";
        var draw=data.clips().get("bow_ground_draw_empty_"+suffix);var hold=data.clips().get("bow_ground_hold_empty_"+suffix);if(draw==null||draw.pullTimes()==null||hold==null)return reject(p);
        int ticks=p.getTicksUsingItem();var m=MOTION.computeIfAbsent(p,k->new Motion());
        if(m.dimension!=p.level().dimension()||m.model!=state.skin.model()||m.hand!=hand||m.arm!=arm||ticks<m.ticks){m.weight=m.holdAge=0;m.stamp=0;m.dimension=p.level().dimension();m.model=state.skin.model();m.hand=hand;m.arm=arm;}
        long now=System.nanoTime();float dt=m.stamp==0||c.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;m.ticks=ticks;m.weight=Math.min(1,m.weight+dt/.18F);if(m.weight<=0)return false;
        boolean full=ticks>=20;if(full)m.holdAge+=dt;else m.holdAge=0;
        String name=full?"bow_ground_hold_empty_"+suffix:"bow_ground_draw_empty_"+suffix;float time=full?m.holdAge%hold.duration()/hold.duration():CombatRig.pullSample(draw,ticks/20F);
        var global=CombatRig.sample(variant,name,time);var model=CombatRigLayer.reference(state.skin.model());model.setupAnim(state);var names=Arrays.asList(data.bones());
        // The source reference bow and vanilla sprite have different size/plane conventions.
        // Keep the actual native sprite; retarget only the drawing arm to its real nock.
        if(!fitDrawingArm(global,names,CombatRig.nativeBowString(new CombatRigLayer.Frame(data,global,name,time,1),bowItem,ticks,arm),drawingSide))return reject(p);
        for(int i=0;i<global.length;i++){
            var nativeBone=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(model,data.bones()[i]))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
            if(data.bones()[i].equals(bowSide+"_sword_reference")){nativeBone=CombatRig.nativeFeedbackReference(model,state,arm,false);if(arm==HumanoidArm.LEFT)nativeBone.rotateY((float)Math.PI);}
            global[i]=CombatRig.blend(nativeBone,global[i],m.weight);
        }
        for(String side:new String[]{"left","right"}){
            var foot=global[names.indexOf(side+"_foot")].getTranslation(new Vector3f());var sole=LockedWalkRig.worldFoot(p.getPosition(partial),state.bodyRot,new float[]{foot.x,foot.y,foot.z});
            var hit=p.level().clip(new ClipContext(sole.add(0,.16,0),sole.add(0,-.16,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection()!=net.minecraft.core.Direction.UP||Math.abs(hit.getLocation().y-p.getY())>.012)return reject(p);
        }
        int head=names.indexOf("head");var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(model.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,name,time,m.weight));return true;
    }
    /** Two authored segment lengths, source elbow bend plane, rigid bone rotations; never changes root or bow hand. */
    static boolean fitDrawingArm(Matrix4f[] g,List<String> names,Vector3f target){
        return fitDrawingArm(g,names,target,"right");
    }
    static boolean fitDrawingArm(Matrix4f[] g,List<String> names,Vector3f target,String side){
        int upper=names.indexOf(side+"_upper_arm"),elbow=names.indexOf(side+"_elbow_deform"),palm=names.indexOf(side+"_palm_grip");
        var shoulder=g[upper].getTranslation(new Vector3f());var oldElbow=g[elbow].getTranslation(new Vector3f());var oldPalm=g[palm].getTranslation(new Vector3f());
        var a=new Vector3f(oldElbow).sub(shoulder);var b=new Vector3f(oldPalm).sub(oldElbow);float l1=a.length(),l2=b.length();var direction=new Vector3f(target).sub(shoulder);float distance=direction.length();
        if(distance<Math.abs(l1-l2)+.00001F||distance>l1+l2-.00001F)return false;
        direction.div(distance);var plane=new Vector3f(a).fma(-a.dot(direction),direction);if(plane.lengthSquared()<1e-8F)return false;plane.normalize();
        float cosine=(l1*l1+distance*distance-l2*l2)/(2*l1*distance),sine=(float)Math.sqrt(Math.max(0,1-cosine*cosine));
        var nextElbow=new Vector3f(shoulder).fma(l1*cosine,direction).fma(l1*sine,plane);
        var q1=new Quaternionf().rotationTo(a.normalize(),new Vector3f(nextElbow).sub(shoulder).normalize());
        var q2=new Quaternionf().rotationTo(b.normalize(),new Vector3f(target).sub(nextElbow).normalize());
        var upperDelta=new Matrix4f().translation(shoulder).rotate(q1).translate(new Vector3f(shoulder).negate());
        var lowerDelta=new Matrix4f().translation(nextElbow).rotate(q2).translate(new Vector3f(oldElbow).negate());
        g[upper]=upperDelta.mul(g[upper]);
        g[elbow].translationRotateScale(nextElbow,new Quaternionf(q1).slerp(q2,.5F).mul(g[elbow].getUnnormalizedRotation(new Quaternionf()).normalize()),g[elbow].getScale(new Vector3f()));
        for(String n:new String[]{side+"_forearm",side+"_wrist",side+"_palm_grip",side+"_sword_reference",side+"_shield_mount"}){int i=names.indexOf(n);g[i]=new Matrix4f(lowerDelta).mul(g[i]);}
        return true;
    }

}
