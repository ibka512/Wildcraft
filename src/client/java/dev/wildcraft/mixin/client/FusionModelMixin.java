package dev.wildcraft.mixin.client;
import dev.wildcraft.client.fuse.FusionItemRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemModelResolver.class) public abstract class FusionModelMixin{
 @Inject(method="updateForTopItem",at=@At("TAIL")) private void wildcraft$fusedLayers(ItemStackRenderState state,ItemStack item,ItemDisplayContext context,Level level,ItemOwner owner,int seed,CallbackInfo ci){FusionItemRenderer.append((ItemModelResolver)(Object)this,state,item,context,level,owner,seed);}
}
