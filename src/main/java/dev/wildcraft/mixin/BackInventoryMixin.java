package dev.wildcraft.mixin;

import dev.wildcraft.equipment.BackEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class BackInventoryMixin {
    @Unique private BackEquipment.Transaction wildcraft$transaction;
    @Inject(method = "clicked", at = @At("HEAD"))
    private void wildcraft$before(int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer p) wildcraft$transaction = BackEquipment.beforeTransaction(p);
    }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void wildcraft$after(int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer p && wildcraft$transaction != null) {
            BackEquipment.afterTransaction(p, wildcraft$transaction);
            wildcraft$transaction = null;
        }
    }
}
