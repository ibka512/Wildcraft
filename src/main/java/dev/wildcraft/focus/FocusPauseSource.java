package dev.wildcraft.focus;

/** Implemented only by the integrated server; common code never loads client classes. */
public interface FocusPauseSource {
    boolean wildcraft$focusPaused();
}
