package dev.wildcraft.test.research.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observe actual source parent before production's native item redirect; no mutation. */
@Mixin(ItemInHandLayer.class)
public abstract class TridentHandRootAuditMixin {
 @Inject(method="submitArmWithItem",at=@At("HEAD"),order=900)
 private void root(ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo ci){if(state instanceof AvatarRenderState avatar){if(dev.wildcraft.combat.MeleeWeapon.of(stack)==dev.wildcraft.combat.MeleeWeapon.SWORD){dev.wildcraft.test.JumpAudit.handRoot(avatar,item,arm,poses);dev.wildcraft.test.SwordWaistAudit.handRoot(avatar,item,arm,poses);}if(dev.wildcraft.combat.MeleeWeapon.of(stack)==dev.wildcraft.combat.MeleeWeapon.AXE)dev.wildcraft.test.SwordWaistAudit.handRoot(avatar,item,arm,poses);if(stack.is(Items.CROSSBOW))dev.wildcraft.test.CrossbowTransitionAudit.handRoot(avatar,item,arm,poses);if(stack.is(Items.MACE)){dev.wildcraft.test.MaceTransitionAudit.handRoot(avatar,item,arm,poses);dev.wildcraft.test.GroundMaceAudit.handRoot(avatar,item,arm,poses);dev.wildcraft.test.AirMaceAudit.handRoot(avatar,item,arm,poses);}if(stack.is(Items.TRIDENT)){dev.wildcraft.test.TridentTransitionAudit.handRoot(avatar,item,arm,poses);dev.wildcraft.test.GroundTridentAudit.handRoot(avatar,item,arm,poses);dev.wildcraft.test.AirTridentAudit.handRoot(avatar,item,arm,poses);}if(dev.wildcraft.combat.MeleeWeapon.of(stack)==dev.wildcraft.combat.MeleeWeapon.SPEAR){dev.wildcraft.test.SpearTransitionAudit.handRoot(avatar,item,arm,poses);dev.wildcraft.test.GroundSpearAudit.handRoot(avatar,item,arm,poses);dev.wildcraft.test.AirSpearAudit.handRoot(avatar,item,arm,poses);}}}
}
