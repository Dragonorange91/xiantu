package com.longju.xiantu.cultivation;

/** A stage-relative combat reward, independent of meditation/root/array multipliers. */
public final class KillCultivationRules {
    private KillCultivationRules() {}
    public static double differenceMultiplier(int difference) {
        return difference <= -3 ? 0 : difference == -2 ? .1 : difference == -1 ? .5
                : difference == 0 ? 1 : difference == 1 ? 1.25 : 1.5;
    }
    public static double reward(double cap, double progress, int playerRealm, int targetRealm, boolean elite, boolean boss) {
        if (!Double.isFinite(cap) || !Double.isFinite(progress) || cap <= 0 || progress >= cap
                || playerRealm < 0 || playerRealm > 8 || targetRealm < 0 || targetRealm > 8) return 0;
        double fraction = Math.min(.05, .005 * (boss ? 10 : elite ? 3 : 1) * differenceMultiplier(targetRealm-playerRealm));
        return Math.min(cap-Math.max(0,progress), cap*fraction);
    }
}
