package dev.wildcraft.client.input;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.ClimbInput;
import dev.wildcraft.traversal.Climbing;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class ClimbControls {
    public static final KeyMapping CLIMB = WildcraftBindings.CLIMB;
    private static ClimbInput lastSent;

    private ClimbControls() {
    }

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(ClimbControls::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSent = null);
    }

    private static void tick(Minecraft client) {
        if (client.player == null || !ClientPlayNetworking.canSend(ClimbInput.TYPE)) {
            lastSent = null;
            return;
        }
        var frame = ActionInput.tickFrame();
        boolean held = frame.down(dev.wildcraft.input.Action.CLIMB);
        ClimbInput input = held ? new ClimbInput(true, frame.forward(), frame.sideways()) : ClimbInput.RELEASED;
        client.player.setAttached(Climbing.INTENT, input);
        var view = client.player.getAttached(Climbing.VIEW);
        if (!input.equals(lastSent) || held && client.player.tickCount % 5 == 0 || !held && view != null && view.blocked()) {
            ClientPlayNetworking.send(input);
            lastSent = input;
        }
    }
}
