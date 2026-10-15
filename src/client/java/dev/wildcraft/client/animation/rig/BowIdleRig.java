package dev.wildcraft.client.animation.rig;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Adopted idle only; actual bow use/shot, travel, inventory and first-person remain native. */
public final class BowIdleRig {
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion {long stamp;float age,weight;Object dimension;PlayerModelType model;HumanoidArm arm;boolean carry;}
    private BowIdleRig(){}
    static void clear(){MOTION.clear();}static void cancel(Player p){MOTION.remove(p);}
    private static boolean reject(Player p){cancel(p);return false;}
    static boolean extract(Player p,AvatarRenderState state){
        var c=Minecraft.getInstance();var v=MeleeActions.view(p);boolean carry=p.getOffhandItem().is(Items.SHIELD);
        if(!Combat.compatible(p)||!p.getMainHandItem().is(Items.BOW)||!p.getOffhandItem().isEmpty()&&!carry||p.isUsingItem()||!p.onGround()||p.getPose()!=Pose.STANDING||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||state.scale!=1
            ||p.getDeltaMovement().horizontalDistanceSqr()>=.000025||v.phase()!=MeleeView.IDLE||v.hurt()!=0||ParryActions.view(p).remaining()!=0||state.showCape&&state.skin.cape()!=null
            ||p==c.player&&(c.gui.screen()!=null&&!c.gui.screen().isPauseScreen()||c.options.keyAttack.isDown()||c.options.keyUse.isDown()))return reject(p);
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return reject(p);
        var settles=state.getData(BackTransitions.SETTLES);if(settles!=null)for(var s:settles)if(s!=null)return reject(p);
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard",name="bow_idle_"+(carry?"carry":"empty")+"_"+(p.getMainArm()==HumanoidArm.RIGHT?"R":"L");var d=CombatRig.asset(variant);if(d==null||!d.clips().containsKey(name))return reject(p);
        var m=MOTION.computeIfAbsent(p,k->new Motion());if(m.dimension!=p.level().dimension()||m.model!=state.skin.model()||m.arm!=p.getMainArm()||m.carry!=carry){m.age=m.weight=0;m.stamp=0;m.dimension=p.level().dimension();m.model=state.skin.model();m.arm=p.getMainArm();m.carry=carry;}
        long now=System.nanoTime();float dt=m.stamp==0||c.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;m.age+=dt;m.weight=Math.min(1,m.weight+dt/.18F);if(m.weight<=0)return false;
        float time=m.age%d.clips().get(name).duration()/d.clips().get(name).duration();var global=CombatRig.sample(variant,name,time);var model=CombatRigLayer.reference(state.skin.model());model.setupAnim(state);
        for(int i=0;i<global.length;i++){
            String bone=d.bones()[i];var nativeBone=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(model,bone))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(d.bindWorld()[i]));
            String main=p.getMainArm()==HumanoidArm.RIGHT?"right_":"left_",off=p.getMainArm()==HumanoidArm.RIGHT?"left_":"right_";
            if(bone.equals(main+"sword_reference")&&!state.getMainHandItemState().isEmpty())nativeBone=CombatRig.nativeFeedbackReference(model,state,p.getMainArm(),false);
            if(carry&&bone.equals(off+"shield_mount")&&!(p.getMainArm()==HumanoidArm.RIGHT?state.leftHandItemState:state.rightHandItemState).isEmpty())nativeBone=CombatRig.nativeFeedbackReference(model,state,p.getMainArm()==HumanoidArm.RIGHT?HumanoidArm.LEFT:HumanoidArm.RIGHT,true);
            global[i]=CombatRig.blend(nativeBone,global[i],m.weight);
        }
        int head=Arrays.asList(d.bones()).indexOf("head");var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(model.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(d,global,name,time,m.weight));return true;
    }
}
