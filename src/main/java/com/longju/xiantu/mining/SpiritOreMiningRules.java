package com.longju.xiantu.mining;

import java.util.function.DoubleSupplier;

public final class SpiritOreMiningRules {
    private SpiritOreMiningRules() {}

    public static int experienceForOre(int oreRank) {
        return switch (oreRank) {
            case 1, 2 -> 1;
            case 3, 4 -> 2;
            case 5, 6 -> 4;
            case 7 -> 8;
            case 8 -> 16;
            default -> 0;
        };
    }

    public static double bonusChance(int pickaxeQuality) {
        return switch (Math.max(1, Math.min(5, pickaxeQuality))) {
            case 1 -> 0.15D;
            case 2 -> 0.1725D;
            case 3 -> 0.2025D;
            case 4 -> 0.24D;
            default -> 0.30D;
        };
    }

    public static int rollBonusDrops(int veinLevel, int pickaxeQuality, DoubleSupplier randomRoll) {
        int rolls = Math.max(0, Math.min(3, veinLevel));
        double chance = bonusChance(pickaxeQuality);
        int bonus = 0;
        for (int i = 0; i < rolls; i++) {
            if (randomRoll.getAsDouble() < chance) bonus++;
        }
        return bonus;
    }
}
