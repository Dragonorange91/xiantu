package com.longju.xiantu.item;

import com.longju.xiantu.cultivation.RealmRegistry;

public final class SpiritStoneAbsorptionRegression {
    private static int checks;

    public static void main(String[] args) {
        var equalTier = evaluate(2, 300_000, 2, 0, 1_000_000);
        require(equalTier.decision() == SpiritStoneAbsorptionRules.Decision.READY
                        && equalTier.gainMilli() == 300_000 && !equalTier.overRank(),
                "Same-tier absorption transfers the stone's remaining charge");

        var partiallyCharged = evaluate(1, 37_500, 1, 10_000, 1_000_000);
        require(partiallyCharged.decision() == SpiritStoneAbsorptionRules.Decision.READY
                        && partiallyCharged.gainMilli() == 37_500,
                "Partial charge is absorbed at its actual stored value");

        var oneAbove = evaluate(3, 900_000, 2, 0, 2_000_000);
        require(oneAbove.decision() == SpiritStoneAbsorptionRules.Decision.READY
                        && oneAbove.gainMilli() == 900_000 && !oneAbove.overRank(),
                "One tier above absorbs one stone normally");

        var dangerous = evaluate(4, 2_700_000, 2, 0, 10_000_000);
        require(dangerous.decision() == SpiritStoneAbsorptionRules.Decision.READY && dangerous.overRank(),
                "Two or more tiers above starts backlash without a confirmation prompt");

        var highestOverflow = evaluate(8, 299_000_000, 1, 0, 10_000_000);
        require(highestOverflow.decision() == SpiritStoneAbsorptionRules.Decision.READY
                        && highestOverflow.overRank() && highestOverflow.overflow(),
                "Highest-tier over-rank absorption bypasses overflow confirmation and caps the gain");

        var partialRoom = evaluate(1, 100_000, 1, 950_000, 1_000_000);
        require(partialRoom.decision() == SpiritStoneAbsorptionRules.Decision.READY && partialRoom.overflow(),
                "Near-full power starts without crouch confirmation and is capped at the available room");

        require(evaluate(1, 100_000, 1, 1_000_000, 1_000_000).decision()
                        == SpiritStoneAbsorptionRules.Decision.FULL,
                "Full power rejects absorption without consuming stones");
        require(evaluate(9, 100_000, 1, 0, 1_000_000).decision()
                        == SpiritStoneAbsorptionRules.Decision.INVALID,
                "Invalid stone ranks are rejected");
        int[] realmStoneRanks = {1, 2, 3, 4, 5, 6, 6, 7, 8};
        for (int realm = 0; realm < realmStoneRanks.length; realm++) {
            require(SpiritStoneAbsorptionRules.stoneRankForRealm(realm) == realmStoneRanks[realm],
                    "Realm " + realm + " maps to spirit-stone rank " + realmStoneRanks[realm]);
        }
        require(SpiritStoneAbsorptionRules.absorptionAmountMilli(50, 20) == 50_000L
                        && SpiritStoneAbsorptionRules.absorptionAmountMilli(450, 20) == 450_000L,
                "Absorption amount follows the cultivation-specific per-second rate");
        require(RealmRegistry.spiritStoneAbsorptionPerSecond(0, 0) == 50
                        && RealmRegistry.spiritStoneAbsorptionPerSecond(0, 8) == 130
                        && RealmRegistry.spiritStoneAbsorptionPerSecond(1, 0) == 140
                        && RealmRegistry.spiritStoneAbsorptionPerSecond(8, 3) == 450,
                "All forty-one cultivation stages receive distinct increasing absorption rates");
        for (int index = 0; index < RealmRegistry.REALMS.size() - 1; index++) {
            var current = RealmRegistry.get(index);
            var next = RealmRegistry.get(index + 1);
            require(RealmRegistry.spiritStoneAbsorptionPerSecond(current.majorIndex(), current.stage())
                            < RealmRegistry.spiritStoneAbsorptionPerSecond(next.majorIndex(), next.stage()),
                    "Absorption rate increases at cultivation stage " + index);
        }
        require(SpiritStoneAbsorptionRules.backlashDamage(1.0D, 2.0D, 0, 2) == 4.0D
                        && SpiritStoneAbsorptionRules.backlashDamage(1.0D, 2.0D, 1, 2) == 8.0D
                        && SpiritStoneAbsorptionRules.backlashDamage(1.0D, 2.0D, 2, 2) == 16.0D
                        && SpiritStoneAbsorptionRules.backlashDamage(1.0D, 2.0D, 3, 2) == 32.0D
                        && SpiritStoneAbsorptionRules.backlashDamage(1.0D, 2.0D, 6, 2) == 256.0D,
                "Backlash damage doubles without a cap and scales with the rank gap");
        require(SpiritStoneAbsorptionRules.backlashDamage(1.0D, 2.0D, 0, 7)
                        + SpiritStoneAbsorptionRules.backlashDamage(1.0D, 2.0D, 1, 7) > 20.0D,
                "A Qi Refining player cannot survive the first two pulses from a Dao Stone");
        require(evaluate(1, 100_000, 1, 1_000_000, 1_000_000).decision()
                        == SpiritStoneAbsorptionRules.Decision.FULL,
                "Full power cannot begin absorbing any stone");
        System.out.println("Spirit stone absorption regression: " + checks + " passed");
    }

    private static SpiritStoneAbsorptionRules.Plan evaluate(int tier, long charge, int realm,
                                                            long current, long cap) {
        return SpiritStoneAbsorptionRules.evaluate(tier, charge, realm, current, cap);
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
        System.out.println("[Spirit stone absorption PASS] " + label);
    }
}
