package dev.wildcraft.mixin.client;
import dev.wildcraft.client.animation.rig.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Submit the same resolved native item once, including its Fuse model layers. */
@Mixin(ItemInHandLayer.class)
public abstract class CombatRigItemMixin {
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void wildcraft$grip(ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci){
        if(item.isEmpty()||!(state instanceof AvatarRenderState avatar))return;
        var frame=avatar.getData(CombatRigLayer.FRAME);if(frame==null)return;
        boolean shieldTransition=frame.clip().startsWith("shield_equipment_");
        boolean shield=(arm!=avatar.mainArm||shieldTransition)&&stack.is(net.minecraft.world.item.Items.SHIELD);
        var weapon=dev.wildcraft.combat.MeleeWeapon.of(stack);
        boolean bowUse=frame.clip().startsWith("bow_ground_")||frame.clip().startsWith("bow_air_");
        boolean bow=stack.is(net.minecraft.world.item.Items.BOW)&&(bowUse&&arm==(frame.clip().endsWith("_R")?HumanoidArm.RIGHT:HumanoidArm.LEFT)||(frame.clip().startsWith("bow_idle_")||frame.clip().startsWith("bow_equipment_"))&&arm==avatar.mainArm);
        boolean trident=weapon==dev.wildcraft.combat.MeleeWeapon.TRIDENT&&(frame.clip().startsWith("trident_equipment_")||frame.clip().startsWith("trident_attack_"))&&arm==avatar.mainArm;
        boolean spear=weapon==dev.wildcraft.combat.MeleeWeapon.SPEAR&&(frame.clip().startsWith("spear_equipment_")||frame.clip().startsWith("spear_attack_"))&&arm==avatar.mainArm;
        boolean mace=weapon==dev.wildcraft.combat.MeleeWeapon.MACE&&(frame.clip().startsWith("mace_equipment_")||frame.clip().startsWith("mace_attack_"))&&arm==avatar.mainArm;
        boolean crossbow=stack.is(net.minecraft.world.item.Items.CROSSBOW)&&frame.clip().startsWith("crossbow_equipment_")&&arm==avatar.mainArm;
        if(bowUse&&!bow)return;
        if(!shield&&!bow&&!trident&&!spear&&!mace&&!crossbow&&((arm!=avatar.mainArm&&!shieldTransition)||weapon!=dev.wildcraft.combat.MeleeWeapon.SWORD&&!(weapon==dev.wildcraft.combat.MeleeWeapon.AXE&&frame.clip().startsWith("axe_"))))return;
        pose.pushPose();pose.mulPose(CombatRig.heldPose(frame,item,arm,shield));item.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();ci.cancel();
    }
}
