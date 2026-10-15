package dev.wildcraft.client.animation.rig;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Actual bounded native HOP selects art; no root movement, grounded-contact forcing or new defense. */
public final class HopRig {
    private HopRig(){}
    static boolean extract(Player p,AvatarRenderState state,float partial){
        var c=Minecraft.getInstance();var v=MeleeActions.view(p);
        if(v.phase()!=MeleeView.HOP||!Combat.compatible(p)||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||state.scale!=1||p.isUsingItem()||v.hurt()!=0||ParryActions.view(p).remaining()!=0
            ||MeleeWeapon.of(p.getMainHandItem())!=MeleeWeapon.SWORD||!p.getOffhandItem().isEmpty()&&!p.getOffhandItem().is(Items.SHIELD)||state.showCape&&state.skin.cape()!=null
            ||p==c.player&&c.gui.screen()!=null&&!c.gui.screen().isPauseScreen())return false;
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return false;
        String direction=switch(v.direction()){case 1->"left";case -1->"right";case -2->"back";default->null;};if(direction==null)return false;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);if(data==null)return false;
        String name="dodge_"+direction+"_"+(p.getOffhandItem().isEmpty()?"empty":"carry")+"_"+(p.getMainArm()==HumanoidArm.RIGHT?"R":"L");if(!data.clips().containsKey(name))return false;
        float time=Math.clamp((v.age()+(c.isPaused()?0:partial))/Math.max(1,v.duration()),0,1);var global=CombatRig.sample(variant,name,time);
        var reference=CombatRigLayer.reference(state.skin.model());reference.setupAnim(state);int head=Arrays.asList(data.bones()).indexOf("head");
        var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(reference.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();
        global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,name,time,1));return true;
    }
}
