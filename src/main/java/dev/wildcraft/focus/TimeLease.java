package dev.wildcraft.focus;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;

/** Owns one temporary world-rate write, preserving subsequent external control. */
public final class TimeLease implements AutoCloseable {
    private static final java.util.Map<ServerTickRateManager, TimeLease> ACTIVE = new java.util.IdentityHashMap<>();
    private final ServerTickRateManager manager;
    private final float baseline;
    private final float ownedRate;
    private final long epoch;
    private boolean closed;

    private TimeLease(ServerTickRateManager manager, float rate) {
        this.manager = manager;
        baseline = manager.tickrate();
        ownedRate = rate;
        manager.setTickRate(rate);
        epoch = ((TimeControlEpoch) manager).wildcraft$timeEpoch();
    }

    public static TimeLease acquire(MinecraftServer server, float rate) {
        var manager = server.tickRateManager();
        if (!server.isSingleplayer() || server.isDedicatedServer() || server.isPublished()
                || server.getPlayerList().getPlayerCount() != 1 || manager.isFrozen()
                || manager.isSprinting() || manager.isSteppingForward() || ACTIVE.containsKey(manager) || !Float.isFinite(rate) || rate < 5 || rate > 20 || manager.tickrate() <= rate) {
            return null;
        }
        TimeLease lease = new TimeLease(manager, rate);
        ACTIVE.put(manager, lease);
        return lease;
    }

    public boolean ownsControl() {
        return !closed && manager.tickrate() == ownedRate && !manager.isFrozen() && !manager.isSprinting() && !manager.isSteppingForward()
                && ((TimeControlEpoch) manager).wildcraft$timeEpoch() == epoch;
    }

    @Override
    public void close() {
        // Freeze/sprint ownership is separate: undo only our rate, preserve their mode.
        if (!closed && manager.tickrate() == ownedRate && ((TimeControlEpoch) manager).wildcraft$timeEpoch() == epoch) manager.setTickRate(baseline);
        ACTIVE.remove(manager, this);
        closed = true;
    }
}
