package com.longju.xiantu.mining;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class SpiritVeinProgress {
    private static final String XP_KEY = "xiantu_spirit_vein_xp";
    private static final String LEVEL_KEY = "xiantu_spirit_vein_level";
    private static final int[] THRESHOLDS = {128, 1_024, 8_192};

    private SpiritVeinProgress() {}

    public static int level(ServerPlayer player) {
        return Math.max(0, Math.min(3, player.getPersistentData().getInt(LEVEL_KEY)));
    }

    public static int experience(ServerPlayer player) {
        return Math.max(0, player.getPersistentData().getInt(XP_KEY));
    }

    public static int nextThreshold(ServerPlayer player) {
        return nextThresholdForLevel(level(player));
    }

    public static int nextThresholdForLevel(int level) {
        return THRESHOLDS[Math.max(0, Math.min(2, level))];
    }

    public static int displayExperienceForLevel(int experience, int level) {
        return Math.min(Math.max(0, experience), nextThresholdForLevel(level));
    }

    public static int levelForExperience(int experience) {
        int total = Math.max(0, experience);
        int level = 0;
        while (level < 3 && total >= THRESHOLDS[level]) level++;
        return level;
    }

    public static void award(ServerPlayer player, int amount) {
        if (amount <= 0 || player.getAbilities().instabuild) return;
        CompoundTag data = player.getPersistentData();
        int previousLevel = level(player);
        int total = Math.min(Integer.MAX_VALUE, experience(player) + amount);
        data.putInt(XP_KEY, total);
        int level = levelForExperience(total);
        if (level != previousLevel) {
            data.putInt(LEVEL_KEY, level);
            player.displayClientMessage(Component.translatable("message.xiantu.vein_level_up", level)
                    .withStyle(ChatFormatting.AQUA), false);
        }
    }

    public static void copy(CompoundTag from, CompoundTag to) {
        if (from.contains(XP_KEY)) to.putInt(XP_KEY, Math.max(0, from.getInt(XP_KEY)));
        if (from.contains(LEVEL_KEY)) to.putInt(LEVEL_KEY, Math.max(0, Math.min(3, from.getInt(LEVEL_KEY))));
    }
}
