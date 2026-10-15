package dev.wildcraft.client.animation;

import dev.wildcraft.combat.Combat;
import dev.wildcraft.traversal.*;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Bounded native masks. Legs, shield arm, torso and independent aim are not a full-body clip. */
public final class CombatPoses {
    public static final RenderStateDataKey<Layer> LAYER=RenderStateDataKey.create(()->"wildcraft:combat_layer");
    public record Layer(float lock,float defense,HumanoidArm arm,boolean ready){}
    private static final Map<Player,Blend> STATES=new WeakHashMap<>();
    private static final class Blend{long time;float lock,defense,body;boolean started;HumanoidArm arm=HumanoidArm.LEFT;}
    private CombatPoses(){}
    public static void initialize(){ClientPlayConnectionEvents.DISCONNECT.register((h,c)->STATES.clear());}
    public static void extract(Player p,AvatarRenderState state){
        state.setData(LAYER,null);
        if(!p.isAlive()||p.isSpectator()||p.isInvisible()||p.isPassenger()||Gliding.active(p)||Climbing.active(p)){
            STATES.remove(p);return;
        }
        var b=STATES.computeIfAbsent(p,k->new Blend());long now=System.nanoTime();
        float dt=b.time==0?0:Math.clamp((now-b.time)/1_000_000_000F,0,.05F);b.time=now;
        if(Minecraft.getInstance().isPaused())dt=0;
        var target=Combat.target(p);boolean defend=p.isUsingItem()&&p.getUseItem().is(Items.SHIELD);
        b.lock=approach(b.lock,target==null?0:1,dt/.18F);b.defense=!defend&&p.isUsingItem()?0:approach(b.defense,defend?1:0,dt/.18F);
        if(defend)b.arm=p.getUsedItemHand()==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite();
        if(!b.started){b.body=state.bodyRot;b.started=true;}
        float desired=target==null?state.bodyRot:Combat.yawTo(p,target);
        b.body+=Math.clamp(Mth.wrapDegrees(desired-b.body),-180*dt,180*dt);
        if(b.lock>0){float old=state.bodyRot;state.bodyRot=old+Mth.wrapDegrees(b.body-old)*b.lock;state.yRot=Mth.wrapDegrees(state.yRot+old-state.bodyRot);}
        if(b.lock<=0&&b.defense<=0){if(target==null&&!defend)STATES.remove(p);return;}
        state.setData(LAYER,new Layer(b.lock,b.defense,b.arm,!p.isUsingItem()&&state.currentSwing==null&&!p.getMainHandItem().isEmpty()));
    }
    private static float approach(float value,float target,float step){return value+Math.clamp(target-value,-step,step);}
    public static void apply(HumanoidModel<?> model,AvatarRenderState state){
        var layer=state.getData(LAYER);if(layer==null)return;
        model.body.y+=.25F*layer.lock;model.rightLeg.yRot+=.09F*layer.lock;model.leftLeg.yRot-=.09F*layer.lock;
        model.rightLeg.zRot+=.025F*layer.lock;model.leftLeg.zRot-=.025F*layer.lock;
        if(layer.ready){var arm=state.mainArm==HumanoidArm.RIGHT?model.rightArm:model.leftArm;arm.xRot+=( -.35F-arm.xRot)*layer.lock*.65F;}
        if(layer.defense>0){var arm=layer.arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm;
            arm.xRot+=(-1.08F-arm.xRot)*layer.defense;
            float shieldYaw=(layer.arm==HumanoidArm.RIGHT?-.5236F:.5236F)+model.head.yRot*(1-layer.lock);
            arm.yRot+=(shieldYaw-arm.yRot)*layer.defense;model.body.yRot+=(layer.arm==HumanoidArm.RIGHT?-.08F:.08F)*layer.defense;}
        // In 26.3 sleeves, pants and jacket are native children and inherit the parent transform.
    }
}
