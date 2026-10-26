package com.longju.xiantu.cultivation;

import net.minecraft.nbt.CompoundTag;

public final class RealmRegression {
    private static int checks;

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
        System.out.println("[Realm PASS] " + label);
    }

    public static void main(String[] args) {
        require(RealmRegistry.MAJOR_REALM_NAMES.size() == 9 && RealmRegistry.REALMS.size() == 41,
                "Nine major realms contain forty-one minor stages");
        require(RealmRegistry.MAJOR_REALM_NAMES.get(6).equals("合体")
                        && RealmRegistry.MAJOR_REALM_NAMES.get(5).equals("炼虚")
                        && RealmRegistry.MAJOR_REALM_NAMES.get(7).equals("大乘"),
                "Body Combining remains between Void Refining and Great Vehicle");

        int nextIndex = 0;
        long previousCap = 0;
        for (int realm = 0; realm < RealmRegistry.STAGE_COUNTS.length; realm++) {
            require(RealmRegistry.indexOf(realm, 0) == nextIndex, "Realm start index " + realm);
            for (int stage = 0; stage < RealmRegistry.STAGE_COUNTS[realm]; stage++) {
                RealmDefinition definition = RealmRegistry.get(realm, stage);
                require(definition.index() == nextIndex + stage
                                && definition.majorIndex() == realm && definition.stage() == stage,
                        "Stage mapping " + realm + ":" + stage);
                require(definition.progressCap() > 0 && definition.powerCap() > 0,
                        "Positive caps " + realm + ":" + stage);
                require(definition.progressCap() > previousCap, "Progress requirements increase across every stage and realm");
                previousCap = definition.progressCap();
            }
            nextIndex += RealmRegistry.STAGE_COUNTS[realm];
        }
        long[] qiCaps={330,380,429,479,528,578,627,677,726};
        for(int i=0;i<qiCaps.length;i++)require(RealmRegistry.get(0,i).progressCap()==qiCaps[i],"Qi stage cap "+i);
        require(nextIndex == 41 && RealmRegistry.get(40).majorName().equals("渡劫")
                        && RealmRegistry.get(40).minorName().equals("巅峰"),
                "Final realm stage and flattened index");
        require(RealmRegistry.isFinalStage(8, 3) && !RealmRegistry.isFinalStage(8, 2)
                        && !RealmRegistry.isFinalStage(7, 3),
                "Only Tribulation Peak is final");

        PlayerCultivationData data = new PlayerCultivationData();
        require(!data.cultivationUnlocked() && data.powerSpeed()==0 && data.attackBonus()==0,"New player has not entered cultivation");
        require(data.progressMax() == 330 && data.majorRealm() == 0 && data.stage() == 0,
                "New player begins at Qi Refining layer one");
        data.setProgress(data.progressMax());
        require(!BreakthroughRules.shouldAutomaticallyBreakthrough(data),
                "A player without cultivation qualification cannot auto-advance");
        data.setProgress(0);
        data.setCultivationUnlocked(true);
        require(!BreakthroughRules.shouldAutomaticallyBreakthrough(data),
                "An incomplete minor stage does not auto-advance");
        data.setProgress(data.progressMax());
        require(BreakthroughRules.shouldAutomaticallyBreakthrough(data)
                        && !BreakthroughRules.isMajorBreakthrough(data.majorRealm(), data.stage()),
                "A full minor stage auto-advances without confirmation");
        data.setRealmStage(0, 8);
        data.setProgress(data.progressMax());
        require(!BreakthroughRules.shouldAutomaticallyBreakthrough(data)
                        && BreakthroughRules.isMajorBreakthrough(data.majorRealm(), data.stage()),
                "The last stage of a major realm still requires confirmation");
        data.setRealmStage(8, 3);
        require(!BreakthroughRules.shouldAutomaticallyBreakthrough(data),
                "The final cultivation stage never auto-advances");
        data.setRealmStage(0, 0);
        data.setProgress(0);
        require(data.healthBonus() == 0.0D && data.powerSpeed() == 1.0D,
                "Qi Refining layer one starts with 20 health and 1 power recovery per second");
        data.setProgress(999_999.0D);
        require(data.progress() == data.progressMax(), "Progress clamps to current stage cap");
        data.setRealmStage(1, 0);
        require(data.progress() == 330.0D && data.progressMax() == 4_212.0D,
                "Stage cap is re-derived without treating a direct stage set as a breakthrough");
        require(data.healthBonus() == 12 && data.attackBonus() == 9
                        && data.powerSpeed() == 1.0D,
                "Direct realm changes derive rank stats without requiring breakthrough history");
        data.setRealmStage(8, 3);
        require(data.healthBonus() == 646.0D && data.attackBonus() == 332.0D && data.powerSpeed() == 750.0D,
                "Tribulation Peak reaches 666 health, 333 base attack and 750 power recovery per second");
        int[][] health = {{20,21,21,22,22,23,24,24,25},{32,35,37,41},{52,56,61,67},
                {84,91,98,108},{135,146,158,173},{215,232,252,275},{270,292,316,346},
                {340,367,398,435},{520,562,608,666}};
        int[][] attack = {{6,6,6,7,7,7,7,7,7},{10,11,12,13},{18,19,21,23},
                {32,35,37,41},{56,60,66,72},{96,104,112,123},{124,134,145,159},
                {160,173,187,205},{260,281,304,333}};
        for (int realm = 0; realm < health.length; realm++) {
            for (int stage = 0; stage < health[realm].length; stage++) {
                data.setRealmStage(realm, stage);
                require(data.healthBonus() + 20 == health[realm][stage]
                                && data.attackBonus() + 1 == attack[realm][stage],
                        "Integer health/attack stats at " + realm + ":" + stage);
            }
        }
        data.setRealmStage(8, 3);
        data.setRealmStage(0, 0);
        require(data.healthBonus() == 0 && data.attackBonus() == 5,
                "Realm rollback drops both stats without retaining breakthrough bonuses");
        data.setPowerSpeed(data.powerSpeed() + 12.5D);
        data.setRealmStage(3, 0);
        require(data.powerSpeed() == 20.0D,
                "Permanent recovery bonuses remain additive when rank changes");
        data.setRealmStage(1, 0);
        data.setProgress(1234.567D);
        CompoundTag saved = new CompoundTag();
        data.addSpiritRoot(SpiritRootType.FIRE);
        data.addSpiritRoot(SpiritRootType.WATER);
        data.save(saved);
        require(!saved.contains("yin_yang"), "Inactive yin-yang attribute is no longer serialized");
        PlayerCultivationData restored = new PlayerCultivationData();
        restored.load(saved);
        require(restored.spiritRootCount() == 2 && restored.hasSpiritRoot(SpiritRootType.FIRE)
                && restored.hasSpiritRoot(SpiritRootType.WATER), "Removing inactive attribute preserves real spirit roots");
        require(restored.majorRealm() == 1 && restored.stage() == 0
                        && Math.abs(restored.progress() - 1234.567D) < 0.001D
                        && restored.powerSpeed() == 13.5D,
                "Stage progress and rank-derived recovery bonuses survive capability serialization");
        saved.putDouble("attack_bonus", 9999.5);
        restored.load(saved);
        require(restored.attackBonus() == 9 && restored.healthBonus() == 12,
                "Stale accumulated attack NBT cannot override realm-derived combat stats");
        PlayerCultivationData clone = (PlayerCultivationData) restored.copy();
        require(clone.attackBonus() == restored.attackBonus() && clone.healthBonus() == restored.healthBonus(),
                "Clone derives the same stats from the copied realm");

        PlayerCultivationData chance = new PlayerCultivationData();
        chance.setCultivationUnlocked(true);
        require(Math.abs(BreakthroughService.successChance(chance) - 0.82D) < 0.0001D,
                "Displayed minor breakthrough chance matches server calculation");
        double[] stageChance={.80,.80,.80,.70,.70,.70,.60,.60,.40};
        for(int stage=0;stage<stageChance.length;stage++) {
            chance.setRealmStage(0,stage);
            require(Math.abs(BreakthroughRules.baseChance(0,stage)-stageChance[stage])<.0001,
                    "Qi Refining base chance at layer "+(stage+1));
        }
        for(int realm=1;realm<RealmRegistry.STAGE_COUNTS.length;realm++) {
            double[] expected={.80,.70,.60,.40};
            for(int stage=0;stage<RealmRegistry.STAGE_COUNTS[realm];stage++)
                require(Math.abs(BreakthroughRules.baseChance(realm,stage)-expected[stage])<.0001,
                        "Major realm base chance at "+realm+":"+stage);
        }
        chance.setRealmStage(0, 8);
        require(Math.abs(BreakthroughService.successChance(chance) - 0.42D) < 0.0001D,
                "Qi Refining ninth layer uses the major breakthrough rule");
        require(BreakthroughRules.failureProgress(330)==231
                        && BreakthroughRules.failureProgress(4212)==2948,
                "Failure returns precisely to floor of seventy percent stage progress");
        require(BreakthroughRules.failureInnerDemon(false)==10
                        && BreakthroughRules.failureInnerDemon(true)==20,
                "Ordinary failed breakthroughs use the documented heart-demon delta");
        chance.setInnerDemon(100); chance.setComprehension(1);
        require(BreakthroughService.successChance(chance)==BreakthroughRules.MIN_CHANCE,
                "Breakthrough chance respects five-percent floor");
        chance.setRealmStage(0,0); chance.setInnerDemon(0); chance.setComprehension(20);
        require(BreakthroughService.successChance(chance)==BreakthroughRules.MAX_CHANCE,
                "Prepared breakthrough chance respects one-hundred-percent ceiling");
        chance.setRealmStage(8, 3);
        require(BreakthroughService.successChance(chance) == 0.0D,
                "Maximum realm has no breakthrough chance");

        System.out.println("Realm regression: " + checks + " passed");
    }
}
