package com.longju.xiantu.cultivation;

public final class InnerDemonRules {
    public static final int DURATION_TICKS = 20 * 60 * 3;
    public static final float FAILURE_HEALTH_RATIO = 0.20F;
    public static final double REQUIRED_INNER_DEMON = 60.0D;
    public static final double VICTORY_REDUCTION = 30.0D;
    public static final double FAILURE_INCREASE = 10.0D;
    public static final float CRYSTAL_DROP_CHANCE = 0.10F;

    private InnerDemonRules() {}
    public static boolean requiresChallenge(boolean majorBreakthrough, int currentMajorRealm, double innerDemon) {
        return majorBreakthrough && currentMajorRealm >= 3 && innerDemon >= REQUIRED_INNER_DEMON;
    }
    public static double victoryInnerDemon(double current) { return Math.max(0.0D, current - VICTORY_REDUCTION); }
    public static boolean wouldFallBelowFailureHealth(float health, float maxHealth, float damage) {
        return maxHealth > 0.0F && health - damage < maxHealth * FAILURE_HEALTH_RATIO;
    }
    public static float damageToFailureBoundary(float health, float maxHealth) {
        return Math.max(0.0F, health - maxHealth * FAILURE_HEALTH_RATIO);
    }
    public static boolean belowFailureHealth(float health, float maxHealth) {
        return maxHealth > 0.0F && health < maxHealth * FAILURE_HEALTH_RATIO;
    }
}
