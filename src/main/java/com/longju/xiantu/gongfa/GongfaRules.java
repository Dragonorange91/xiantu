package com.longju.xiantu.gongfa;

/** Pure rules shared by the server, preview and regression harness. */
public final class GongfaRules {
    private static final int[] EXPERIENCE = {60, 90, 120, 180, 240, 300, 420, 540, 720};
    public static final int PREPARATION_TICKS = 200;
    private GongfaRules() {}
    public static double combatMultiplier(double physiqueBonus, double methodBonus) {
        return Math.min(2.2, 1 + Math.max(0, physiqueBonus) + Math.max(0, methodBonus));
    }
    public static int nextExperience(int level) { return level >= 9 ? 0 : EXPERIENCE[Math.max(0, level)]; }
    public static double multiplier(double maximum, int level) {
        return 1 + (maximum - 1) * Math.max(0, Math.min(9, level)) / 9;
    }
    public static int replacementRealm(int realm) { return Math.max(0, realm - 1); }
    public static int replacementStage(int realm, int stage) {
        if (realm == 1) return new int[]{0, 2, 5, 8}[Math.max(0, Math.min(3, stage))];
        return stage;
    }
    public static int killExperience(int playerRank, int targetRank, boolean elite, boolean boss) {
        int base = 20 * Math.max(1, Math.min(9, targetRank)) * (boss ? 5 : elite ? 2 : 1);
        return playerRank - targetRank >= 2 ? base / 4 : base;
    }
}
