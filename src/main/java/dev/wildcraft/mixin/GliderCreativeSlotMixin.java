package dev.wildcraft.mixin;

import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.registry.WildcraftItems;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class GliderCreativeSlotMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void wildcraft$creativeGear(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo info) {
        if (packet.slotNum() < 46) return;
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, player.level());
        var slot = GliderEquipment.slot(player.inventoryMenu);
        if (packet.slotNum() != slot.index) return;
        var stack = packet.itemStack();
        if (player.hasInfiniteMaterials() && stack.isItemEnabled(player.level().enabledFeatures())
                && (stack.isEmpty() || stack.is(WildcraftItems.PARAGLIDER) && stack.getCount() == 1)) {
            slot.setByPlayer(stack);
            player.inventoryMenu.setRemoteSlot(slot.index, stack);
            player.inventoryMenu.broadcastChanges();
        } else {
            player.inventoryMenu.sendAllDataToRemote();
        }
        info.cancel();
    }
}
