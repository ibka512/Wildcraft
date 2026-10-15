package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.NativeFallbackAudit;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ItemInHandLayer.class)
public abstract class NativeFallbackItemScopeMixin {
 @WrapMethod(method="submitArmWithItem")
 private void scope(ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack poses,SubmitNodeCollector collector,int light,Operation<Void> original){
  int previous=NativeFallbackAudit.kind;
  try{NativeFallbackAudit.kind=NativeFallbackAudit.enabled&&state instanceof AvatarRenderState a&&a.getData(CombatRigLayer.FRAME)==null&&!item.isEmpty()?(stack.is(Items.BOW)?1:stack.is(Items.TORCH)?2:stack.is(Items.SHIELD)?3:0):0;original.call(state,item,stack,arm,poses,collector,light);}
  finally{NativeFallbackAudit.kind=previous;}
 }
}
