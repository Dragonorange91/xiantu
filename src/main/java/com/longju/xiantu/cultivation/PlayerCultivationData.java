package com.longju.xiantu.cultivation;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumSet;

public final class PlayerCultivationData implements CultivationData {
    private static final long MAX_POWER_MILLI = 1_000_000_000_000_000L;
    private final com.longju.xiantu.alchemy.MedicineData medicine = new com.longju.xiantu.alchemy.MedicineData();
    private final com.longju.xiantu.spell.SpellData spells = new com.longju.xiantu.spell.SpellData();
    private final com.longju.xiantu.gongfa.GongfaData gongfa = new com.longju.xiantu.gongfa.GongfaData();
    private boolean cultivationUnlocked;
    private boolean characterInitialized;
    private PhysiqueType physique = PhysiqueType.ORDINARY;
    private boolean physiqueInitialized;
    private String deathPhysique = "";
    private int deathSpiritRootMask = -1;
    private double demonSubMilli;
    private net.minecraft.nbt.CompoundTag reincarnationStoneRecords = new net.minecraft.nbt.CompoundTag();
    private net.minecraft.nbt.ListTag pendingReincarnationStones = new net.minecraft.nbt.ListTag();
    @Override public PhysiqueType physique() { return physique; }
    public boolean physiqueInitialized() { return physiqueInitialized; }
    public void setPhysique(PhysiqueType value) { physique = java.util.Objects.requireNonNull(value); physiqueInitialized = true; }
    public PhysiqueType deathPhysique(net.minecraft.util.RandomSource random, double[] weights) {
        if (deathPhysique.isEmpty()) deathPhysique = PhysiqueType.roll(random.nextDouble(),weights).id;
        return PhysiqueType.byId(deathPhysique);
    }
    public void clearDeathPhysique() { deathPhysique = ""; }
    public int deathSpiritRootMask(net.minecraft.util.RandomSource random) {
        if (deathSpiritRootMask < 0) deathSpiritRootMask = CultivationService.rollSpiritRootMask(random);
        return deathSpiritRootMask;
    }
    public void clearDeathSpiritRootMask() { deathSpiritRootMask = -1; }
    public net.minecraft.nbt.CompoundTag reincarnationStoneRecords() { return reincarnationStoneRecords.copy(); }
    public void setReincarnationStoneRecords(net.minecraft.nbt.CompoundTag records) { reincarnationStoneRecords = records.copy(); }
    public net.minecraft.nbt.ListTag pendingReincarnationStones() { return pendingReincarnationStones.copy(); }
    public void setPendingReincarnationStones(net.minecraft.nbt.ListTag pending) { pendingReincarnationStones = pending.copy(); }
    public void accumulateInnerDemon(double points) {
        if (!Double.isFinite(points) || points<=0 || innerDemon>=100) return;
        double milli=demonSubMilli+points*1000;
        long whole=(long)Math.floor(milli+1e-9);
        innerDemon=Math.min(100,innerDemon+whole/1000.0);
        demonSubMilli=innerDemon>=100?0:Math.max(0,milli-whole);
    }
    public boolean characterInitialized() { return characterInitialized; }
    public void setCharacterInitialized(boolean value) { characterInitialized = value; }
    @Override public boolean cultivationUnlocked() { return cultivationUnlocked; }
    @Override public void setCultivationUnlocked(boolean value) { cultivationUnlocked = value; }
    private static double safe(double minimum, double value) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(1e12, value)) : minimum;
    }

    private int majorRealm;
    private int stage;
    private long cultivationProgress;
    private int progressMilliRemainder;
    private double meditationSubMilli;
    private long spiritualPowerMilli;
    private long spiritualPowerMaxMilli;
    private double powerSpeedBonus;
    private double merits;
    private double ferocious;
    private int comprehension = 1;
    private double innerDemon;
    private double breakthroughBarrier;
    private double defenseBonus;
    private final EnumSet<SpiritRootType> spiritRoots = EnumSet.noneOf(SpiritRootType.class);

    @Override public com.longju.xiantu.alchemy.MedicineData medicine() { return medicine; }
    @Override public com.longju.xiantu.spell.SpellData spells() { return spells; }
    @Override public com.longju.xiantu.gongfa.GongfaData gongfa() { return gongfa; }
    @Override public int majorRealm() { return majorRealm; }
    @Override public int stage() { return stage; }
    @Override public int realm() { return RealmRegistry.indexOf(majorRealm, stage); }
    @Override public int minorRealm() { return stage; }
    @Override public double progress() { return cultivationProgress + progressMilliRemainder / 1000.0D; }
    @Override public double progressMax() { return RealmRegistry.get(majorRealm, stage).progressCap(); }
    @Override public double power() { return spiritualPowerMilli / 1000.0D; }
    @Override public double powerMax() { return spiritualPowerMaxMilli / 1000.0D; }
    @Override public double powerSpeed() { return cultivationUnlocked ? RealmRegistry.basePowerRecovery(majorRealm, stage) + powerSpeedBonus : 0; }
    @Override public void setPowerSpeed(double value) {
        powerSpeedBonus = safe(0, value - RealmRegistry.basePowerRecovery(majorRealm, stage));
    }
    @Override public int spiritRootCount() { return spiritRoots.size(); }
    @Override public double merits() { return merits; }
    @Override public double ferocious() { return ferocious; }
    @Override public int comprehension() { return comprehension; }
    @Override public double innerDemon() { return innerDemon; }
    @Override public double breakthroughBarrier() { return breakthroughBarrier; }
    @Override public double healthBonus() { return cultivationUnlocked ? RealmRegistry.healthBonus(majorRealm, stage) : 0; }
    @Override public double attackBonus() { return cultivationUnlocked ? RealmRegistry.attackBonus(majorRealm, stage) : 0; }
    @Override public double defenseBonus() { return defenseBonus; }
    @Override public boolean hasSpiritRoot(SpiritRootType type) { return spiritRoots.contains(type); }

    @Override public void setRealm(int ordinal, int ignoredStage) {
        RealmDefinition definition = RealmRegistry.get(ordinal);
        setRealmStage(definition.majorIndex(), definition.stage());
    }

    @Override public void setRealmStage(int realm, int nextStage) {
        RealmDefinition definition = RealmRegistry.get(realm, nextStage);
        majorRealm = definition.majorIndex();
        stage = definition.stage();
        setProgress(progress());
    }

    @Override public void setProgress(double value) {
        meditationSubMilli = 0;
        double bounded = Math.min(progressMax(), safe(0, value));
        long milli = Math.round(bounded * 1000.0D);
        cultivationProgress = milli / 1000L;
        progressMilliRemainder = (int) (milli % 1000L);
    }

    @Override public void setPower(double value) {
        spiritualPowerMilli = Math.min(MAX_POWER_MILLI, Math.round(safe(0, value) * 1000.0D));
    }

    @Override public void setPowerMax(double value) {
        spiritualPowerMaxMilli = Math.min(MAX_POWER_MILLI, Math.round(safe(0, value) * 1000.0D));
    }

    @Override public void setMerits(double value) { merits = safe(0, value); }
    @Override public void setFerocious(double value) { ferocious = safe(0, value); }
    @Override public void setComprehension(int value) { comprehension = Math.max(1, Math.min(20, value)); }
    @Override public void setInnerDemon(double value) { innerDemon = safe(0, Math.min(100, value)); demonSubMilli=0; }
    @Override public void setBreakthroughBarrier(double value) { breakthroughBarrier = safe(0, value); }
    @Override public void setDefenseBonus(double value) { defenseBonus = safe(0, value); }
    @Override public void addPower(double amount) { setPower(power() + amount); }

    @Override public void addProgress(double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || progress() >= progressMax()) return;
        setProgress(progress() + amount);
    }

    /** Preserve sub-thousandth gains across ticks, saves and respawns; discard stage overflow. */
    public void accumulateMeditation(double points) {
        if (!Double.isFinite(points) || points <= 0) return;
        if (progress() >= progressMax()) { meditationSubMilli = 0; return; }
        double milli = meditationSubMilli + points * 1000;
        long wholeMilli = (long) Math.floor(milli + 1e-9);
        double residue = Math.max(0, milli - wholeMilli);
        if (wholeMilli > 0) addProgress(wholeMilli / 1000.0);
        meditationSubMilli = progress() >= progressMax() ? 0 : residue;
    }

    @Override public void addSpiritRoot(SpiritRootType type) {
        spiritRoots.add(type);
    }

    public void resetSpiritRoots() {
        spiritRoots.clear();
    }

    @Override public CultivationData copy() {
        PlayerCultivationData copy = new PlayerCultivationData();
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(CultivationData source) {
        physique = source.physique();
        physiqueInitialized = source instanceof PlayerCultivationData p && p.physiqueInitialized;
        deathPhysique = source instanceof PlayerCultivationData p ? p.deathPhysique : "";
        deathSpiritRootMask = source instanceof PlayerCultivationData p ? p.deathSpiritRootMask : -1;
        reincarnationStoneRecords = source instanceof PlayerCultivationData p ? p.reincarnationStoneRecords.copy() : new net.minecraft.nbt.CompoundTag();
        pendingReincarnationStones = source instanceof PlayerCultivationData p ? p.pendingReincarnationStones.copy() : new net.minecraft.nbt.ListTag();
        cultivationUnlocked = source.cultivationUnlocked();
        characterInitialized = source instanceof PlayerCultivationData p && p.characterInitialized;
        gongfa.copyFrom(source.gongfa());
        medicine.copyFrom(source.medicine());
        spells.copyFrom(source.spells());
        setRealmStage(source.majorRealm(), source.stage());
        setProgress(source.progress());
        meditationSubMilli = source instanceof PlayerCultivationData p ? p.meditationSubMilli : 0;
        setPower(source.power());
        setPowerMax(source.powerMax());
        setPowerSpeed(source.powerSpeed());
        setMerits(source.merits());
        setFerocious(source.ferocious());
        setComprehension(source.comprehension());
        setInnerDemon(source.innerDemon());
        demonSubMilli = source instanceof PlayerCultivationData p ? p.demonSubMilli : 0;
        setBreakthroughBarrier(source.breakthroughBarrier());
        setDefenseBonus(source.defenseBonus());
        spiritRoots.clear();
        for (SpiritRootType type : SpiritRootType.values()) if (source.hasSpiritRoot(type)) spiritRoots.add(type);
    }

    public void save(CompoundTag tag) {
        tag.putString("physique", physique.id);
        tag.putBoolean("physique_initialized", physiqueInitialized);
        tag.putString("death_physique", deathPhysique);
        tag.putInt("death_spirit_root_mask", deathSpiritRootMask);
        tag.putDouble("demon_sub_milli", demonSubMilli);
        tag.put("reincarnation_stone_records", reincarnationStoneRecords.copy());
        tag.put("pending_reincarnation_stones", pendingReincarnationStones.copy());
        tag.putBoolean("cultivation_unlocked", cultivationUnlocked);
        tag.putBoolean("character_initialized", characterInitialized);
        tag.put("gongfa", gongfa.save());
        tag.putInt("data_version", 3);
        tag.put("medicine", medicine.save());
        tag.put("spells", spells.save());
        tag.putInt("realm", majorRealm);
        tag.putInt("stage", stage);
        tag.putLong("cultivation_progress", cultivationProgress);
        tag.putInt("cultivation_progress_milli_remainder", progressMilliRemainder);
        tag.putDouble("meditation_sub_milli", meditationSubMilli);
        tag.putLong("spiritual_power_milli", spiritualPowerMilli);
        tag.putLong("spiritual_power_max_milli", spiritualPowerMaxMilli);
        tag.putDouble("power_speed_bonus", powerSpeedBonus);
        tag.putDouble("merits", merits);
        tag.putDouble("ferocious", ferocious);
        tag.putInt("comprehension", comprehension);
        tag.putDouble("inner_demon", innerDemon);
        tag.putDouble("breakthrough_barrier", breakthroughBarrier);
        // Realm attack is derived, not an accumulated/persisted upgrade counter.
        tag.remove("attack_bonus");
        tag.putDouble("defense_bonus", defenseBonus);
        for (SpiritRootType type : SpiritRootType.values()) tag.putBoolean("root_" + type.name().toLowerCase(), hasSpiritRoot(type));
    }

    public void load(CompoundTag tag) {
        physique = PhysiqueType.byId(tag.getString("physique"));
        physiqueInitialized = tag.getBoolean("physique_initialized");
        deathPhysique = tag.getString("death_physique");
        deathSpiritRootMask = tag.contains("death_spirit_root_mask") ? tag.getInt("death_spirit_root_mask") : -1;
        reincarnationStoneRecords = tag.getCompound("reincarnation_stone_records").copy();
        pendingReincarnationStones = tag.getList("pending_reincarnation_stones", net.minecraft.nbt.Tag.TAG_COMPOUND).copy();
        cultivationUnlocked = tag.getBoolean("cultivation_unlocked");
        characterInitialized = tag.getBoolean("character_initialized");
        gongfa.load(tag.getCompound("gongfa"));
        medicine.load(tag.getCompound("medicine"));
        spells.load(tag.getCompound("spells"));
        if (tag.getInt("data_version") >= 2) {
            setRealmStage(tag.getInt("realm"), tag.getInt("stage"));
            cultivationProgress = Math.max(0, Math.min((long) progressMax(), tag.getLong("cultivation_progress")));
            progressMilliRemainder = Math.max(0, Math.min(999, tag.getInt("cultivation_progress_milli_remainder")));
            if (progress() > progressMax()) setProgress(progressMax());
            double residue = tag.getDouble("meditation_sub_milli");
            meditationSubMilli = progress() >= progressMax() || !Double.isFinite(residue) ? 0 : Math.max(0, Math.min(.999999999, residue));
            spiritualPowerMilli = Math.max(0, Math.min(MAX_POWER_MILLI, tag.getLong("spiritual_power_milli")));
            spiritualPowerMaxMilli = Math.max(0, Math.min(MAX_POWER_MILLI, tag.getLong("spiritual_power_max_milli")));
        }
        if (tag.getInt("data_version") >= 3) {
            powerSpeedBonus = safe(0, tag.getDouble("power_speed_bonus"));
        } else if (tag.getInt("data_version") >= 2) {
            setPowerSpeed(tag.getDouble("power_speed"));
        }
        setMerits(tag.getDouble("merits"));
        setFerocious(tag.getDouble("ferocious"));
        setComprehension(tag.contains("comprehension") ? tag.getInt("comprehension") : 1);
        setInnerDemon(tag.getDouble("inner_demon"));
        double demonResidue=tag.getDouble("demon_sub_milli");
        demonSubMilli=innerDemon>=100 || !Double.isFinite(demonResidue)?0:Math.max(0,Math.min(.999999999,demonResidue));
        setBreakthroughBarrier(tag.getDouble("breakthrough_barrier"));
        setDefenseBonus(tag.getDouble("defense_bonus"));
        spiritRoots.clear();
        for (SpiritRootType type : SpiritRootType.values()) if (tag.getBoolean("root_" + type.name().toLowerCase())) spiritRoots.add(type);
    }
}
