package com.longju.xiantu.medicine;

public final class MedicineRules {
    private static final int[] SOIL_BASE_SECONDS = {240, 210, 180, 150, 120, 100, 80};

    private MedicineRules() {}

    public static boolean canPlant(int herbTier, boolean vanillaFarmland, int spiritSoilTier) {
        if (herbTier < 1 || herbTier > 6) return false;
        if (vanillaFarmland) return herbTier == 1;
        return spiritSoilTier >= 0 && spiritSoilTier <= 6 && spiritSoilTier >= herbTier - 1;
    }

    public static int growthSeconds(int herbTier, int spiritSoilTier) {
        if (herbTier < 1 || herbTier > 6 || spiritSoilTier < 0 || spiritSoilTier > 6) {
            throw new IllegalArgumentException("Herb or soil tier outside implemented range");
        }
        return SOIL_BASE_SECONDS[spiritSoilTier] * (1 << (herbTier - 1));
    }

    public static float wildSeedChance(int herbTier) {
        if (herbTier >= 1 && herbTier <= 2) return 0.15f;
        if (herbTier >= 3 && herbTier <= 4) return 0.10f;
        if (herbTier == 5) return 0.05f;
        if (herbTier == 6) return 0.02f;
        return 0.0f;
    }

    public static float cultivatedSeedChance(int herbTier) {
        return herbTier >= 1 && herbTier <= 4 ? 0.25f : 0.0f;
    }
}
