package dev.wildcraft.input;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Holds edges across frames and quarantines inputs held while a screen owns input. */
public final class ActionTracker {
    private final EnumSet<Action> held = EnumSet.noneOf(Action.class);
    private final EnumSet<Action> pressed = EnumSet.noneOf(Action.class);
    private final EnumSet<Action> released = EnumSet.noneOf(Action.class);
    private final EnumSet<Action> blocked = EnumSet.noneOf(Action.class);

    public void observe(Set<Action> physical, boolean available) {
        blocked.retainAll(physical);
        if (!available) {
            blocked.addAll(physical);
            blocked.removeAll(Set.of(Action.FORWARD, Action.BACKWARD, Action.LEFT, Action.RIGHT, Action.SNEAK, Action.SPRINT));
        }
        var next = EnumSet.noneOf(Action.class);
        if (available) { next.addAll(physical); next.removeAll(blocked); }
        for (Action action : Action.values()) {
            if (next.contains(action) && !held.contains(action)) pressed.add(action);
            if (!next.contains(action) && held.contains(action)) released.add(action);
        }
        held.clear(); held.addAll(next);
        if (!available) pressed.clear();
    }
    public boolean takePress(Action action){return pressed.remove(action);}
    public boolean down(Action action) { return held.contains(action); }
    public ActionFrame drain(InputContext context, Map<Action, Integer> clicks) {
        var allowedClicks = new java.util.EnumMap<Action, Integer>(Action.class);
        if (context != InputContext.GUI && context != InputContext.INACTIVE)
            clicks.forEach((action, count) -> { if (!blocked.contains(action) && count > 0) allowedClicks.put(action, Math.min(16, count)); });
        var frame = new ActionFrame(context, held, pressed, released, allowedClicks);
        pressed.clear(); released.clear();
        return frame;
    }
}
