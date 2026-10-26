package com.longju.xiantu.item;

public final class SpiritStoneAbsorptionRules {
    private SpiritStoneAbsorptionRules() {}

    public static double backlashDamage(double startDamage, double damageMultiplier, long previousPulses,
                                        int rankDifference) {
        int rankGap = Math.max(1, rankDifference);
        return startDamage * rankGap * Math.pow(damageMultiplier, Math.max(0L, previousPulses) + 1L);
    }

    public static Plan evaluate(int stoneRank, long stoneChargeMilli, int currentRealmRank,
                                long currentPowerMilli, long powerCapMilli) {
        if (stoneRank < 1 || stoneRank > 8 || stoneChargeMilli <= 0
                || currentPowerMilli < 0 || powerCapMilli <= 0) {
            return Plan.rejected(Decision.INVALID);
        }
        if (currentPowerMilli >= powerCapMilli) return Plan.rejected(Decision.FULL);

        int rankDifference = stoneRank - Math.max(1, Math.min(9, currentRealmRank));
        boolean overRank = rankDifference >= 2;
        boolean overflow = stoneChargeMilli > powerCapMilli - currentPowerMilli;
        return new Plan(Decision.READY, stoneChargeMilli, overRank, overflow);
    }

    public static long absorptionAmountMilli(int ratePerSecond, int elapsedTicks) {
        if (ratePerSecond <= 0 || elapsedTicks <= 0) return 0L;
        return (long) ratePerSecond * 1_000L * elapsedTicks / 20L;
    }

    public static int stoneRankForRealm(int majorRealm) {
        int realm = Math.max(0, Math.min(8, majorRealm));
        return switch (realm) {
            case 0, 1, 2, 3, 4 -> realm + 1;
            case 5, 6 -> 6;
            case 7 -> 7;
            default -> 8;
        };
    }

    public enum Decision {
        READY, FULL, INVALID
    }

    public record Plan(Decision decision, long gainMilli, boolean overRank, boolean overflow) {
        private static Plan rejected(Decision decision) {
            return new Plan(decision, 0L, false, false);
        }
    }
}
