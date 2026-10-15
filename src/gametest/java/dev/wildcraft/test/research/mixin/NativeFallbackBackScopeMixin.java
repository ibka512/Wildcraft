package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.NativeFallbackAudit;
import dev.wildcraft.client.render.BackEquipmentLayer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(BackEquipmentLayer.class)
public abstract class NativeFallbackBackScopeMixin {
 @WrapMethod(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V")
 private void back(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch,Operation<Void> original){var previous=NativeFallbackAudit.expectedBackShield;try{var items=state.getData(BackEquipmentLayer.ITEMS);NativeFallbackAudit.expectedBackShield=NativeFallbackAudit.enabled&&items!=null&&!items[1].isEmpty()?items[1]:null;original.call(poses,collector,light,state,yaw,pitch);}finally{NativeFallbackAudit.expectedBackShield=previous;}}
}
