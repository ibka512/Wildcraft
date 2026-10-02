package dev.wildcraft.mixin;

import dev.wildcraft.player.GliderSlot;
import dev.wildcraft.registry.WildcraftItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class GliderInventoryMixin extends AbstractContainerMenu {
    @Unique private int wildcraft$gliderSlot;

    protected GliderInventoryMixin(MenuType<?> type, int id) { super(type, id); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void wildcraft$equipmentSlot(Inventory inventory, boolean active, Player owner, CallbackInfo info) {
        wildcraft$gliderSlot = slots.size();
        addSlot(new GliderSlot(owner));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void wildcraft$shiftEquipment(Player player, int index, CallbackInfoReturnable<ItemStack> info) {
        Slot clicked = slots.get(index);
        ItemStack stack = clicked.getItem();
        boolean unequip = index == wildcraft$gliderSlot;
        boolean equip = index >= 9 && index < 45 && stack.is(WildcraftItems.PARAGLIDER) && !slots.get(wildcraft$gliderSlot).hasItem();
        if (!unequip && !equip) return;
        ItemStack before = stack.copy();
        boolean moved = unequip ? moveItemStackTo(stack, 9, 45, false)
                : moveItemStackTo(stack, wildcraft$gliderSlot, wildcraft$gliderSlot + 1, false);
        if (!moved) {
            info.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) clicked.setByPlayer(ItemStack.EMPTY, before);
        else clicked.setChanged();
        clicked.onTake(player, stack);
        info.setReturnValue(before);
    }
}
