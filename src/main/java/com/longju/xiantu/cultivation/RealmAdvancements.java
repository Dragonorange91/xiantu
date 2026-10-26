package com.longju.xiantu.cultivation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Permanent major-realm milestones. Mortal placeholder realm zero never grants Qi. */
public final class RealmAdvancements {
    public static final String[] IDS={"qi","foundation","golden_core","nascent_soul","spirit","void","unity","mahayana","tribulation"};
    private RealmAdvancements() {}
    public static void check(ServerPlayer player, CultivationData data) {
        if (!data.cultivationUnlocked() || player.isSpectator()) return;
        for (int i=0;i<=Math.min(data.majorRealm(),IDS.length-1);i++) {
            var advancement=player.server.getAdvancements().getAdvancement(new ResourceLocation("xiantu","cultivation/"+IDS[i]));
            if (advancement==null) continue;
            var progress=player.getAdvancements().getOrStartProgress(advancement);
            if (!progress.isDone()) for (String criterion:progress.getRemainingCriteria())
                player.getAdvancements().award(advancement,criterion);
        }
    }
}
