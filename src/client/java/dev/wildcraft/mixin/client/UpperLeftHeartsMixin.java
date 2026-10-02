package dev.wildcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildcraft.client.hud.StaminaHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Relocates the native hearts, preserving damage blink, poison, absorption and hardcore sprites. */
@Mixin(Hud.class)
public abstract class UpperLeftHeartsMixin {
    @WrapOperation(method = "extractPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractHearts(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"))
    private void wildcraft$hearts(Hud hud, GuiGraphicsExtractor g, Player p, int x, int y, int rowHeight, int regenerationIndex,
                                 float maxHealth, int current, int old, int absorption, boolean blink, Operation<Void> original) {
        int rows = Math.max(1, (int)Math.ceil((Math.ceil(maxHealth / 2.0) + Math.ceil(absorption / 2.0)) / 10.0));
        int bottom = 12 + (rows - 1) * rowHeight;
        StaminaHud.setHeartBottom(bottom + 9);
        original.call(hud, g, p, 12, bottom, rowHeight, regenerationIndex, maxHealth, current, old, absorption, blink);
    }
}
