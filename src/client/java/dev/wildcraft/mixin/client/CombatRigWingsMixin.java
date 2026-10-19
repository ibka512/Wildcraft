package dev.wildcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Attach native wings to the qualified waist torso without replacing native wing animation. */
@Mixin(WingsLayer.class)
public abstract class CombatRigWingsMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
        at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",shift=At.Shift.AFTER))
    private void wildcraft$waistWings(PoseStack poses,SubmitNodeCollector collector,int light,HumanoidRenderState state,float yaw,float pitch,CallbackInfo ci){
        if(!(state instanceof AvatarRenderState avatar)||!state.chestEquipment.is(Items.ELYTRA))return;
        var frame=avatar.getData(CombatRigLayer.FRAME);
        if(frame==null||!(frame.clip().startsWith("equipment_")||frame.clip().startsWith("axe_equipment_")||frame.clip().startsWith("trident_equipment_")||frame.clip().startsWith("mace_equipment_")||frame.clip().startsWith("spear_equipment_")||frame.clip().startsWith("shield_equipment_")||frame.clip().startsWith("bow_equipment_")||frame.clip().startsWith("crossbow_equipment_"))||!frame.clip().contains("_waist_"))return;
        poses.mulPose(CombatRigLayer.body(frame));
    }
}
