package dev.wildcraft.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.combat.MeleeWeapon;
import dev.wildcraft.network.MeleeView;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import dev.wildcraft.mixin.client.FusionLayerAccessor;
import dev.wildcraft.mixin.client.FusionStateAccessor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Skin arms sharing the native item's final hand transform. These models never own gameplay. */
public final class MeleeHands {
    private static final PlayerModel STANDARD=model(false),SLIM=model(true);
    private MeleeHands() { }
    private static PlayerModel model(boolean slim){return new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);}
    public static boolean active(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand,ItemStack item){
        if(avatar==null||hand!=InteractionHand.MAIN_HAND||hands.isScoping||avatar.isUsingItem||hands.useItemRemainingTicks>0||avatar.isAutoSpinAttack||avatar.isSpectator)return false;
        var v=avatar.getData(MeleePoses.LAYER);return v!=null&&MeleeWeapon.of(item)==v.kind()&&(v.phase()==MeleeView.CHARGE||v.phase()==MeleeView.SWING||v.phase()==MeleeView.AIR_SWING||v.phase()==MeleeView.DROP_SWING);
    }
    public static boolean support(MeleeView v){return v.kind()==MeleeWeapon.TRIDENT||v.kind()==MeleeWeapon.SPEAR||v.kind()==MeleeWeapon.MACE||v.axe()&&(v.phase()==MeleeView.CHARGE||v.charged());}
    public static boolean freeSupport(AvatarRenderState avatar){return (avatar.mainArm==HumanoidArm.RIGHT?avatar.leftHandItemStack:avatar.rightHandItemStack).isEmpty();}
    public static void submit(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,PoseStack poses,SubmitNodeCollector collector){
        if(avatar.isInvisible||avatar.isSpectator)return;
        var v=avatar.getData(MeleePoses.LAYER);if(v==null)return;
        boolean slim=avatar.skin.model()==PlayerModelType.SLIM;PlayerModel model=slim?SLIM:STANDARD;
        // Own models only. Native setup also applies sleeve visibility; the shared avatar rig is untouched.
        model.setupAnim(avatar);
        submitArm(model,avatar,poses,collector,avatar.mainArm,false,anchor(avatar,hands,false));
        if(support(v)&&freeSupport(avatar)&&hands.offHandItem.isEmpty())submitArm(model,avatar,poses,collector,avatar.mainArm.getOpposite(),true,anchor(avatar,hands,true));
    }
    /** The shaft/hilt anchor uses the same authored transform as the native item, including special tridents. */
    public static Vector3f anchor(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,boolean support){
        return anchor(hands.mainHandRenderState,hands.mainHandItem,avatar.mainArm,support);
    }
    public static Vector3f anchor(net.minecraft.client.renderer.item.ItemStackRenderState item,ItemStack stack,HumanoidArm arm,boolean support){
        var kind=MeleeWeapon.of(stack);
        Vector3f point=kind==MeleeWeapon.TRIDENT?new Vector3f(0,support?.70F:1.10F,0):kind==MeleeWeapon.SPEAR?new Vector3f(support?.53F:.70F,support?.47F:.30F,.5F):kind!=null?new Vector3f(support?.35F:.23F,support?.35F:.23F,.5F):stack.is(net.minecraft.world.item.Items.SHIELD)?new Vector3f(0,.15F,0):new Vector3f(.5F,.5F,.5F);
        var layers=((FusionStateAccessor)item).wildcraft$layers();
        if(layers.length==0||layers[0]==null)return new Vector3f();
        var layer=(FusionLayerAccessor)layers[0];var nativePose=new PoseStack();
        layer.wildcraft$transform().apply(arm==HumanoidArm.LEFT,nativePose.last());nativePose.mulPose(layer.wildcraft$local());
        return nativePose.last().pose().transformPosition(point);
    }
    /** Rotate about the actual grip instead of the camera origin; the weapon and hands move together. */
    public static void place(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,PoseStack poses,float partial){
        var v=avatar.getData(MeleePoses.LAYER);float sign=avatar.mainArm==HumanoidArm.RIGHT?1:-1;
        float t=Math.clamp((v.age()+partial)/Math.max(1,v.duration()),0,1);float pulse=(float)Math.sin(t*Math.PI);var grip=anchor(avatar,hands,false);
        poses.translate(-sign*.22F,.18F,-.14F);
        if(v.phase()==MeleeView.CHARGE)poses.rotateDegrees(com.mojang.math.Axis.XP,(v.kind()==MeleeWeapon.MACE?-35:v.kind()==MeleeWeapon.SPEAR||v.kind()==MeleeWeapon.TRIDENT?-8:v.axe()?-28:-18)*t);
        else {poses.translate(0,-.05F*pulse,(v.kind()==MeleeWeapon.SPEAR||v.kind()==MeleeWeapon.TRIDENT?-.18F:-.06F)*pulse);poses.rotateDegrees(com.mojang.math.Axis.XP,(v.phase()==MeleeView.DROP_SWING?18:8)*pulse);}
        poses.scale(.85F,.85F,.85F);poses.translate(-grip.x,-grip.y,-grip.z);
    }
    private static void submitArm(PlayerModel model,AvatarRenderState avatar,PoseStack poses,SubmitNodeCollector collector,HumanoidArm arm,boolean support,Vector3f grip){
        renderArm(model,avatar,poses,collector,arm,support,grip);
    }
    /** Shared geometry only; equipment transitions do not inherit an attack phase or attack audit. */
    public static void skinArm(AvatarRenderState avatar,PoseStack poses,SubmitNodeCollector collector,HumanoidArm arm,boolean support,Vector3f grip){
        if(avatar.isInvisible||avatar.isSpectator)return;
        PlayerModel model=avatar.skin.model()==PlayerModelType.SLIM?SLIM:STANDARD;
        // Update only the submitted arm: a second hand must not reset the first hand's queued pose.
        var part=arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm;part.visible=true;
        part.getChild(arm==HumanoidArm.RIGHT?"right_sleeve":"left_sleeve").visible=arm==HumanoidArm.RIGHT?avatar.showRightSleeve:avatar.showLeftSleeve;
        renderArm(model,avatar,poses,collector,arm,support,grip);
    }
    private static void renderArm(PlayerModel model,AvatarRenderState avatar,PoseStack poses,SubmitNodeCollector collector,HumanoidArm arm,boolean support,Vector3f grip){
        var part=arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm;
        part.x=part.y=part.z=part.xRot=part.yRot=part.zRot=0;
        float sign=(support?avatar.mainArm:arm)==HumanoidArm.RIGHT?1:-1;
        var direction=new Vector3f(sign*(support?.32F:-.16F),.5F,-.65F).normalize();
        poses.pushPose();
        poses.translate(grip.x,grip.y,grip.z);poses.rotate(new Quaternionf().rotationTo(new Vector3f(0,1,0),direction));
        // Palm center is inside the final four pixels of the actual standard/slim skin arm.
        float center=arm==HumanoidArm.RIGHT?(avatar.skin.model()==PlayerModelType.SLIM?-.5F:-1F):(avatar.skin.model()==PlayerModelType.SLIM?.5F:1F);
        poses.translate(-center/16F,-8.5F/16F,0);
        collector.submitModelPart(part,poses,RenderTypes.entityTranslucent(avatar.skin.body().texturePath()),avatar.lightCoords,OverlayTexture.NO_OVERLAY,null);
        poses.popPose();
    }
}
