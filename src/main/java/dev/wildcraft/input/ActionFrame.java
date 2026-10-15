package dev.wildcraft.input;

import java.util.Map;
import java.util.Set;

/** Immutable tick delivery. Several render-frame edges can coexist in one tick. */
public record ActionFrame(InputContext context, Set<Action> held, Set<Action> pressed,
                          Set<Action> released, Map<Action, Integer> clicks) {
    public static final ActionFrame EMPTY = new ActionFrame(InputContext.INACTIVE, Set.of(), Set.of(), Set.of(), Map.of());
    public ActionFrame {
        held = Set.copyOf(held);
        pressed = Set.copyOf(pressed);
        released = Set.copyOf(released);
        clicks = Map.copyOf(clicks);
    }
    public boolean down(Action action) { return held.contains(action); }
    public boolean triggered(Action action) { return down(action) || fresh(action); }
    public boolean fresh(Action action) { return pressed.contains(action); }
    public int clicks(Action action) { return clicks.getOrDefault(action, 0); }
    public byte forward() { return (byte) ((down(Action.FORWARD) ? 1 : 0) - (down(Action.BACKWARD) ? 1 : 0)); }
    public byte sideways() { return (byte) ((down(Action.LEFT) ? 1 : 0) - (down(Action.RIGHT) ? 1 : 0)); }
    public boolean gameplay() { return context != InputContext.GUI && context != InputContext.INACTIVE; }
}
