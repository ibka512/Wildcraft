package dev.wildcraft.focus;

/** A revision detects external writes even when they set the same numeric rate. */
public interface TimeControlEpoch {
    long wildcraft$timeEpoch();
}
