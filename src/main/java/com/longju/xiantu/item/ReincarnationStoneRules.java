package com.longju.xiantu.item;

import com.longju.xiantu.cultivation.RealmRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

final class ReincarnationStoneRules {
    private static final String RECORDER = "owner";
    private static final String INSTANCE_ID = "instance";
    private static final String REALM = "realm";
    private static final String UNLOCKED = "unlocked";
    private static final String GONGFA = "gongfa";

    private ReincarnationStoneRules() {}

    static CompoundTag selectRestoreSource(CompoundTag itemRecord, CompoundTag characterRecord,
                                           UUID instance, UUID owner, boolean hasPendingDeath) {
        if (matchesRecord(characterRecord, instance, owner)) return characterRecord;
        return hasPendingDeath && matchesRecord(itemRecord, instance, owner) ? itemRecord : null;
    }

    static boolean matchesRecord(CompoundTag record, UUID instance, UUID owner) {
        return record != null && record.hasUUID(INSTANCE_ID) && record.hasUUID(RECORDER)
                && record.getUUID(INSTANCE_ID).equals(instance) && record.getUUID(RECORDER).equals(owner)
                && record.contains(REALM, Tag.TAG_INT)
                && record.contains(UNLOCKED, Tag.TAG_BYTE)
                && record.contains(GONGFA, Tag.TAG_COMPOUND);
    }

    static String recordedRealmText(CompoundTag record) {
        if (!record.getBoolean(UNLOCKED)) return "凡人";
        var realm = RealmRegistry.get(record.getInt(REALM));
        return realm.majorName() + "·" + realm.minorName();
    }

    static int remainingAfterConsume(int count) {
        return Math.max(0, count - 1);
    }

    static boolean isHigherRecord(boolean currentUnlocked, int currentRealm, double currentProgress,
                                  boolean recordedUnlocked, int recordedRealm, double recordedProgress) {
        if (currentUnlocked != recordedUnlocked) return currentUnlocked;
        return currentRealm > recordedRealm || (currentRealm == recordedRealm && currentProgress > recordedProgress);
    }
}
