package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.MeleeHands;
import dev.wildcraft.combat.MeleeWeapon;
import dev.wildcraft.test.GripAudit;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MeleeHands.class)
public abstract class GripRenderAuditMixin {
    @Inject(method="submitArm",at=@At("TAIL"))
    private static void wildcraft$arm(PlayerModel model,AvatarRenderState avatar,PoseStack poses,SubmitNodeCollector collector,HumanoidArm arm,boolean support,org.joml.Vector3f grip,CallbackInfo ci){var v=avatar.getData(dev.wildcraft.client.animation.MeleePoses.LAYER);GripAudit.observe(support,v.kind(),v.phase());}
}
