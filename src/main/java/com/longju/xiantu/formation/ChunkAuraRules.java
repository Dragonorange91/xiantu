package com.longju.xiantu.formation;

/** Pure, simultaneous allocation. All vertical positions in a 16x16 chunk share a pool. */
public final class ChunkAuraRules {
    private ChunkAuraRules() {}
    public static int chunk(int blockCoordinate) { return Math.floorDiv(blockCoordinate, 16); }
    public static double rescale(double remaining, double oldCapacity, double newCapacity) {
        if (!(oldCapacity > 0) || !Double.isFinite(remaining)) return 0;
        return Math.max(0, Math.min(1, remaining / oldCapacity)) * newCapacity;
    }
    public record Allocation(double remaining, double supply, double concentration) {}
    public static Allocation allocate(double remaining, double capacity, double concentration, double demand) {
        double available = Math.max(0, Math.min(capacity, remaining));
        double supply = demand > 0 ? Math.min(1, available / demand) : 0;
        double density = concentration * (.5 + .5 * available / capacity);
        return new Allocation(Math.max(0, available - Math.max(0, demand)), supply, density);
    }
    public static double recover(double remaining, double capacity, double amount, boolean occupied) {
        return Math.min(capacity, Math.max(0, remaining) + (occupied ? 0 : Math.max(0, amount)));
    }
}
