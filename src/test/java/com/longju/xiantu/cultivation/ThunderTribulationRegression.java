package com.longju.xiantu.cultivation;

public final class ThunderTribulationRegression {
    private static int checks;
    private ThunderTribulationRegression() {}

    public static void main(String[] args) {
        int[] counts = {0, 3, 6, 9, 9, 9, 9, 9, 0};
        int[] times = {0, 500, 860, 1220, 1220, 1220, 1220, 1220, 0};
        require(!ThunderTribulationRules.hasTribulation(0), "Qi Refining to Foundation does not trigger tribulation");
        require(ThunderTribulationRules.hasTribulation(1), "Foundation to Golden Core is the first tribulation");
        require(!ThunderTribulationRules.requiresTribulation(true, 0, RealmRegistry.STAGE_COUNTS[0] - 1),
                "The last Qi Refining stage still has no tribulation");
        require(ThunderTribulationRules.requiresTribulation(true, 1, RealmRegistry.STAGE_COUNTS[1] - 1),
                "Foundation breakthrough into Golden Core triggers tribulation");
        require(!ThunderTribulationRules.requiresTribulation(false, 1, RealmRegistry.STAGE_COUNTS[1] - 1),
                "Minor-stage breakthroughs do not trigger tribulation");
        require(!ThunderTribulationRules.requiresTribulation(true, 1, RealmRegistry.STAGE_COUNTS[1] - 2),
                "Tribulation only starts at the final stage of a major realm");
        for (int realm = 0; realm < counts.length; realm++) {
            require(ThunderTribulationRules.strikes(realm) == counts[realm], "Strike count for realm " + realm);
            require(ThunderTribulationRules.totalTicks(realm) == times[realm], "25/43/61 second timeline for realm " + realm);
        }
        double[] ratios = {0, .25, .30, .35, .40, .425, .45, .50, 0};
        for (int realm = 0; realm < ratios.length; realm++)
            require(ThunderTribulationRules.damageRatio(realm) == ratios[realm], "Damage snapshot ratio for realm " + realm);
        require(ThunderTribulationRules.rawDamage(1, 3) == 10.24D, "Foundation to Golden Core strike uses 32 x 1.28 x 25 percent");
        require(ThunderTribulationRules.rawDamage(2, 3) == 19.968D, "Golden Core to Nascent Soul strike uses 52 x 1.28 x 30 percent");
        require(ThunderTribulationRules.rawDamage(3, 3) == 37.632D, "Nascent Soul to Spirit Transformation strike uses 84 x 1.28 x 35 percent");
        System.out.println("Thunder tribulation regression: " + checks + " passed");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("[Thunder tribulation regression FAIL] " + label);
        checks++;
        System.out.println("[Thunder tribulation regression PASS] " + label);
    }
}
