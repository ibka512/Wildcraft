package dev.wildcraft.client.input;

import dev.wildcraft.input.*;
import dev.wildcraft.mechanics.MachineEntity;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
import java.util.EnumMap;
import java.util.EnumSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BowItem;

/** The sole device adapter for Wildcraft. Vanilla retains ownership of its own click queues. */
public final class ActionInput {
    private static ActionTracker tracker = new ActionTracker();
    private static ActionFrame tickFrame = ActionFrame.EMPTY;
    private static LocalPlayer observedPlayer;
    private ActionInput() { }
    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(ActionInput::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> reset());
    }
    public static ActionFrame tickFrame() { return tickFrame; }
    public static boolean gameplay(Minecraft c){return available(c);}
    public static boolean takePress(Action action){return tracker.takePress(action);}
    public static boolean down(Action action) { return tracker.down(action); }
    public static void reset() {
        tracker = new ActionTracker(); tickFrame = ActionFrame.EMPTY; observedPlayer = null;
    }
    /** Called on render frames as well as client ticks; captures taps even during slow world time. */
    public static void frame(Minecraft client) {
        var physical = EnumSet.noneOf(Action.class);
        var o = client.options;
        read(physical, Action.FORWARD, o.keyUp); read(physical, Action.BACKWARD, o.keyDown);
        read(physical, Action.LEFT, o.keyLeft); read(physical, Action.RIGHT, o.keyRight);
        read(physical, Action.JUMP, o.keyJump); read(physical, Action.SNEAK, o.keyShift);
        read(physical, Action.SPRINT, o.keySprint); read(physical, Action.ATTACK, o.keyAttack);
        read(physical, Action.USE, o.keyUse); read(physical, Action.INVENTORY, o.keyInventory);
        read(physical, Action.DROP, o.keyDrop); read(physical, Action.SWAP_HAND, o.keySwapOffhand);
        for (int i = 0; i < 9; i++) read(physical, Action.values()[Action.HOTBAR_1.ordinal() + i], o.keyHotbarSlots[i]);
        WildcraftBindings.ALL.forEach((action, binding) -> read(physical, action, binding));
        if (client.player != observedPlayer) {
            reset(); observedPlayer = client.player;
            tracker.observe(physical, false);
            discardClicks();
        }
        boolean available = available(client);
        tracker.observe(physical, available);
        if (!available) discardClicks();
    }
    private static void discardClicks() {
        WildcraftBindings.ALL.values().forEach(binding -> { while (binding.consumeClick()) { /* UI owns these presses. */ } });
    }
    private static void read(EnumSet<Action> physical, Action action, KeyMapping binding) {
        if (binding.isDown()) physical.add(action);
    }
    private static boolean available(Minecraft c) {
        return c.player != null && c.player.isAlive() && !c.player.isSpectator()
                && c.gui.screen() == null && c.gui.overlay() == null && !c.isPaused() && c.isWindowActive();
    }
    private static InputContext context(Minecraft c) {
        if (c.gui.screen() != null || c.gui.overlay() != null) return InputContext.GUI;
        if (!available(c)) return InputContext.INACTIVE;
        var p = c.player;
        if (p.getVehicle() instanceof MachineEntity) return InputContext.MECHANISM;
        if (p.isPassenger()) return InputContext.RIDING;
        if (Gliding.active(p)) return InputContext.GLIDING;
        if (Climbing.active(p)) return InputContext.CLIMBING;
        if (p.isUsingItem() && p.getUseItem().getItem() instanceof BowItem) return InputContext.AIMING;
        return dev.wildcraft.combat.Combat.locked(p) ? InputContext.LOCKED_COMBAT : InputContext.NORMAL;
    }
    private static void tick(Minecraft client) {
        dev.wildcraft.client.combat.SkillControls.startTick();
        frame(client);
        // Deliver skill edges before the ordinary tick snapshot drains them.
        dev.wildcraft.client.combat.SkillControls.frame(client);
        var clicks = new EnumMap<Action, Integer>(Action.class);
        WildcraftBindings.ALL.forEach((action, binding) -> {
            int count = 0;
            while (binding.consumeClick()) count = Math.min(16, count + 1);
            if (count > 0) clicks.put(action, count);
        });
        tickFrame = tracker.drain(context(client), clicks);
    }
}
