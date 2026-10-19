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

/** Adopted ordinary airborne strike; native world recovery and movement remain authoritative. */
public final class AirMeleeRig {
    private AirMeleeRig() { }
    static boolean extract(Player p,AvatarRenderState state,float partial) {
        var client=Minecraft.getInstance();var view=MeleeActions.view(p);var weapon=MeleeWeapon.of(p.getMainHandItem());
        if(view.phase()!=MeleeView.AIR_SWING||view.charged()||weapon!=view.kind()
            ||weapon!=MeleeWeapon.SWORD&&weapon!=MeleeWeapon.AXE||p.onGround()||!Combat.compatible(p)
            ||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||p.isUsingItem()||state.scale!=1
            ||view.hurt()!=0||p.hurtTime>0||ParryActions.view(p).remaining()!=0
            ||!p.getOffhandItem().isEmpty()&&!p.getOffhandItem().is(Items.SHIELD)
            ||state.showCape&&state.skin.cape()!=null
            ||p==client.player&&client.gui.screen()!=null&&!client.gui.screen().isPauseScreen())return false;
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
            if(!p.getItemBySlot(slot).isEmpty())return false;
        var settles=state.getData(BackTransitions.SETTLES);if(settles!=null)for(var settle:settles)if(settle!=null)return false;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";
        String name=(weapon==MeleeWeapon.SWORD?"sword":"axe")+"_air_attack_"+(p.getOffhandItem().isEmpty()?"empty":"carry")+"_"+(p.getMainArm()==HumanoidArm.RIGHT?"R":"L");
        var data=CombatRig.asset(variant);if(data==null||!data.clips().containsKey(name))return false;
        // Native rendering already freezes partial; clearing it rewinds the paused source pose.
        float age=view.age()+partial,time=Math.clamp(age/Math.max(1,view.duration()),0,1);
        var global=CombatRig.sample(variant,name,time);var reference=CombatRigLayer.reference(state.skin.model());reference.setupAnim(state);
        // One world tick of entry, with no separate clock, stale pose, displacement or delayed damage.
        float weight=Math.clamp(age,0,1);weight=weight*weight*(3-2*weight);
        for(int i=0;i<global.length;i++){
            var nativeBone=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(reference,data.bones()[i]))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
            global[i]=CombatRig.blend(nativeBone,global[i],weight);
        }
        int head=Arrays.asList(data.bones()).indexOf("head");
        var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(reference.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();
        global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,name,time,weight));return true;
    }
}
