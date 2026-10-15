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

/** Bounded adopted shield gesture on the existing same-reference, instant ownership transition. */
public final class ShieldTransitionRig {
    private ShieldTransitionRig() { }
    static boolean extract(Player p,AvatarRenderState state,float partial) {
        var c=Minecraft.getInstance();var view=MeleeActions.view(p);
        if(!Combat.compatible(p)||!p.onGround()||p.getPose()!=Pose.STANDING||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()
            ||state.scale!=1||p.isUsingItem()||p.getDeltaMovement().horizontalDistanceSqr()>=.0004
            ||view.phase()!=MeleeView.IDLE||view.hurt()!=0||p.hurtTime>0||ParryActions.view(p).remaining()!=0
            ||state.showCape&&state.skin.cape()!=null||state.getDataOrDefault(BackTransitions.WAIST,false)
            ||p==c.player&&c.gui.screen()!=null&&!c.gui.screen().isPauseScreen())return false;
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return false;
        var settles=state.getData(BackTransitions.SETTLES);if(settles==null)return false;
        for(int i=0;i<settles.length;i++)if(i!=BackEquipment.SHIELD&&settles[i]!=null)return false;
        // Ordinary offhand routes are transfer/use/GUI cancellations or publish an intermediate main owner.
        // Keep them native until a legal owner2 source event is actually verified.
        var event=settles[BackEquipment.SHIELD];if(event==null||event.handOwner()!=1)return false;
        var arm=event.handOwner()==1?p.getMainArm():p.getMainArm().getOpposite();
        var settle=BackTransitions.forArm(state,arm);if(settle!=event)return false;
        var hand=BackTransitions.held(state,arm);var other=BackTransitions.held(state,arm.getOpposite());
        if(!other.isEmpty()&&MeleeWeapon.of(other)!=MeleeWeapon.SWORD)return false;
        var visual=p.getAttached(BackEquipment.VISUAL);if(visual==null||visual.shield().reference()==0||visual.shield().owner()!=settle.owner())return false;
        var shieldStack=settle.draw()?hand:BackEquipment.view(p).shield();
        if(!shieldStack.is(Items.SHIELD)||settle.draw()&&settle.owner()!=settle.handOwner()||!settle.draw()&&!hand.isEmpty())return false;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";
        String name="shield_equipment_"+(settle.draw()?"draw":"stow")+"_back_"+(other.isEmpty()?"empty":"sword")+"_"+(arm==HumanoidArm.RIGHT?"R":"L");
        var data=CombatRig.asset(variant);if(data==null||!data.clips().containsKey(name))return false;
        float time=settle.progress(),weight=BackTransitions.smooth(Math.clamp(Math.min(time/.12F,(1-time)/.12F),0,1));
        var global=CombatRig.sample(variant,name,time);
        // Adopted reference handle is at local (0,.125,.03125). Native handle center
        // is .03125 ahead of its .09375 bound center; keep native size/UV axes and align the real handle.
        int shieldMount=Arrays.asList(data.bones()).indexOf((arm==HumanoidArm.RIGHT?"right":"left")+"_shield_mount");
        global[shieldMount].translate(0,.125F,0);
        var model=CombatRigLayer.reference(state.skin.model());model.setupAnim(state);var names=Arrays.asList(data.bones());
        for(int i=0;i<global.length;i++){
            var nativeBone=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(model,data.bones()[i]))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
            String bone=data.bones()[i];
            for(var heldArm:HumanoidArm.values()){
                String side=heldArm==HumanoidArm.RIGHT?"right":"left";
                if(bone.equals(side+"_sword_reference")&&heldArm==arm.getOpposite()&&!other.isEmpty())nativeBone=CombatRig.nativeFeedbackReference(model,state,heldArm,false);
                if(bone.equals(side+"_shield_mount")&&heldArm==arm&&settle.draw())nativeBone=CombatRig.nativeFeedbackReference(model,state,heldArm,true);
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
