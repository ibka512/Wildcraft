package dev.wildcraft.test.industry.mixin;
import dev.wildcraft.test.industry.NativeAirFocusProbe;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Drive a fresh jump only after the player's original native use-release has actually run. */
@Mixin(MultiPlayerGameMode.class)
public abstract class NativeAirFocusReleaseMixin {
    @Inject(method="releaseUsingItem",at=@At("RETURN"))
    private void wildcraft$jumpAfterNativeRelease(Player p,CallbackInfo ci){NativeAirFocusProbe.afterNativeBowRelease();}
}
