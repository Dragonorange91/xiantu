package com.longju.xiantu.gongfa;

import net.minecraft.nbt.CompoundTag;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Learned methods survive a replacement, but not ordinary death. Pending work is never resumed. */
public final class GongfaData {
    public static final class Progress {
        private int level, experience;
        private double milliRemainder;
        public int level() { return level; }
        public int experience() { return experience; }
        public double milliRemainder() { return milliRemainder; }
        public void clear() { level = 0; experience = 0; milliRemainder = 0; }
        public boolean add(double points) {
            if (!Double.isFinite(points) || points <= 0 || level >= 9) return false;
            double total = milliRemainder + points * 1000;
            int whole = (int) Math.min(2670, Math.floor(total / 1000 + 1e-9));
            milliRemainder = Math.max(0, total - whole * 1000);
            experience += whole;
            int before = level;
            while (level < 9 && experience >= GongfaRules.nextExperience(level)) {
                experience -= GongfaRules.nextExperience(level++);
            }
            if (level == 9) { experience = 0; milliRemainder = 0; }
            return before != level;
        }
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("level", level); tag.putInt("experience", experience);
            tag.putDouble("milli_remainder", milliRemainder); return tag;
        }
        private void load(CompoundTag tag) {
            level = Math.max(0, Math.min(9, tag.getInt("level")));
            experience = level == 9 ? 0 : Math.max(0, Math.min(GongfaRules.nextExperience(level)-1, tag.getInt("experience")));
            double residue = tag.getDouble("milli_remainder");
            milliRemainder = level == 9 || !Double.isFinite(residue) ? 0 : Math.max(0, Math.min(999.999999, residue));
        }
    }
    private final Map<String, Progress> learned = new LinkedHashMap<>();
    private String active = "";
    private UUID lastTransaction;
    public boolean knows(String id) { return learned.containsKey(id); }
    public boolean learn(String id) {
        if (GongfaRegistry.get(id) == null || GongfaRegistry.get(id).tierIndex() == 0 || knows(id)) return false;
        learned.put(id, new Progress()); return true;
    }
    public String active() { return active; }
    public Progress progress(String id) { return learned.get(id); }
    public Progress activeProgress() { return progress(active); }
    public int activeLevel() { return activeProgress() == null ? 0 : activeProgress().level(); }
    public boolean commit(String id, UUID transaction) {
        if (!knows(id) || id.equals(active) || transaction == null || transaction.equals(lastTransaction)) return false;
        if (activeProgress() != null) activeProgress().clear();
        progress(id).clear(); active = id; lastTransaction = transaction; return true;
    }
    public void clear() { learned.clear(); active = ""; lastTransaction = null; }
    public void copyFrom(GongfaData source) { load(source.save()); }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(), methods = new CompoundTag();
        learned.forEach((id, progress) -> methods.put(id, progress.save()));
        tag.put("learned", methods); tag.putString("active", active);
        if (lastTransaction != null) tag.putUUID("last_transaction", lastTransaction);
        return tag;
    }
    public void load(CompoundTag tag) {
        clear(); CompoundTag methods = tag.getCompound("learned");
        for (String id : methods.getAllKeys()) {
            if (GongfaRegistry.get(id) == null || GongfaRegistry.get(id).tierIndex() == 0) continue;
            Progress progress = new Progress(); progress.load(methods.getCompound(id)); learned.put(id, progress);
        }
        String candidate = tag.getString("active"); active = knows(candidate) ? candidate : "";
        if (tag.hasUUID("last_transaction")) lastTransaction = tag.getUUID("last_transaction");
    }
}
