package dev.wildcraft.mixin.client;

import dev.wildcraft.player.GliderSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class GliderCreativeScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private static CreativeModeTab selectedTab;

    protected GliderCreativeScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyArgs(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void wildcraft$creativeGearPosition(Args args) {
        if (args.get(0) instanceof GliderSlot) {
            args.set(2, 152);
            args.set(3, 20);
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void wildcraft$creativeGearBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo info) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY) return;
        int x = leftPos + 151;
        int y = topPos + 19;
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }
}
