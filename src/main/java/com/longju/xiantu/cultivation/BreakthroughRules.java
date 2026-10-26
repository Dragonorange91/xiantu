package com.longju.xiantu.cultivation;

/** Pure breakthrough arithmetic shared by the GUI preview and authoritative server transaction. */
public final class BreakthroughRules {
    public static final double MIN_CHANCE = .05D;
    public static final double MAX_CHANCE = 1.0D;
    private BreakthroughRules() {}

    public static boolean isMajorBreakthrough(int realm, int stage) {
        return realm >= 0 && realm < RealmRegistry.STAGE_COUNTS.length
                && stage == RealmRegistry.STAGE_COUNTS[realm] - 1;
    }

    public static boolean shouldAutomaticallyBreakthrough(CultivationData data) {
        return data.cultivationUnlocked()
                && !RealmRegistry.isFinalStage(data.majorRealm(), data.stage())
                && !isMajorBreakthrough(data.majorRealm(), data.stage())
                && data.progress() >= data.progressMax();
    }

    public static double baseChance(int realm, int stage) {
        if (realm < 0 || realm >= RealmRegistry.STAGE_COUNTS.length
                || stage < 0 || stage >= RealmRegistry.STAGE_COUNTS[realm]) return 0;
        if (isMajorBreakthrough(realm, stage)) return .40D;
        if (realm == 0) return stage < 3 ? .80D : stage < 6 ? .70D : .60D;
        return switch (stage) {
            case 0 -> .80D;
            case 1 -> .70D;
            default -> .60D;
        };
    }

    public static double successChance(CultivationData data) {
        if (RealmRegistry.isFinalStage(data.majorRealm(), data.stage())) return 0;
        return clamp(baseChance(data.majorRealm(), data.stage())
                + data.comprehension() * .02D
                + data.physique().breakthroughBonus
                - data.innerDemon() * .005D, MIN_CHANCE, MAX_CHANCE);
    }

    public static long failureProgress(long progressCap) {
        long bounded=Math.max(0,progressCap);
        return bounded / 10 * 7 + bounded % 10 * 7 / 10;
    }

    public static double failureInnerDemon(boolean majorBreakthrough) {
        return majorBreakthrough ? 20 : 10;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
