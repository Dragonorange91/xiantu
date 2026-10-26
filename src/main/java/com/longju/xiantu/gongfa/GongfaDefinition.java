package com.longju.xiantu.gongfa;

/** Percent bonuses are declared at layer nine, never granted in full at layer zero. */
public record GongfaDefinition(String id, String name, int tierIndex, int element,
                               int minRealm, int maxRealm, double powerMaxBonus, double recoveryBonus,
                               double attackBonus, double healthBonus, double armorBonus,
                               GongfaBuff buff, double buffBonus) {
    private static final String[] TIERS = {"凡阶", "黄阶", "玄阶", "地阶", "天阶", "仙阶", "神阶", "混沌阶"};
    private static final double[] SPEED = {1, 1.3, 1.6, 2, 2.5, 2.8, 3, 3.2};
    public String tier() { return TIERS[tierIndex]; }
    public double maximumMultiplier() { return SPEED[tierIndex]; }
    public boolean basicElemental() { return tierIndex == 1 && element >= 1 && element <= 5; }
    public boolean special() { return id.equals("tai_chu_dao_jing"); }
    public boolean learnableAt(int realm, int stage) {
        if (tierIndex == 0) return true;
        if (!usableAt(realm)) return false;
        if (special()) return true;
        return realm >= Math.max(1, minRealm) || basicElemental() && realm == 0 && stage == 8;
    }
    public String learningRequirement() {
        if (special()) return "练气起可修炼至渡劫";
        if (basicElemental()) return "练气九层可换学，用于突破筑基";
        return com.longju.xiantu.cultivation.RealmRegistry.MAJOR_REALM_NAMES.get(Math.max(1,minRealm)) + "起可学习";
    }
    public boolean usableAt(int realm) { return realm >= minRealm; }
    public double cultivationMultiplier(int layer) { return GongfaRules.multiplier(maximumMultiplier(), layer); }
    public double powerBonus(int layer) { return powerMaxBonus * Math.max(0, Math.min(9, layer)) / 9; }
    public double recoveryBonus(int layer) { return recoveryBonus * Math.max(0, Math.min(9, layer)) / 9; }
    public double attackBonus(int layer) { return attackBonus * Math.max(0, Math.min(9, layer)) / 9; }
    public double healthBonus(int layer) { return healthBonus * Math.max(0, Math.min(9, layer)) / 9; }
    public double armorBonus(int layer) { return armorBonus * Math.max(0, Math.min(9, layer)) / 9; }
    public double buffBonus(int layer) { return buffBonus * Math.max(0, Math.min(9, layer)) / 9; }
}
