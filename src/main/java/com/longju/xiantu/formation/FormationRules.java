package com.longju.xiantu.formation;

import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.item.SpiritStoneAbsorptionRules;

/** Shared server rules and pure arithmetic; no inventory or client authority. */
public final class FormationRules {
    private FormationRules() {}
    public static int powerPerMinute(int tier) { return new int[]{50, 200, 600}[tier - 1]; }
    public static int bonusTicks(int stoneRank) { return stoneRank * 120 * 20; }
    public static double bonusMultiplier(int tier, int stoneRank) {
        return 1 + Math.min(stoneRank / 10.0, new double[]{0.3, 0.5, 0.8}[tier - 1]);
    }
    public static boolean unsafe(int realm, int stoneRank) {
        return stoneRank > SpiritStoneAbsorptionRules.stoneRankForRealm(realm);
    }
    public static int minimumRealm(int stoneRank) {
        return stoneRank <= 6 ? Math.max(0, stoneRank - 1) : stoneRank;
    }
    public static double rootMultiplier(int roots) {
        return switch (roots) { case 1 -> 2; case 2 -> 1.5; case 3 -> 1; case 4 -> 0.8; default -> 0.6; };
    }
    public static double demonMultiplier(double demon) {
        return demon < 30 ? 1 : demon < 60 ? 0.8 : demon < 90 ? 0.5 : 0.2;
    }
    public static double baseRate(int realm) {
        // Per minor stage: three roots, ordinary physique, no method/demon/environment bonus.
        // A growing progress cap must not silently turn later stages into hours of waiting.
        return RealmRegistry.get(realm, 0).progressCap() / (naturalStageMinutes(realm) * 60);
    }
    public static double naturalStageMinutes(int realm) { return 4 + 2 * Math.max(0, Math.min(8, realm)); }
    /** Future physique/technique/temporary effects plug into explicit, separate factors. */
    public static double progressPerSecond(int realm, int roots, double demon, int arrayTier, int stoneRank,
                                          double physique, double technique, double temporaryBonus) {
        return baseRate(realm) * rootMultiplier(roots) * demonMultiplier(demon) * 1.5
                * bonusMultiplier(arrayTier, stoneRank) * physique * technique
                * (1 + Math.max(0, Math.min(.35, temporaryBonus)));
    }
    public static long ticksForBudget(long budget, int costPerMinute) {
        long debit = costPerMinute * 1000L;
        return budget / debit + (budget % debit == 0 ? 0 : 1);
    }
    // ContainerData crosses the wire as signed shorts. Use only fifteen unsigned bits per limb.
    public static int limb(long value, int index) { return (int) ((value >>> (15 * index)) & 0x7fff); }
    public static long join(int... limbs) {
        long value = 0;
        for (int i = 0; i < limbs.length; i++) value |= (long) (limbs[i] & 0x7fff) << (15 * i);
        return value;
    }
}
