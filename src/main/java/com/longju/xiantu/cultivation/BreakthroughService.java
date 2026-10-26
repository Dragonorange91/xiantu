package com.longju.xiantu.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BreakthroughService {
    private static final String COOLDOWN_KEY = "xiantu_breakthrough_cooldown";
    private static final int COOLDOWN_TICKS = 200;

    private BreakthroughService() {}

    public static boolean tryAutomaticMinorBreakthrough(ServerPlayer player) {
        return player.getCapability(CultivationCapability.DATA).map(data -> {
            if (ThunderTribulationService.active(player) || cooldownTicks(player) > 0
                    || com.longju.xiantu.gongfa.GongfaService.remainingTicks(player) > 0
                    || !BreakthroughRules.shouldAutomaticallyBreakthrough(data)
                    || !com.longju.xiantu.gongfa.GongfaService.breakthroughDenial(data).isEmpty()) return false;
            return tryBreakthrough(player);
        }).orElse(false);
    }

    public static boolean tryBreakthrough(ServerPlayer player) {
        return player.getCapability(CultivationCapability.DATA).map(data -> {
            if (!(data instanceof PlayerCultivationData mutable)) return false;
            if (ThunderTribulationService.active(player)) {
                player.sendSystemMessage(Component.literal("天劫尚未结束，不能再次突破"));
                return false;
            }
            long gameTime = player.serverLevel().getServer().overworld().getGameTime();
            if (player.getPersistentData().getLong(COOLDOWN_KEY) > gameTime) {
                player.sendSystemMessage(Component.literal("突破冷却中"));
                return false;
            }
            if (data.progress() < data.progressMax()) {
                player.sendSystemMessage(Component.literal("当前阶段修为尚未圆满"));
                return false;
            }
            if (RealmRegistry.isFinalStage(data.majorRealm(), data.stage())) {
                player.sendSystemMessage(Component.literal("你已达到当前修炼体系的最高境界"));
                return false;
            }
            boolean majorBreakthrough = BreakthroughRules.isMajorBreakthrough(data.majorRealm(), data.stage());
            if (ThunderTribulationRules.requiresTribulation(majorBreakthrough, data.majorRealm(), data.stage())
                    && !ThunderTribulationService.canBegin(player, data.majorRealm(), data.stage())) {
                player.sendSystemMessage(Component.literal("当前无法开始天劫"));
                return false;
            }
            if (com.longju.xiantu.gongfa.GongfaService.remainingTicks(player) > 0) return false;
            String gongfaDenial = com.longju.xiantu.gongfa.GongfaService.breakthroughDenial(data);
            if (!gongfaDenial.isEmpty()) { player.sendSystemMessage(Component.literal(gongfaDenial)); return false; }

            player.getPersistentData().putLong(COOLDOWN_KEY, gameTime + COOLDOWN_TICKS);

            if (InnerDemonRules.requiresChallenge(majorBreakthrough, data.majorRealm(), data.innerDemon())) {
                boolean started = InnerDemonService.begin(player, mutable);
                if (!started) player.getPersistentData().putLong(COOLDOWN_KEY, gameTime);
                return false;
            }

            return resolve(player, mutable, majorBreakthrough, BreakthroughRules.successChance(data));
        }).orElse(false);
    }

    static boolean resolve(ServerPlayer player, PlayerCultivationData mutable, boolean majorBreakthrough, double chance) {
        return resolve(player, mutable, majorBreakthrough, chance, true);
    }

    static boolean resolveAfterInnerDemon(ServerPlayer player, PlayerCultivationData mutable) {
        boolean majorBreakthrough = BreakthroughRules.isMajorBreakthrough(mutable.majorRealm(), mutable.stage());
        if (ThunderTribulationRules.requiresTribulation(majorBreakthrough, mutable.majorRealm(), mutable.stage())
                && !ThunderTribulationService.canBegin(player, mutable.majorRealm(), mutable.stage())) {
            player.sendSystemMessage(Component.literal("心魔战胜利，但当前无法开始天劫；本次突破未掷骰，也未晋阶"));
            return false;
        }
        boolean succeeded = resolve(player, mutable, majorBreakthrough, BreakthroughRules.successChance(mutable), false);
        if (succeeded) {
            if (ThunderTribulationService.active(player))
                player.sendSystemMessage(Component.literal("心魔战胜利，心魔值降低30；突破判定已通过，天劫开始，尚未晋阶"));
            else {
                RealmDefinition next = RealmRegistry.get(mutable.majorRealm(), mutable.stage());
                player.sendSystemMessage(Component.literal("心魔战胜利，心魔值降低30；突破成功至 " + next.majorName() + "·" + next.minorName()));
            }
        } else {
            player.sendSystemMessage(Component.literal("心魔战胜利，心魔值降低30；重掷突破失败，修为回退至阶段上限七成，心魔增加20"));
        }
        return succeeded;
    }

    private static boolean resolve(ServerPlayer player, PlayerCultivationData mutable, boolean majorBreakthrough, double chance, boolean announce) {
            chance = Math.min(1, chance + com.longju.xiantu.alchemy.MedicineService.effect(player,"understanding")*.02
                + com.longju.xiantu.alchemy.PillChoiceMenu.bonus(player,true));
            if (player.getRandom().nextDouble() >= chance) {
                mutable.setProgress(BreakthroughRules.failureProgress((long) mutable.progressMax()));
                PhysiqueService.addInnerDemon(mutable,BreakthroughRules.failureInnerDemon(majorBreakthrough));
                if (announce) player.sendSystemMessage(Component.literal("突破失败，修为回退至当前阶段上限的七成，心魔加深"));
                CultivationEvents.sync(player);
                return false;
            }

            int oldRealm = mutable.majorRealm();
            int nextRealm = majorBreakthrough ? oldRealm + 1 : oldRealm;
            int nextStage = majorBreakthrough ? 0 : mutable.stage() + 1;
            if (ThunderTribulationRules.requiresTribulation(majorBreakthrough, oldRealm, mutable.stage())) {
                boolean started = ThunderTribulationService.begin(player, oldRealm, mutable.stage(), chance);
                if (started && announce) player.sendSystemMessage(Component.literal("突破判定通过，进入天劫；渡劫成功后才会晋升"));
                return started;
            }
            commit(player, mutable, nextRealm, nextStage);
            RealmDefinition definition = RealmRegistry.get(nextRealm, nextStage);
            if (announce) player.sendSystemMessage(Component.literal("突破至 " + definition.majorName() + "·" + definition.minorName()));
            return true;
    }

    static void commit(ServerPlayer player, PlayerCultivationData mutable, int nextRealm, int nextStage) {
        int oldRealm = mutable.majorRealm();
        long oldPowerCap = RealmRegistry.get(oldRealm, mutable.stage()).powerCap();
        long nextPowerCap = RealmRegistry.get(nextRealm, nextStage).powerCap();
        boolean majorBreakthrough = nextRealm != oldRealm;
        mutable.setRealmStage(nextRealm, nextStage);
        mutable.setProgress(0);
        if (nextPowerCap > oldPowerCap) mutable.setPowerMax(mutable.powerMax() + nextPowerCap - oldPowerCap);
        if (majorBreakthrough) mutable.setDefenseBonus(mutable.defenseBonus() + defenseGain(nextRealm));
        CultivationService.applyRealmAttributes(player, mutable);
        com.longju.xiantu.gongfa.GongfaService.refresh(player);
        CultivationEvents.sync(player);
    }

    public static double successChance(ServerPlayer player) {
        return Math.min(1,BreakthroughRules.successChance(com.longju.xiantu.alchemy.MedicineService.data(player))
            +com.longju.xiantu.alchemy.MedicineService.effect(player,"understanding")*.02+com.longju.xiantu.alchemy.PillChoiceMenu.bonus(player,false));
    }
    public static double successChance(CultivationData data) {
        return BreakthroughRules.successChance(data);
    }

    public static int cooldownTicks(ServerPlayer player) {
        long gameTime = player.serverLevel().getServer().overworld().getGameTime();
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE,
                player.getPersistentData().getLong(COOLDOWN_KEY) - gameTime));
    }

    private static double defenseGain(int realm) { return realm <= 1 ? 0.5 : realm <= 3 ? 1 : 1.5; }
}
