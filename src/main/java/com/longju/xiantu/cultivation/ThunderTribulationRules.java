package com.longju.xiantu.cultivation;

public final class ThunderTribulationRules {
    private ThunderTribulationRules() {}

    public static boolean requiresTribulation(boolean majorBreakthrough, int realm, int stage) {
        return majorBreakthrough && hasTribulation(realm)
                && realm < RealmRegistry.STAGE_COUNTS.length
                && stage == RealmRegistry.STAGE_COUNTS[realm] - 1;
    }

    public static boolean hasTribulation(int realm) {
        return realm >= 1 && realm <= 7;
    }

    public static int strikes(int realm) {
        return !hasTribulation(realm) ? 0 : realm == 1 ? 3 : realm == 2 ? 6 : 9;
    }

    public static double damageRatio(int realm) {
        return switch (realm) {
            case 1 -> .25D;
            case 2 -> .30D;
            case 3 -> .35D;
            case 4 -> .40D;
            case 5 -> .425D;
            case 6 -> .45D;
            case 7 -> .50D;
            default -> 0;
        };
    }

    public static double rawDamage(int realm, int stage) {
        return RealmRegistry.baseHealthExact(realm, stage) * damageRatio(realm);
    }

    public static int totalTicks(int realm) {
        int count = strikes(realm);
        return count == 0 ? 0 : 100 + count * 120 + 40;
    }

}
