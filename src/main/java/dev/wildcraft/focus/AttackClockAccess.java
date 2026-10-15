package dev.wildcraft.focus;
/** Only advances the native weapon recovery counter; never ticks a player twice. */
public interface AttackClockAccess {int wildcraft$attackTicks();void wildcraft$attackTicks(int ticks);}
