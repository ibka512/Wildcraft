package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.EquipmentHands;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.test.EquipmentAudit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EquipmentHands.class)
public abstract class EquipmentRenderAuditMixin {
    @Inject(method="submit",at=@At("TAIL"))
    private static void wildcraft$draw(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand,ItemStackRenderState item,ItemStack stack,PoseStack poses,SubmitNodeCollector collector,CallbackInfo ci){if(EquipmentAudit.enabled){var s=BackTransitions.forArm(avatar,hand==InteractionHand.MAIN_HAND?avatar.mainArm:avatar.mainArm.getOpposite());EquipmentAudit.draw[s.category()]++;}}
    @Inject(method="empty",at=@At("TAIL"))
    private static void wildcraft$return(AvatarRenderState avatar,FirstPersonHandsAndItemsRenderState hands,InteractionHand hand,PoseStack poses,SubmitNodeCollector collector,CallbackInfo ci){if(EquipmentAudit.enabled){var s=BackTransitions.forArm(avatar,hand==InteractionHand.MAIN_HAND?avatar.mainArm:avatar.mainArm.getOpposite());EquipmentAudit.holster[s.category()]++;}}
}
