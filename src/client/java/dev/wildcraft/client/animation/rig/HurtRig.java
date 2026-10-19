package dev.wildcraft.client.animation.rig;
import dev.wildcraft.client.animation.MeleePoses;
import dev.wildcraft.combat.*;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Ordinary authoritative hurt as one rigid upper-body delta over the current native pose. */
public final class HurtRig {
    private HurtRig() { }
    static boolean extract(Player p,AvatarRenderState state,float partial){
        var c=Minecraft.getInstance();var v=MeleeActions.view(p);boolean shield=p.getOffhandItem().is(Items.SHIELD);
        boolean guard=shield&&p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.OFF_HAND&&p.getUseItem()==p.getOffhandItem();
        if(v.hurt()<=0||!Combat.compatible(p)||p.getPose()!=Pose.STANDING||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||state.scale!=1
            ||MeleeWeapon.of(p.getMainHandItem())!=MeleeWeapon.SWORD||!p.getOffhandItem().isEmpty()&&!shield||p.isUsingItem()&&!guard
            ||ParryActions.view(p).remaining()!=0||state.showCape&&state.skin.cape()!=null||p==c.player&&c.gui.screen()!=null&&!c.gui.screen().isPauseScreen())return false;
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return false;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard",equip=guard?"guard":shield?"carry":"empty",arm=p.getMainArm()==HumanoidArm.RIGHT?"R":"L";
        float angle=(Mth.wrapDegrees(v.hurtYaw()-p.getYRot())+360)%360,at=angle/90;int lo=(int)at,hi=(lo+1)%4;float mix=at-lo;
        String[] dirs={"front","right","back","left"};String a="hurt_chest_"+equip+"_"+dirs[lo]+"_"+arm,b="hurt_chest_"+equip+"_"+dirs[hi]+"_"+arm;
        var data=CombatRig.asset(variant);if(data==null||!data.clips().containsKey(a)||!data.clips().containsKey(b))return false;
        var mainItem=p.getMainArm()==HumanoidArm.RIGHT?state.rightHandItemState:state.leftHandItemState;
        var offItem=p.getMainArm()==HumanoidArm.RIGHT?state.leftHandItemState:state.rightHandItemState;if(mainItem.isEmpty()||shield&&offItem.isEmpty())return false;
        // Only a qualified source suppresses the old procedural hurt; unsupported states keep it.
        state.setData(MeleePoses.SOURCE_HURT,true);var model=CombatRigLayer.reference(state.skin.model());model.setupAnim(state);var names=Arrays.asList(data.bones());var global=new Matrix4f[names.size()];
        for(int i=0;i<global.length;i++)global[i]=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(model,names.get(i)))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
        for(var hand:HumanoidArm.values()){
            boolean main=hand==p.getMainArm();if(!main&&!shield)continue;String bone=(hand==HumanoidArm.RIGHT?"right":"left")+(main?"_sword_reference":"_shield_mount");global[names.indexOf(bone)]=CombatRig.nativeFeedbackReference(model,state,hand,!main);
        }
        // Native rendering already freezes partial; clearing it rewinds the live hurt envelope.
        float time=Math.clamp((6-v.hurt()+partial)/6F,0,1);
        int pelvis=names.indexOf("pelvis"),body=names.indexOf("body");var start=CombatRig.sample(variant,a,0);
        var delta=CombatRig.blend(delta(variant,a,time,pelvis,body),delta(variant,b,time,pelvis,body),mix);
        // Source recoil directions are global head-forward, not its side-facing idle pelvis rotation.
        var heading=new Matrix4f().translation(global[pelvis].getTranslation(new Vector3f())).rotateY((float)Math.toRadians(-Mth.wrapDegrees(p.getYRot()-state.bodyRot))).translate(start[pelvis].getTranslation(new Vector3f()).negate());
        var world=new Matrix4f(heading).mul(delta).mul(new Matrix4f(heading).invert());
        apply(data,global,world);
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,a,time,1));return true;
    }
    static void apply(CombatRig.Data data,Matrix4f[] global,Matrix4f world){
        var names=Arrays.asList(data.bones());
        for(int i=0;i<global.length;i++){
            String bone=names.get(i);if(!(bone.equals("body")||bone.equals("head")||bone.contains("arm")||bone.contains("elbow")||bone.contains("wrist")||bone.contains("palm")||bone.contains("reference")||bone.contains("mount")))continue;
            var target=new Matrix4f(world).mul(global[i]);if(bone.equals("head"))target.translationRotateScale(target.getTranslation(new Vector3f()),global[i].getUnnormalizedRotation(new Quaternionf()).normalize(),global[i].getScale(new Vector3f()));global[i]=target;
        }
    }
    private static Matrix4f delta(String variant,String name,float time,int pelvis,int body){var start=CombatRig.sample(variant,name,0);var sample=CombatRig.sample(variant,name,time);return new Matrix4f(start[pelvis]).mul(new Matrix4f(sample[pelvis]).invert()).mul(sample[body]).mul(new Matrix4f(start[body]).invert());}
}
