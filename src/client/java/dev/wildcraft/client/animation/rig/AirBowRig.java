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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Adopted left-bow and derived right-bow draw/hold follows actual native use. No shot, inventory or skill clock is owned here. */
public final class AirBowRig {
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion {long stamp;float weight,holdAge;Object dimension;PlayerModelType model;InteractionHand hand;HumanoidArm arm;net.minecraft.world.item.ItemStack use;}
    private AirBowRig(){}
    static void clear(){MOTION.clear();FocusLegRig.clear();}static void cancel(Player p){MOTION.remove(p);FocusLegRig.cancel(p);}
    private static boolean reject(Player p){cancel(p);return false;}
    static boolean extract(Player p,AvatarRenderState state,float partial){
        var c=Minecraft.getInstance();var v=MeleeActions.view(p);
        var screen=c.gui.screen();boolean pauseScreen=screen!=null&&screen.isPauseScreen();
        boolean localReleased=p==c.player&&!c.options.keyUse.isDown()&&!c.isPaused()&&!pauseScreen;
        if(!Combat.compatible(p)||!p.isUsingItem()||!p.getUseItem().is(Items.BOW)||p.onGround()||p.getPose()!=Pose.STANDING||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||state.scale!=1
            ||v.phase()!=MeleeView.IDLE||p.hurtTime>0||v.hurt()!=0||ParryActions.view(p).remaining()!=0||state.showCape&&state.skin.cape()!=null
            ||localReleased||p==c.player&&screen!=null&&!pauseScreen)return reject(p);
        var hand=p.getUsedItemHand();var arm=hand==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite();
        var other=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        var bowItem=arm==HumanoidArm.LEFT?state.leftHandItemState:state.rightHandItemState;
        var bowStack=arm==HumanoidArm.LEFT?state.leftHandItemStack:state.rightHandItemStack;
        if(!other.isEmpty()||p.getUseItem()!=p.getItemInHand(hand)||!p.getItemInHand(hand).is(Items.BOW)||bowItem.isEmpty()||!bowStack.is(Items.BOW))return reject(p);
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return reject(p);
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);if(data==null)return reject(p);
        String suffix=arm==HumanoidArm.LEFT?"L":"R",bowSide=arm==HumanoidArm.LEFT?"left":"right",drawingSide=arm==HumanoidArm.LEFT?"right":"left";
        var draw=data.clips().get("bow_air_draw_empty_"+suffix);var hold=data.clips().get("bow_air_hold_empty_"+suffix);if(draw==null||draw.pullTimes()==null||hold==null)return reject(p);
        // Focus client prediction can reconcile backwards within this same native use; it is not a new entry.
        int ticks=p.getTicksUsingItem();var m=MOTION.computeIfAbsent(p,k->new Motion());
        if(m.dimension!=p.level().dimension()||m.model!=state.skin.model()||m.hand!=hand||m.arm!=arm||m.use!=p.getUseItem()){m.weight=m.holdAge=0;m.stamp=0;m.dimension=p.level().dimension();m.model=state.skin.model();m.hand=hand;m.arm=arm;m.use=p.getUseItem();}
        long now=System.nanoTime();float dt=m.stamp==0||c.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;m.weight=Math.min(1,m.weight+dt/.18F);if(m.weight<=0)return false;
        boolean full=ticks>=20;if(full)m.holdAge+=dt;else m.holdAge=0;
        String name=full?"bow_air_hold_empty_"+suffix:"bow_air_draw_empty_"+suffix;float time=full?m.holdAge%hold.duration()/hold.duration():CombatRig.pullSample(draw,ticks/20F);
        var global=CombatRig.sample(variant,name,time);var model=CombatRigLayer.reference(state.skin.model());model.setupAnim(state);var names=Arrays.asList(data.bones());
        // The source reference bow and vanilla sprite have different size/plane conventions.
        // Keep the actual native sprite; retarget only the drawing arm to its real nock.
        if(!GroundBowRig.fitDrawingArm(global,names,CombatRig.nativeBowString(new CombatRigLayer.Frame(data,global,name,time,1),bowItem,ticks,arm),drawingSide))return reject(p);
        for(int i=0;i<global.length;i++){
            var nativeBone=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(model,data.bones()[i]))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
            if(data.bones()[i].equals(bowSide+"_sword_reference")){nativeBone=CombatRig.nativeFeedbackReference(model,state,arm,false);if(arm==HumanoidArm.LEFT)nativeBone.rotateY((float)Math.PI);}
            global[i]=CombatRig.blend(nativeBone,global[i],m.weight);
        }
        int head=names.indexOf("head");var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(model.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        FocusLegRig.apply(p,state,data,global);
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,name,time,m.weight));return true;
    }
}
