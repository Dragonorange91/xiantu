package com.longju.xiantu.wood;

public final class SpiritWoodWorldgenRules {
    public static double chance(SpiritWoodType type, boolean vanillaForest) {
        if (type.rank() == 1) return type.chance;
        return type.chance * multiplier(type, vanillaForest);
    }

    public static double multiplier(SpiritWoodType type, boolean vanillaForest) {
        return vanillaForest && type.rank() > 1 ? 1.5D : 1.0D;
    }

    private SpiritWoodWorldgenRules() {}
}
