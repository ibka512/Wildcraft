package dev.wildcraft.client.animation.rig;

import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.*;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.network.MeleeView;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Bounded adopted trident gesture on the existing same-reference, instant ownership transition. */
public final class TridentTransitionRig {
    private TridentTransitionRig() { }
    static boolean extract(Player p,AvatarRenderState state,float partial) {
        var c=Minecraft.getInstance();var view=MeleeActions.view(p);
        if(!Combat.compatible(p)||!p.onGround()||p.getPose()!=Pose.STANDING||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()
            ||state.scale!=1||p.isUsingItem()||p.getDeltaMovement().horizontalDistanceSqr()>=.0004
            ||view.phase()!=MeleeView.IDLE||view.hurt()!=0||p.hurtTime>0||ParryActions.view(p).remaining()!=0
            ||!p.getOffhandItem().isEmpty()&&!p.getOffhandItem().is(Items.SHIELD)
            ||p==c.player&&c.gui.screen()!=null&&!c.gui.screen().isPauseScreen())return false;
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty()&&!(slot==EquipmentSlot.CHEST&&p.getItemBySlot(slot).is(Items.ELYTRA)))return false;
        boolean waist=state.getDataOrDefault(BackTransitions.WAIST,false);
        if(waist&&!(state.showCape&&state.skin.cape()!=null)&&!p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA))return false;
        var settles=state.getData(BackTransitions.SETTLES);if(settles==null)return false;
        for(int i=1;i<settles.length;i++)if(settles[i]!=null)return false;
        var settle=BackTransitions.forArm(state,p.getMainArm());if(settle==null||settle.category()!=BackEquipment.MELEE||settle.handOwner()!=1)return false;
        var visual=p.getAttached(BackEquipment.VISUAL);if(visual==null||visual.melee().reference()==0||visual.melee().owner()!=settle.owner())return false;
        var trident=settle.draw()?p.getMainHandItem():BackEquipment.view(p).melee();
        if(MeleeWeapon.of(trident)!=MeleeWeapon.TRIDENT||settle.draw()&&settle.owner()!=1||!settle.draw()&&!p.getMainHandItem().isEmpty())return false;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";
        String name="trident_equipment_"+(settle.draw()?"draw":"stow")+"_"+(waist?"waist":"back")+"_"+(p.getOffhandItem().isEmpty()?"empty":"shield")+"_"+(p.getMainArm()==HumanoidArm.RIGHT?"R":"L");
        var data=CombatRig.asset(variant);if(data==null||!data.clips().containsKey(name))return false;
        float time=settle.progress(),weight=BackTransitions.smooth(Math.clamp(Math.min(time/.12F,(1-time)/.12F),0,1));
        var global=CombatRig.sample(variant,name,time);
        // Proposed sampled reference correction only; all 22 skin joint matrices stay authored.
        var bones=Arrays.asList(data.bones());String mainSide=p.getMainArm()==HumanoidArm.RIGHT?"right":"left";
        for(String side:new String[]{"left","right"})global[bones.indexOf(side+"_palm_grip")].setTranslation(global[bones.indexOf(side+"_wrist")].transformPosition(new Vector3f(0,.0625F,0)));
        global[bones.indexOf(mainSide+"_sword_reference")].setTranslation(global[bones.indexOf(mainSide+"_palm_grip")].getTranslation(new Vector3f()));
        if(p.getOffhandItem().is(Items.SHIELD)){int shield=Arrays.asList(data.bones()).indexOf((p.getMainArm()==HumanoidArm.RIGHT?"left":"right")+"_shield_mount");global[shield].translate(0,.125F,0);}
        var model=CombatRigLayer.reference(state.skin.model());model.setupAnim(state);var names=Arrays.asList(data.bones());
        for(int i=0;i<global.length;i++){
            var nativeBone=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(model,data.bones()[i]))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
            String bone=data.bones()[i];
            for(var arm:HumanoidArm.values()){
                String side=arm==HumanoidArm.RIGHT?"right":"left";
                boolean main=arm==p.getMainArm(),shield=!main&&p.getOffhandItem().is(Items.SHIELD);
                if(bone.equals(side+"_sword_reference")&&main&&settle.draw())nativeBone=CombatRig.nativeFeedbackReference(model,state,arm,false);
                if(bone.equals(side+"_shield_mount")&&shield)nativeBone=CombatRig.nativeFeedbackReference(model,state,arm,true);
            }
            global[i]=CombatRig.blend(nativeBone,global[i],weight);
        }
        for(String side:new String[]{"left","right"}){
            var foot=global[names.indexOf(side+"_foot")].getTranslation(new Vector3f());var sole=LockedWalkRig.worldFoot(p.getPosition(partial),state.bodyRot,new float[]{foot.x,foot.y,foot.z});
            var hit=p.level().clip(new ClipContext(sole.add(0,.16,0),sole.add(0,-.16,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection()!=net.minecraft.core.Direction.UP||Math.abs(hit.getLocation().y-p.getY())>.012)return false;
        }
        int head=names.indexOf("head");var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(model.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();
        global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,name,time,weight));return true;
    }
}
