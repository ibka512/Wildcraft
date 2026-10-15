package dev.wildcraft.client.animation;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.combat.MeleeWeapon;
import dev.wildcraft.network.MeleeView;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
/** Native rig masks; phase authority is separate from visual interpolation. */
public final class MeleePoses {
    public static final RenderStateDataKey<MeleeView> LAYER=RenderStateDataKey.create(()->"wildcraft:melee_layer");
    public static final RenderStateDataKey<Float> PARTIAL=RenderStateDataKey.create(()->"wildcraft:melee_partial");
    public static final RenderStateDataKey<Boolean> SOURCE_HURT=RenderStateDataKey.create(()->"wildcraft:source_hurt");
    private MeleePoses(){}
    public static void extract(Player p,AvatarRenderState state,float partial){state.setData(LAYER,MeleeActions.eligible(p)?MeleeActions.view(p):MeleeView.OFF);state.setData(PARTIAL,partial);}
    public static void apply(HumanoidModel<?> model,AvatarRenderState state){
        var v=state.getData(LAYER);if(v==null)return;
        var arm=state.mainArm==HumanoidArm.RIGHT?model.rightArm:model.leftArm;float sign=state.mainArm==HumanoidArm.RIGHT?1:-1;
        float t=Math.clamp((v.age()+state.getDataOrDefault(PARTIAL,0F))/Math.max(1,v.duration()),0,1);
        if(!state.isUsingItem){
            var support=state.mainArm==HumanoidArm.RIGHT?model.leftArm:model.rightArm;
            boolean longWeapon=(v.kind()==MeleeWeapon.TRIDENT||v.kind()==MeleeWeapon.SPEAR),heavy=v.kind()==MeleeWeapon.MACE;
            if((longWeapon||heavy)&&v.phase()==MeleeView.CHARGE){
                float blend=.2F+.8F*t;
                arm.xRot+=((heavy?-2.5F:-1.45F)-arm.xRot)*blend;
                arm.yRot=-sign*(heavy?.2F:.35F)*blend;arm.zRot=sign*.12F*blend;
                if(MeleeHands.freeSupport(state)){support.xRot+=((heavy?-1.8F:-.85F)-support.xRot)*blend;support.yRot=sign*.25F*blend;}
                model.body.xRot+=(heavy?-.12F:.1F)*blend;
                model.rightLeg.xRot+=.08F*blend;model.leftLeg.xRot-=.08F*blend;
            }else if(v.phase()==MeleeView.CHARGE){float blend=.2F+.8F*t;arm.xRot+=((v.axe()?-2.35F:-1.2F)-arm.xRot)*blend;arm.zRot=sign*(v.axe()?.12F:.3F)*blend;model.body.yRot-=sign*.1F*blend;}
            if((longWeapon||heavy)&&(v.phase()==MeleeView.SWING||v.phase()==MeleeView.AIR_SWING||v.phase()==MeleeView.DROP_SWING)){
                float pulse=Mth.sin(t*(float)Math.PI);
                arm.xRot-=(heavy?1.45F:.35F)*pulse;arm.yRot-=sign*(heavy?.15F:.08F)*pulse;
                if(MeleeHands.freeSupport(state))support.xRot-=(heavy?.95F:.3F)*pulse;model.body.xRot+=(heavy?.18F:.12F)*pulse;
            }else if(v.phase()==MeleeView.SWING||v.phase()==MeleeView.AIR_SWING||v.phase()==MeleeView.DROP_SWING){float pulse=Mth.sin(t*(float)Math.PI);arm.xRot-=(v.axe()?1.1F:.55F)*pulse;arm.yRot-=sign*(v.axe()?.12F:.65F)*pulse;model.body.yRot+=sign*(v.charged()?.2F:.1F)*pulse;}
        }
        if(v.phase()==MeleeView.AIR_SWING||v.phase()==MeleeView.DROP_SWING){float pulse=Mth.sin(t*(float)Math.PI);model.body.xRot+=(v.phase()==MeleeView.DROP_SWING?.2F:.08F)*pulse;model.rightLeg.xRot+=.3F*pulse;model.leftLeg.xRot+=.2F*pulse;arm.xRot-=(v.phase()==MeleeView.DROP_SWING?.6F:.2F)*pulse;}
        if(v.phase()==MeleeView.HOP){float pulse=Mth.sin(t*(float)Math.PI);model.rightLeg.xRot+=.5F*pulse;model.leftLeg.xRot-=.5F*pulse;model.body.zRot+=v.direction()==-2?0:-v.direction()*.16F*pulse;model.body.xRot+=v.direction()==-2?-.12F*pulse:0;}
        if(v.hurt()>0&&!state.getDataOrDefault(SOURCE_HURT,false)){float pulse=Mth.sin(v.hurt()/6F*(float)Math.PI);float angle=(float)Math.toRadians(Mth.wrapDegrees(v.hurtYaw()-state.bodyRot));model.body.xRot-=Math.cos(angle)*.09F*pulse;model.body.zRot-=Math.sin(angle)*.09F*pulse;}
    }
}
