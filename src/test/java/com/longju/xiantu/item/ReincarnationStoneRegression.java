package com.longju.xiantu.item;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class ReincarnationStoneRegression {
    private static int checks;

    public static void main(String[] args) {
        UUID owner = UUID.randomUUID();
        UUID instance = UUID.randomUUID();
        CompoundTag mortal = record(owner, instance, false);
        CompoundTag cultivator = record(owner, instance, true);
        check(ReincarnationStoneRules.recordedRealmText(mortal).equals("凡人"), "Locked character is shown as mortal");
        check(ReincarnationStoneRules.recordedRealmText(cultivator).equals("练气·1层"), "Unlocked realm label uses current stage");
        check(ReincarnationStoneRules.selectRestoreSource(mortal, cultivator, instance, owner, false) == cultivator,
                "Character record remains the primary restore source");
        check(ReincarnationStoneRules.selectRestoreSource(mortal, new CompoundTag(), instance, owner, true) == mortal,
                "Exact item record can recover when a pending death transaction exists");
        check(ReincarnationStoneRules.selectRestoreSource(mortal, new CompoundTag(), instance, owner, false) == null,
                "Item record alone cannot bypass a missing death transaction");
        check(ReincarnationStoneRules.selectRestoreSource(mortal, new CompoundTag(), UUID.randomUUID(), owner, true) == null,
                "Different item instance cannot restore");
        check(ReincarnationStoneRules.selectRestoreSource(mortal, new CompoundTag(), instance, UUID.randomUUID(), true) == null,
                "Different owner cannot restore");
        check(!ReincarnationStoneRules.matchesRecord(new CompoundTag(), instance, owner),
                "Incomplete record is rejected without reading absent UUID fields");
        check(ReincarnationStoneRules.remainingAfterConsume(1) == 0,
                "Successful restore consumes the held single stone");
        check(ReincarnationStoneRules.remainingAfterConsume(2) == 1,
                "Successful restore consumes exactly one stone");
        check(ReincarnationStoneRules.isHigherRecord(true, 0, 0, false, 0, 0),
                "Unlocking cultivation advances beyond a mortal record at the same realm index");
        check(!ReincarnationStoneRules.isHigherRecord(false, 0, 0, true, 0, 0),
                "Losing cultivation qualification does not count as record growth");
        check(ReincarnationStoneRules.isHigherRecord(true, 0, 1, true, 0, 0),
                "Progress growth at the same realm can update the stone");
        check(ReincarnationStoneRules.isHigherRecord(true, 1, 0, true, 0, 100),
                "A higher realm can update the stone regardless of old progress");
        System.out.println("Reincarnation stone regression: " + checks + " passed");
    }

    private static CompoundTag record(UUID owner, UUID instance, boolean unlocked) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("owner", owner);
        tag.putUUID("instance", instance);
        tag.putInt("realm", 0);
        tag.putBoolean("unlocked", unlocked);
        tag.put("gongfa", new CompoundTag());
        return tag;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("[Reincarnation stone PASS] " + message);
    }
}
