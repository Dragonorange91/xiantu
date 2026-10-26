package com.longju.xiantu.cultivation;

public final class InnerDemonRegression {
    private static int checks;
    private InnerDemonRegression() {}

    public static void main(String[] args) {
        require(!InnerDemonRules.requiresChallenge(false, 3, 100), "Minor breakthrough never starts a heart-demon battle");
        require(!InnerDemonRules.requiresChallenge(true, 2, 100), "Before Nascent Soul, major breakthroughs never start a heart-demon battle");
        require(!InnerDemonRules.requiresChallenge(true, 3, 59.999), "Nascent Soul breakthrough below sixty proceeds normally");
        require(InnerDemonRules.requiresChallenge(true, 3, 60), "Nascent Soul to Spirit Transformation starts a heart-demon battle at sixty");
        require(InnerDemonRules.requiresChallenge(true, 8, 100), "Later major breakthroughs also start a heart-demon battle");
        require(InnerDemonRules.DURATION_TICKS == 3600, "Challenge duration is exactly three minutes");
        require(InnerDemonRules.FAILURE_HEALTH_RATIO == .20F, "Low-health failure boundary is twenty percent of maximum health");
        require(!InnerDemonRules.wouldFallBelowFailureHealth(10, 20, 6), "Damage ending exactly at twenty percent does not cross the boundary");
        require(InnerDemonRules.wouldFallBelowFailureHealth(10, 20, 6.01F), "Damage crossing below twenty percent triggers failure");
        require(InnerDemonRules.damageToFailureBoundary(10, 20) == 6, "Crossing damage is capped to leave the player at twenty percent health");
        require(!InnerDemonRules.belowFailureHealth(4, 20) && InnerDemonRules.belowFailureHealth(3.99F, 20),
                "The health check is strict: exactly twenty percent survives, below it fails");
        require(InnerDemonRules.victoryInnerDemon(60) == 30 && InnerDemonRules.victoryInnerDemon(20) == 0,
                "Victory reduces inner demon by thirty with a zero floor");
        require(InnerDemonRules.FAILURE_INCREASE == 10,
                "Challenge failure uses only the ten-point penalty and does not stack the normal major-failure penalty");
        require(InnerDemonRules.CRYSTAL_DROP_CHANCE == .10F, "Victory crystal chance is ten percent");
        System.out.println("Inner demon regression: " + checks + " passed");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("[Inner demon regression FAIL] " + label);
        checks++;
        System.out.println("[Inner demon regression PASS] " + label);
    }
}
