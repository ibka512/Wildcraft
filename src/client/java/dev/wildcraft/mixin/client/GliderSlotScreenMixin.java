package dev.wildcraft.mixin.client;

import dev.wildcraft.player.GliderSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class GliderSlotScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    protected GliderSlotScreenMixin(InventoryMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void wildcraft$slotBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo info) {
        int x = leftPos + GliderSlot.X - 1;
        int y = topPos + GliderSlot.Y - 1;
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void wildcraft$slotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo info) {
        int x = leftPos + GliderSlot.X;
        int y = topPos + GliderSlot.Y;
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16 && !menu.slots.stream()
                .filter(slot -> slot instanceof GliderSlot).findFirst().orElseThrow().hasItem()) {
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, Component.translatable("slot.wildcraft.paraglider"), mouseX, mouseY);
        }
    }
}
