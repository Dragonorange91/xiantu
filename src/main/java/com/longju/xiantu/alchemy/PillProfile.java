package com.longju.xiantu.alchemy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Validated item metadata, shared by tooltip and server consumption. */
public record PillProfile(double progress, double power, double maxPower, double speed, double barrier,
                          int minimumRealm, int maximumRealm, int permanentLimit, double toxicity) {
    public boolean allowsRealm(int realm) { return realm >= minimumRealm && realm <= maximumRealm; }
    public boolean hasPermanentGains() { return maxPower > 0 || speed > 0 || barrier > 0; }
    public boolean allowsPermanent(MedicineData data, String pill) { return data.permanentDoses(pill) < permanentLimit; }
    public PillProfile withTag(CompoundTag tag) {
        return new PillProfile(gain(tag, "base_gain", progress), gain(tag, "power_gain", power),
                gain(tag, "power_max_gain", maxPower), gain(tag, "power_speed_gain", speed), gain(tag, "barrier_gain", barrier),
                integer(tag, "minimum_realm", minimumRealm, 0, 40), integer(tag, "maximum_realm", maximumRealm, 0, 40),
                integer(tag, "permanent_limit", permanentLimit, 0, 10000), number(tag, "toxicity", toxicity, 100));
    }
    private static double gain(CompoundTag tag, String key, double fallback) { return number(tag, key, fallback, 1_000_000); }
    private static double number(CompoundTag tag, String key, double fallback, double maximum) {
        if (!tag.contains(key)) return fallback;
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) return 0;
        double value = tag.getDouble(key);
        return Double.isFinite(value) && value >= 0 && value <= maximum ? value : 0;
    }
    private static int integer(CompoundTag tag, String key, int fallback, int minimum, int maximum) {
        if (!tag.contains(key)) return fallback;
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) return fallback;
        double value = tag.getDouble(key);
        return Double.isFinite(value) && value == Math.rint(value) && value >= minimum && value <= maximum ? (int) value : fallback;
    }
}
