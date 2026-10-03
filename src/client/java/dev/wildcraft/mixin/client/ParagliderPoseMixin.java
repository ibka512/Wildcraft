package dev.wildcraft.mixin.client;

import dev.wildcraft.client.render.ParagliderLayer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class ParagliderPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void wildcraft$holdCanopy(AvatarRenderState state, CallbackInfo info) {
        dev.wildcraft.client.render.CharacterPoses.apply((PlayerModel) (Object) this, state);
    }
}
