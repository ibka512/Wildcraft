package dev.wildcraft.test.industry.mixin;

import dev.wildcraft.test.industry.NativeTraversalProbe;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read final production extraction; never changes the ordinary hand states. */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class NativeTraversalHandsMixin {
    @Inject(method="extractRenderState",at=@At("RETURN"),order=2000)
    private void wildcraft$observeNativeHands(LocalPlayer p,float partialTicks,FirstPersonHandsAndItemsRenderState state,CallbackInfo ci){NativeTraversalProbe.observeHands(p,state);}
}
