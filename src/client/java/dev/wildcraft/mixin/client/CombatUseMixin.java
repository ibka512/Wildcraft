package dev.wildcraft.mixin.client;

import dev.wildcraft.client.combat.CombatControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The native release path owns right-click use; separate defense retains its own shield use. */
@Mixin(Minecraft.class)
public abstract class CombatUseMixin {
    @Redirect(method="handleKeybinds",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private void wildcraft$retainDefense(MultiPlayerGameMode mode,Player p){if(!CombatControls.ownsDefense())mode.releaseUsingItem(p);}
}
