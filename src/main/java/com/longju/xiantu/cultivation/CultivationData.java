package com.longju.xiantu.cultivation;

public interface CultivationData {
    PhysiqueType physique();
    com.longju.xiantu.gongfa.GongfaData gongfa();
    boolean cultivationUnlocked();
    void setCultivationUnlocked(boolean unlocked);
    com.longju.xiantu.alchemy.MedicineData medicine();
    com.longju.xiantu.spell.SpellData spells();
    int majorRealm();
    int stage();
    int realm();
    int minorRealm();
    double progress();
    double progressMax();
    double power();
    double powerMax();
    double powerSpeed();
    void setPowerSpeed(double powerSpeed);
    int spiritRootCount();
    double merits();
    double ferocious();
    int comprehension();
    double innerDemon();
    double breakthroughBarrier();
    double healthBonus();
    double attackBonus();
    double defenseBonus();
    boolean hasSpiritRoot(SpiritRootType type);
    void setRealm(int realm, int minorRealm);
    void setRealmStage(int majorRealm, int stage);
    void setProgress(double progress);
    void setPower(double power);
    void setPowerMax(double powerMax);
    void setMerits(double merits);
    void setFerocious(double ferocious);
    void setComprehension(int comprehension);
    void setInnerDemon(double innerDemon);
    void setBreakthroughBarrier(double breakthroughBarrier);
    void setDefenseBonus(double defenseBonus);
    void addPower(double amount);
    void addProgress(double amount);
    void addSpiritRoot(SpiritRootType type);
    CultivationData copy();
}

