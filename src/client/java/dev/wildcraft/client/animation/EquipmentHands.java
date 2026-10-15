package dev.wildcraft.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.MeleeWeapon;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** Bounded skin-arm draw/empty return; native items and current ownership are never replaced with copies. */
public final class EquipmentHands {
    private EquipmentHands() { }
    private static HumanoidArm arm(AvatarRenderState avatar,InteractionHand hand) {
        return hand==InteractionHand.MAIN_HAND?avatar.mainArm:avatar.mainArm.getOpposite();
    }
    private static boolean allowed(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands) {
        return avatar!=null&&!avatar.isUsingItem&&!avatar.isAutoSpinAttack&&!avatar.isInvisible&&!avatar.isSpectator
                &&!hands.isScoping&&hands.useItemRemainingTicks<=0&&avatar.currentSwing==null;
    }
    public static boolean active(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand,ItemStack stack) {
        if(!allowed(avatar,hands))return false;
        var s=BackTransitions.forArm(avatar,arm(avatar,hand));
        return s!=null&&s.draw()&&!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,BackTransitions.held(avatar,arm(avatar,hand)));
    }
    public static boolean stale(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand,ItemStack rendered) {
        if(!allowed(avatar,hands))return false;
        var a=arm(avatar,hand);var all=avatar.getData(BackTransitions.SETTLES);
        if(all==null||ItemStack.isSameItemSameComponents(rendered,BackTransitions.held(avatar,a)))return false;
        int owner=hand==InteractionHand.MAIN_HAND?1:2;
        for(var s:all)if(s!=null&&s.handOwner()==owner)return true;
        return false;
    }
    /** An empty hand can return from the holster even while the native equip interpolation still holds the old item. */
    public static boolean holster(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand) {
        if(!allowed(avatar,hands))return false;
        var s=BackTransitions.forArm(avatar,arm(avatar,hand));
        return s!=null&&!s.draw()&&BackTransitions.held(avatar,arm(avatar,hand)).isEmpty();
    }
    public static void place(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand,ItemStackRenderState item,ItemStack stack,PoseStack poses,float inverseHeight) {
        var a=arm(avatar,hand);var s=BackTransitions.forArm(avatar,a);float w=s.weight(),sign=a==HumanoidArm.RIGHT?1:-1;
        var grip=MeleeHands.anchor(item,stack,a,false);
        poses.translate(-sign*.20F*w,.6F*Math.clamp(inverseHeight,0,1)+.16F*w,-.12F*w);
        if(stack.is(net.minecraft.world.item.Items.TRIDENT))poses.translate(-grip.x*w,-grip.y*w,-grip.z*w);
        poses.translate(grip.x,grip.y,grip.z);poses.rotateDegrees(Axis.XP,-18*w);poses.rotateDegrees(Axis.ZP,sign*12*w);poses.translate(-grip.x,-grip.y,-grip.z);
    }
    public static void submit(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand,ItemStackRenderState item,ItemStack stack,PoseStack poses,SubmitNodeCollector collector) {
        var a=arm(avatar,hand);MeleeHands.skinArm(avatar,poses,collector,a,false,MeleeHands.anchor(item,stack,a,false));
        var kind=MeleeWeapon.of(stack);
        if(hand==InteractionHand.MAIN_HAND&&(kind==MeleeWeapon.TRIDENT||kind==MeleeWeapon.SPEAR||kind==MeleeWeapon.MACE)
                &&MeleeHands.freeSupport(avatar)&&hands.offHandItem.isEmpty())
            MeleeHands.skinArm(avatar,poses,collector,a.getOpposite(),true,MeleeHands.anchor(item,stack,a,true));
    }
    public static void empty(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand,PoseStack poses,SubmitNodeCollector collector) {
        var a=arm(avatar,hand);var s=BackTransitions.forArm(avatar,a);float w=s.weight(),sign=a==HumanoidArm.RIGHT?1:-1;
        poses.pushPose();
        try {MeleeHands.skinArm(avatar,poses,collector,a,false,new Vector3f(sign*(.35F+.35F*w),-.35F+.15F*w,-.6F+.25F*w));}
        finally {poses.popPose();}
    }
}
