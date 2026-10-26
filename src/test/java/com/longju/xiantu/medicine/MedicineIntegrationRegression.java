package com.longju.xiantu.medicine;

public final class MedicineIntegrationRegression {
    private int assertions;

    public static void main(String[] args) {
        new MedicineIntegrationRegression().run();
    }

    private void run() {
        check(MedicineCatalog.HERBS.size() == 60, "all manifest materials are represented");
        check(MedicineCatalog.HERBS.stream().filter(MedicineDefinition::plantable).count() == 39,
                "only the manifest's 39 seed-bearing plants are plantable");
        check(MedicineCatalog.HERBS.stream().filter(MedicineDefinition::wildPlant).count() == 54,
                "all lifecycle plants have a wild plant block");
        check(MedicineCatalog.HERBS.stream().filter(MedicineDefinition::seedItem).count() == 39,
                "seed item count agrees with sowing eligibility");
        check(MedicineCatalog.HERBS.stream().filter(definition -> definition.tier() >= 7
                        && !definition.plantable() && definition.wildPlant() && !definition.seedItem()).count() == 14,
                "all seventh and eighth tier herbs are wild-only and cannot be cultivated");
        for (int tier = 1; tier <= 6; tier++) {
            check(MedicineRules.canPlant(tier, false, tier - 1), "matching soil accepts herb tier " + tier);
            check(!MedicineRules.canPlant(tier, false, tier - 2), "lower soil rejects herb tier " + tier);
            check(MedicineRules.growthSeconds(tier, 0) == 240 * (1 << (tier - 1)),
                    "first soil growth duration for tier " + tier);
        }
        check(MedicineRules.canPlant(1, true, -1), "vanilla farmland accepts first-tier herbs");
        check(!MedicineRules.canPlant(2, true, -1), "vanilla farmland rejects second-tier herbs");
        check(!MedicineRules.canPlant(7, false, 6), "seventh-tier herbs cannot be cultivated");
        check(MedicineRules.growthSeconds(1, 6) == 80, "highest soil growth baseline");
        check(MedicineRules.wildSeedChance(1) == 0.15f && MedicineRules.wildSeedChance(2) == 0.15f
                && MedicineRules.wildSeedChance(3) == 0.10f && MedicineRules.wildSeedChance(4) == 0.10f
                && MedicineRules.wildSeedChance(5) == 0.05f && MedicineRules.wildSeedChance(6) == 0.02f
                && MedicineRules.wildSeedChance(7) == 0,
                "wild seed chance follows the grade bands");
        check(MedicineRules.cultivatedSeedChance(4) == 0.25f && MedicineRules.cultivatedSeedChance(5) == 0,
                "cultivated seed chance does not duplicate rare seeds");
        System.out.println("Medicine integration regression: " + assertions + " assertions passed");
    }

    private void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
