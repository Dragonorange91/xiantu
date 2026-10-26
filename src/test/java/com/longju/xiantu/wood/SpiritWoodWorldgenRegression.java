package com.longju.xiantu.wood;

public final class SpiritWoodWorldgenRegression {
    public static void main(String[] args) {
        int checks = 0;
        for (SpiritWoodType type : SpiritWoodType.values()) {
            double normal = SpiritWoodWorldgenRules.chance(type, false);
            double forest = SpiritWoodWorldgenRules.chance(type, true);
            double expected = type.rank() == 1 ? normal : normal * 1.5D;
            if (Math.abs(forest - expected) > 1.0E-12D) {
                throw new AssertionError("Vanilla-forest tree chance mismatch: " + type.id);
            }
            if (type.rank() == 1 && Math.abs(normal - (1.0D / 8.0D)) > 1.0E-12D) {
                throw new AssertionError("First-tier tree ordinary habitat chance must be 1/8");
            }
            checks += 2;
        }
        if (Math.abs(SpiritWoodWorldgenRules.chance(SpiritWoodType.QING_WEN_ELM, true) - 0.125D) > 1.0E-12D) {
            throw new AssertionError("Vanilla forests should use the same 12.5% first-tier tree chance");
        }
        checks++;
        if (!SpiritTreeGrowth.canReplace(false, true, true)) {
            throw new AssertionError("Spirit-wood canopies must overlap existing leaf blocks");
        }
        if (SpiritTreeGrowth.canReplace(false, true, false)) {
            throw new AssertionError("Spirit-wood trunks must not replace existing leaf blocks");
        }
        if (!SpiritTreeGrowth.canReplace(true, false, false)) {
            throw new AssertionError("Spirit-wood trees must still occupy air");
        }
        checks += 3;
        System.out.println("Spirit wood vanilla-forest worldgen regression: " + checks + " passed");
    }

    private SpiritWoodWorldgenRegression() {}
}
