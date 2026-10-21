package dev.wildcraft.client.input;

import dev.wildcraft.network.GlideInput;
import dev.wildcraft.traversal.Gliding;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/** A fresh airborne jump press toggles; a held ground jump never opens the canopy. */
public final class GlideControls {
    private static LocalPlayer observedPlayer;
    private static boolean requested;
    private static boolean acknowledged;
    private static GlideInput lastSent;
    private static int selectedSlot;
    private static ItemStack mainHand = ItemStack.EMPTY;
    private static ItemStack offHand = ItemStack.EMPTY;

    private GlideControls() {
    }

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(GlideControls::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            observedPlayer = null;
            lastSent = null;
            requested = false;
            acknowledged = false;
        });
    }

    /** Native durability synchronization changes state of the held item, not which item was taken out. */
    private static boolean sameCarriedItem(ItemStack current, ItemStack opened) {
        if (ItemStack.matches(current, opened)) return true;
        if (current.getCount() != opened.getCount() || !ItemStack.isSameItem(current, opened)) return false;
        var a = current.copy();
        var b = opened.copy();
        a.remove(net.minecraft.core.component.DataComponents.DAMAGE);
        b.remove(net.minecraft.core.component.DataComponents.DAMAGE);
        return ItemStack.matches(a, b);
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        var frame = ActionInput.tickFrame();
        if (player == null || !ClientPlayNetworking.canSend(GlideInput.TYPE)) {
            observedPlayer = null;
            lastSent = null;
            requested = false;
            acknowledged = false;
            return;
        }
        if (player != observedPlayer) {
            observedPlayer = player;
            requested = false;
            acknowledged = false;
            lastSent = null;
        }
        var view = player.getAttached(Gliding.VIEW);
        if (view != null && view.active()) {
            acknowledged = true;
        }
        boolean available = frame.gameplay();
        boolean takingItem = requested && (player.getInventory().getSelectedSlot() != selectedSlot
                || !sameCarriedItem(player.getMainHandItem(), mainHand) || !sameCarriedItem(player.getOffhandItem(), offHand)
                || frame.triggered(dev.wildcraft.input.Action.USE) || frame.triggered(dev.wildcraft.input.Action.ATTACK)
                || frame.triggered(dev.wildcraft.input.Action.DEFEND) || frame.triggered(dev.wildcraft.input.Action.SWAP_HAND) || frame.triggered(dev.wildcraft.input.Action.DROP));
        if (!available || takingItem || !Gliding.eligible(player) || view != null && (view.blocked() || acknowledged && !view.active())) {
            requested = false;
        }
        if (available && !takingItem && frame.fresh(dev.wildcraft.input.Action.JUMP) && Gliding.eligible(player) && (view == null || !view.blocked())) {
            requested = !requested;
            acknowledged = false;
            if (requested) {
                selectedSlot = player.getInventory().getSelectedSlot();
                mainHand = player.getMainHandItem().copy();
                offHand = player.getOffhandItem().copy();
            }
        }
        GlideInput input = requested ? GlideInput.OPEN : GlideInput.CLOSED;
        player.setAttached(Gliding.INTENT, input);
        if (!input.equals(lastSent) || requested && player.tickCount % 5 == 0
                || !requested && view != null && view.blocked()) {
            ClientPlayNetworking.send(input);
            lastSent = input;
        }
    }
}
