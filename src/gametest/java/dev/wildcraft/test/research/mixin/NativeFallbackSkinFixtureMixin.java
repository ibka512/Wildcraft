package dev.wildcraft.test.research.mixin;
import dev.wildcraft.test.NativeFallbackAudit;
import dev.wildcraft.test.RigAudit;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Test-only actual native renderer skin/model selection; no production skin replacement. */
@Mixin(AbstractClientPlayer.class)
public abstract class NativeFallbackSkinFixtureMixin {
 @Inject(method="getSkin",at=@At("HEAD"),cancellable=true)
 private void skin(CallbackInfoReturnable<PlayerSkin> ci){if(NativeFallbackAudit.enabled&&RigAudit.fixture!=null)ci.setReturnValue(RigAudit.fixture);}
}
