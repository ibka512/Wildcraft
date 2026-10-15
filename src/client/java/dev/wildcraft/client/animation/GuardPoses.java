package dev.wildcraft.client.animation;
import dev.wildcraft.combat.*;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
/** Small shield pulse and enemy arm phases on native bones, including native armor. */
public final class GuardPoses {
    public record Shield(float pulse,HumanoidArm arm){}
    public static final RenderStateDataKey<Shield> SHIELD=RenderStateDataKey.create(()->"wildcraft:guard_pose");
    public static final RenderStateDataKey<dev.wildcraft.network.ThreatView> THREAT=RenderStateDataKey.create(()->"wildcraft:threat_pose");
    public static final RenderStateDataKey<Float> PARTIAL=RenderStateDataKey.create(()->"wildcraft:threat_partial");
    private GuardPoses(){}
    public static void extract(Player p,AvatarRenderState state,float partial){
        state.setData(SHIELD,null);var v=ParryActions.view(p);
        if(v.remaining()>0&&Combat.compatible(p)&&p.isUsingItem()&&p.getUseItem().is(Items.SHIELD)){
            float pulse=(float)Math.sin(Math.PI*Math.clamp((ParryActions.PULSE-v.remaining()+partial)/ParryActions.PULSE,0,1));
            state.setData(SHIELD,new Shield(pulse,p.getUsedItemHand()==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite()));
        }
    }
    public static void applyShield(HumanoidModel<?> model,AvatarRenderState state){var v=state.getData(SHIELD);if(v==null||state.getData(dev.wildcraft.client.animation.rig.GuardRig.PULSE)!=null)return;var arm=v.arm()==HumanoidArm.RIGHT?model.rightArm:model.leftArm;arm.xRot-=.24F*v.pulse();arm.z-=1.2F*v.pulse();}
    public static void extractThreat(LivingEntity e,ZombieRenderState state,float partial){state.setData(THREAT,ThreatActions.supported(e)?ThreatActions.view(e):null);state.setData(PARTIAL,partial);}
    public static void applyThreat(HumanoidModel<?> model,ZombieRenderState state){
        var v=state.getData(THREAT);if(v==null||v.phase()==0)return;var partial=state.getData(PARTIAL);float t=Math.clamp((v.age()+(partial==null?0:partial))/Math.max(1,v.duration()),0,1);
        if(v.phase()==dev.wildcraft.network.ThreatView.WINDUP){model.rightArm.xRot=-.35F-.45F*t;model.leftArm.xRot=-.35F-.45F*t;model.rightArm.yRot=-.35F;model.leftArm.yRot=.35F;model.body.xRot=-.06F*t;}
        else if(v.phase()==dev.wildcraft.network.ThreatView.SWING){model.rightArm.xRot=-1.65F+.3F*t;model.leftArm.xRot=-1.65F+.3F*t;model.body.xRot=.12F*(1-t);}
        else {model.body.xRot=-.18F*(1-t);model.rightArm.xRot=-.3F;model.leftArm.xRot=-.3F;}
    }
}
