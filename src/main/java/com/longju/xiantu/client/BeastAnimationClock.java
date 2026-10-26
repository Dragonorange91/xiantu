package com.longju.xiantu.client;

/** Monotonic presentation time, with no animation advance during a paused world. */
public final class BeastAnimationClock {
    private BeastAnimationClock() {}
    public static double deltaSeconds(long previous, long now, boolean paused, boolean wasPaused) {
        if (previous == 0 || paused || wasPaused) return 0;
        return Math.min(.1, Math.max(0, (now - previous) / 1_000_000_000.0));
    }
}
