package dev.wildcraft.client.animation.rig;

import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Adopted airborne mace light/charge/hold/strong strike; native world recovery and movement remain authoritative. */
public final class AirMaceRig {
    private AirMaceRig() { }
    static boolean extract(Player p,AvatarRenderState state,float partial) {
        var client=Minecraft.getInstance();var view=MeleeActions.view(p);var weapon=MeleeWeapon.of(p.getMainHandItem());
        if((view.phase()!=MeleeView.AIR_SWING&&view.phase()!=MeleeView.CHARGE)||weapon!=view.kind()
            ||weapon!=MeleeWeapon.MACE||net.minecraft.world.item.MaceItem.canSmashAttack(p)||p.onGround()||p.getPose()!=net.minecraft.world.entity.Pose.STANDING||p.getDeltaMovement().horizontalDistanceSqr()>=.0004||!Combat.compatible(p)
            ||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||p.isUsingItem()||state.scale!=1
            ||view.hurt()!=0||p.hurtTime>0||ParryActions.view(p).remaining()!=0
            ||!p.getOffhandItem().isEmpty()&&!p.getOffhandItem().is(Items.SHIELD)
            ||state.showCape&&state.skin.cape()!=null
            ||p==client.player&&client.gui.screen()!=null&&!client.gui.screen().isPauseScreen())return false;
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
            if(!p.getItemBySlot(slot).isEmpty())return false;
        var settles=state.getData(BackTransitions.SETTLES);if(settles!=null)for(var settle:settles)if(settle!=null)return false;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";
        boolean charging=view.phase()==MeleeView.CHARGE;
        String name=(charging||view.charged()?"mace_attack_air_heavy_":"mace_attack_air_light_")+(p.getOffhandItem().isEmpty()?"empty":"shield")+"_"+(p.getMainArm()==HumanoidArm.RIGHT?"R":"L");
        var data=CombatRig.asset(variant);if(data==null||!data.clips().containsKey(name))return false;
        // Native rendering already freezes partial; clearing it rewinds the paused source pose.
        float age=view.age()+partial,progress=Math.clamp(age/Math.max(1,view.duration()),0,1);
        // Authored1.3 prepare / review .2 hold /1.0 release map to native26/variable hold/20, never a source release deadline.
        float time=charging?(float)(1.3/2.5)*progress:view.charged()?(float)(1.5/2.5)+(float)(1.0/2.5)*progress:progress;
        var global=CombatRig.sample(variant,name,time);var names=Arrays.asList(data.bones());for(String side:new String[]{"left","right"})global[names.indexOf(side+"_palm_grip")].setTranslation(global[names.indexOf(side+"_wrist")].transformPosition(new Vector3f(0,.0625F,0)));if(p.getOffhandItem().is(Items.SHIELD))global[names.indexOf((p.getMainArm()==HumanoidArm.RIGHT?"left":"right")+"_shield_mount")].translate(0,.125F,0);var reference=CombatRigLayer.reference(state.skin.model());reference.setupAnim(state);
        // One world tick of entry, with no separate clock, stale pose, displacement or delayed damage.
        float weight=!charging&&view.charged()?1:Math.clamp(age,0,1);weight=weight*weight*(3-2*weight);
        for(int i=0;i<global.length;i++){
            var nativeBone=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(reference,data.bones()[i]))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
            for(var arm:HumanoidArm.values()){
                String side=arm==HumanoidArm.RIGHT?"right":"left";
                if(data.bones()[i].equals(side+"_sword_reference")&&arm==p.getMainArm())nativeBone=CombatRig.nativeFeedbackReference(reference,state,arm,false,true);
                if(data.bones()[i].equals(side+"_shield_mount")&&arm!=p.getMainArm()&&p.getOffhandItem().is(Items.SHIELD))nativeBone=CombatRig.nativeFeedbackReference(reference,state,arm,true);
            }
            global[i]=CombatRig.blend(nativeBone,global[i],weight);
        }
        int head=Arrays.asList(data.bones()).indexOf("head");
        var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(reference.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();
        global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,name,time,weight));return true;
    }
}
