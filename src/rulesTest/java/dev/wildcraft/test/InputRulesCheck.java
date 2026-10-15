package dev.wildcraft.test;

import dev.wildcraft.input.*;
import java.util.Map;
import java.util.Set;

/** Device-independent failure cases: queued taps, screen ownership, and immutable delivery. */
public final class InputRulesCheck {
    public static void main(String[] args) {
        var tracker = new ActionTracker();
        tracker.observe(Set.of(Action.JUMP), true);
        tracker.observe(Set.of(), true);
        var tap = tracker.drain(InputContext.NORMAL, Map.of());
        check(tap.triggered(Action.JUMP) && tap.fresh(Action.JUMP) && tap.released().contains(Action.JUMP) && !tap.down(Action.JUMP), "A complete sub-tick tap is retained");
        check(!tracker.drain(InputContext.NORMAL, Map.of()).fresh(Action.JUMP), "Tap delivers once");
        tracker.observe(Set.of(Action.CLIMB, Action.USE, Action.SNEAK), true);
        var held = tracker.drain(InputContext.CLIMBING, Map.of(Action.FUSE, 1));
        tracker.observe(Set.of(Action.CLIMB, Action.USE, Action.SNEAK), false);
        var gui = tracker.drain(InputContext.GUI, Map.of(Action.FUSE, 2));
        check(gui.held().isEmpty() && gui.clicks().isEmpty() && gui.released().contains(Action.CLIMB), "Opening a screen releases skills and discards queued commands");
        tracker.observe(Set.of(Action.CLIMB, Action.USE, Action.SNEAK), true);
        var resumed = tracker.drain(InputContext.NORMAL, Map.of());
        check(!resumed.down(Action.CLIMB) && !resumed.down(Action.USE), "Closing a screen does not restart held skills");
        check(resumed.down(Action.SNEAK), "Vanilla crouch modifier can stay held across a screen");
        check(held.down(Action.CLIMB) && held.clicks(Action.FUSE) == 1, "Prior delivery cannot mutate");
        tracker.observe(Set.of(), true);
        tracker.observe(Set.of(Action.CLIMB, Action.FORWARD, Action.BACKWARD, Action.LEFT, Action.RIGHT), true);
        var opposing = tracker.drain(InputContext.NORMAL, Map.of());
        check(opposing.fresh(Action.CLIMB) && opposing.forward() == 0 && opposing.sideways() == 0, "Fresh grip works and opposing directions cancel");
        tracker.observe(Set.of(Action.MACHINE_TOGGLE), false);
        tracker.observe(Set.of(Action.MACHINE_TOGGLE), true);
        check(tracker.drain(InputContext.MECHANISM, Map.of(Action.MACHINE_TOGGLE, 1)).clicks().isEmpty(), "Held blocked command cannot reappear via native click queue");
        tracker.observe(Set.of(), true);
        tracker.observe(Set.of(Action.MACHINE_TOGGLE), true);
        check(tracker.drain(InputContext.MECHANISM, Map.of(Action.MACHINE_TOGGLE, 2)).clicks(Action.MACHINE_TOGGLE) == 2, "Distinct native commands are retained");
        for (int n = 0; n < 1000; n++) {
            tracker.observe(Set.of(Action.ATTACK), true); tracker.observe(Set.of(), true);
            var frame = tracker.drain(InputContext.NORMAL, Map.of());
            check(frame.fresh(Action.ATTACK) && frame.released().contains(Action.ATTACK), "Repeated frame taps are retained independently");
        }
        System.out.println("PASS: sub-tick edges, GUI/focus quarantine, modifiers, immutable snapshots, native command counts and repeated taps");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
