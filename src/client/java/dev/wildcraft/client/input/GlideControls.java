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
    private static boolean jumpWasDown;
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
            jumpWasDown = false;
        });
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        boolean jump = client.options.keyJump.isDown();
        if (player == null || !ClientPlayNetworking.canSend(GlideInput.TYPE)) {
            observedPlayer = null;
            lastSent = null;
            requested = false;
            acknowledged = false;
            jumpWasDown = jump;
            return;
        }
        if (player != observedPlayer) {
            observedPlayer = player;
            requested = false;
            acknowledged = false;
            lastSent = null;
            jumpWasDown = jump;
        }
        var view = player.getAttached(Gliding.VIEW);
        if (view != null && view.active()) {
            acknowledged = true;
        }
        boolean available = client.gui.screen() == null && client.gui.overlay() == null;
        boolean takingItem = requested && (player.getInventory().getSelectedSlot() != selectedSlot
                || !ItemStack.matches(player.getMainHandItem(), mainHand) || !ItemStack.matches(player.getOffhandItem(), offHand)
                || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
                || client.options.keySwapOffhand.isDown() || client.options.keyDrop.isDown());
        if (!available || takingItem || !Gliding.eligible(player) || view != null && (view.blocked() || acknowledged && !view.active())) {
            requested = false;
        }
        if (available && !takingItem && jump && !jumpWasDown && Gliding.eligible(player) && (view == null || !view.blocked())) {
            requested = !requested;
            acknowledged = false;
            if (requested) {
                selectedSlot = player.getInventory().getSelectedSlot();
                mainHand = player.getMainHandItem().copy();
                offHand = player.getOffhandItem().copy();
            }
        }
        jumpWasDown = jump;
        GlideInput input = requested ? GlideInput.OPEN : GlideInput.CLOSED;
        player.setAttached(Gliding.INTENT, input);
        if (!input.equals(lastSent) || requested && player.tickCount % 5 == 0
                || !requested && view != null && view.blocked()) {
            ClientPlayNetworking.send(input);
            lastSent = input;
        }
    }
}
