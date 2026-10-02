package dev.wildcraft.mixin;

import dev.wildcraft.player.PlayerStamina;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Record intermediate peaks even if levels are gained and spent in the same tick. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerExperienceMixin {
    @Inject(method = {"setExperienceLevels", "giveExperienceLevels", "giveExperiencePoints"}, at = @At("TAIL"))
    private void wildcraft$experienceChanged(int amount, CallbackInfo info) {
        PlayerStamina.experienceChanged((ServerPlayer) (Object) this);
    }
}
