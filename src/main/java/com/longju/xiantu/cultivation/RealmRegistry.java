package com.longju.xiantu.cultivation;

import java.util.ArrayList;
import java.util.List;

public final class RealmRegistry {
    public static final List<String> MAJOR_REALM_NAMES = List.of(
            "练气", "筑基", "金丹", "元婴", "化神", "炼虚", "合体", "大乘", "渡劫");
    public static final int[] STAGE_COUNTS = {9, 4, 4, 4, 4, 4, 4, 4, 4};
    private static final String[][] STAGE_NAMES = {
            {"1层", "2层", "3层", "4层", "5层", "6层", "7层", "8层", "9层"},
            {"初期", "中期", "后期", "大圆满"},
            {"初期", "中期", "后期", "大圆满"},
            {"初期", "中期", "后期", "巅峰"},
            {"初期", "中期", "后期", "巅峰"},
            {"初期", "中期", "后期", "巅峰"},
            {"初期", "中期", "后期", "巅峰"},
            {"初期", "中期", "后期", "巅峰"},
            {"初期", "中期", "后期", "巅峰"}
    };
    private static final long[] PROGRESS_CAPS = {330, 4_212, 8_424, 15_120, 25_313, 36_288, 43_288, 51_638, 77_760};
    private static final long[] POWER_CAPS = {100, 300, 1_000, 3_000, 10_000, 30_000, 55_000, 100_000, 300_000};
    private static final int[] BASE_HEALTH = {20, 32, 52, 84, 135, 215, 270, 340, 520};
    private static final int[] BASE_ATTACK = {6, 10, 18, 32, 56, 96, 124, 160, 260};
    private static final int[] STAGE_PERCENT = {100, 108, 117, 128};
    public static final List<RealmDefinition> REALMS = buildStages();

    private RealmRegistry() {}

    private static List<RealmDefinition> buildStages() {
        List<RealmDefinition> result = new ArrayList<>(41);
        int index = 0;
        for (int realm = 0; realm < MAJOR_REALM_NAMES.size(); realm++) {
            for (int stage = 0; stage < STAGE_COUNTS[realm]; stage++) {
                result.add(new RealmDefinition(index++, realm, stage, MAJOR_REALM_NAMES.get(realm),
                        STAGE_NAMES[realm][stage], stageProgressCap(realm, stage), POWER_CAPS[realm]));
            }
        }
        return List.copyOf(result);
    }

    private static long stageProgressCap(int realm, int stage) {
        // Linear growth avoids exponential waiting; later major realms still start above prior peaks.
        int percent = 100 + stage * (realm == 0 ? 15 : 5);
        return (PROGRESS_CAPS[realm] * percent + 50) / 100;
    }

    public static RealmDefinition get(int index) {
        return REALMS.get(Math.max(0, Math.min(index, REALMS.size() - 1)));
    }

    public static RealmDefinition get(int majorRealm, int stage) {
        int realm = Math.max(0, Math.min(majorRealm, STAGE_COUNTS.length - 1));
        int boundedStage = Math.max(0, Math.min(stage, STAGE_COUNTS[realm] - 1));
        return get(indexOf(realm, boundedStage));
    }

    public static int indexOf(int majorRealm, int stage) {
        int realm = Math.max(0, Math.min(majorRealm, STAGE_COUNTS.length - 1));
        int boundedStage = Math.max(0, Math.min(stage, STAGE_COUNTS[realm] - 1));
        int index = boundedStage;
        for (int i = 0; i < realm; i++) index += STAGE_COUNTS[i];
        return index;
    }

    public static boolean isFinalStage(int majorRealm, int stage) {
        return majorRealm == STAGE_COUNTS.length - 1 && stage == STAGE_COUNTS[majorRealm] - 1;
    }

    public static double healthBonus(int majorRealm, int stage) {
        // Vanilla players already have 20 health; add only the difference.
        return baseHealth(majorRealm, stage) - 20;
    }

    public static double attackBonus(int majorRealm, int stage) {
        // Vanilla players already have 1 attack damage; preserve weapon modifiers.
        return baseAttack(majorRealm, stage) - 1;
    }

    public static int baseHealth(int majorRealm, int stage) {
        return roundedStageStat(BASE_HEALTH, majorRealm, stage);
    }

    public static double baseHealthExact(int majorRealm, int stage) {
        RealmDefinition realm = get(majorRealm, stage);
        int percent = realm.majorIndex() == 0 ? 100 + 3 * realm.stage() : STAGE_PERCENT[realm.stage()];
        return BASE_HEALTH[realm.majorIndex()] * percent / 100.0D;
    }

    public static double baseAttackExact(int majorRealm, int stage) {
        RealmDefinition realm = get(majorRealm, stage);
        int percent = realm.majorIndex() == 0 ? 100 + 3 * realm.stage() : STAGE_PERCENT[realm.stage()];
        return BASE_ATTACK[realm.majorIndex()] * percent / 100.0D;
    }

    public static int baseAttack(int majorRealm, int stage) {
        return roundedStageStat(BASE_ATTACK, majorRealm, stage);
    }

    private static int roundedStageStat(int[] values, int majorRealm, int stage) {
        RealmDefinition realm = get(majorRealm, stage);
        int percent = realm.majorIndex() == 0 ? 100 + 3 * realm.stage() : STAGE_PERCENT[realm.stage()];
        // Integer half-up rounding avoids floating-point artifacts at .5 boundaries.
        return (values[realm.majorIndex()] * percent + 50) / 100;
    }

    public static double basePowerRecovery(int majorRealm, int stage) {
        return Math.max(1.0D, get(majorRealm, stage).powerCap() / 400.0D);
    }

    public static int spiritStoneAbsorptionPerSecond(int majorRealm, int stage) {
        return 50 + indexOf(majorRealm, stage) * 10;
    }
}
