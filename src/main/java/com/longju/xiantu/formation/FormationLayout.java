package com.longju.xiantu.formation;

public final class FormationLayout {
    private FormationLayout() {}

    public static int outerRadius(int tier) {
        return switch (tier) {
            case 1 -> 4;
            case 2 -> 7;
            case 3 -> 10;
            default -> 0;
        };
    }

    public static int innerRadius(int tier) {
        return switch (tier) {
            case 1 -> 2;
            case 2 -> 5;
            case 3 -> 8;
            default -> 0;
        };
    }

    public static int baseRingTierAt(int tier, int x, int z) {
        for (int ringTier = 1; ringTier <= tier; ringTier++) {
            if (inRing(outerRadius(ringTier), x, z)) return ringTier;
        }
        return 0;
    }

    public static int runeRingTierAt(int tier, int x, int z) {
        for (int ringTier = 1; ringTier <= tier; ringTier++) {
            if (inRing(innerRadius(ringTier), x, z)) return ringTier;
        }
        return 0;
    }

    private static boolean inRing(int radius, int x, int z) {
        int distance = x * x + z * z;
        return radius > 0 && distance > (radius - 1) * (radius - 1) && distance <= radius * radius;
    }

    public static boolean isBaseRing(int tier, int x, int z) { return baseRingTierAt(tier, x, z) != 0; }

    public static boolean isRuneRing(int tier, int x, int z) {
        return runeRingTierAt(tier, x, z) != 0;
    }

    public static boolean isBaseRing(int x, int z) { return isBaseRing(1, x, z); }
    public static boolean isRuneRing(int x, int z) { return isRuneRing(1, x, z); }

    public static int count(boolean baseRing) { return count(1, baseRing); }

    public static int count(int tier, boolean baseRing) {
        int radius = outerRadius(tier);
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (baseRing ? isBaseRing(tier, x, z) : isRuneRing(tier, x, z)) count++;
            }
        }
        return count;
    }

    public static int countStage(int tier, boolean baseRing) {
        int radius = baseRing ? outerRadius(tier) : innerRadius(tier);
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (inRing(radius, x, z)) count++;
            }
        }
        return count;
    }
}
