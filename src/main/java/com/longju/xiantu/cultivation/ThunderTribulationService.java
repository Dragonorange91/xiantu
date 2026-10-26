package com.longju.xiantu.cultivation;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import java.util.UUID;

public final class ThunderTribulationService {
    public static final String STATE_KEY = "xiantu_thunder_attempt";
    private static final String KEY = STATE_KEY;
    private static final String PHASE_PREPARATION = "preparation";
    private static final String PHASE_WARNING = "warning";
    private static final String PHASE_AFTER = "after";
    private static final String PHASE_OBSERVE = "observe";
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("xiantu", "tribulation_lightning"));

    private ThunderTribulationService() {}

    public static boolean active(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompound(KEY);
        return tag.getBoolean("active");
    }

    public static boolean canBegin(ServerPlayer player, int realm, int stage) {
        return !active(player) && ThunderTribulationRules.requiresTribulation(true, realm, stage);
    }

    public static boolean begin(ServerPlayer player, int realm, int stage, double successChance) {
        int strikes = ThunderTribulationRules.strikes(realm);
        if (active(player) || !ThunderTribulationRules.requiresTribulation(true, realm, stage)) return false;

        UUID attempt = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", true);
        tag.putUUID("id", attempt);
        tag.putUUID("player", player.getUUID());
        tag.putInt("realm", realm);
        tag.putInt("stage", stage);
        tag.putInt("target_realm", realm + 1);
        tag.putInt("target_stage", 0);
        tag.putDouble("success_chance", successChance);
        tag.putDouble("damage_ratio", ThunderTribulationRules.damageRatio(realm));
        tag.putDouble("raw_damage", ThunderTribulationRules.rawDamage(realm, stage));
        tag.putInt("strike_count", strikes);
        tag.putInt("strike_index", 0);
        tag.putString("phase", PHASE_PREPARATION);
        tag.putInt("phase_ticks", 100);
        tag.putInt("total_ticks", ThunderTribulationRules.totalTicks(realm));
        tag.putInt("materials_recorded", 0);
        tag.put("materials", new ListTag());
        player.getPersistentData().put(KEY, tag);
        player.getPersistentData().putInt("xiantu_thunder_ticks", tag.getInt("total_ticks"));
        player.sendSystemMessage(Component.literal("天劫降临，五秒后落下第一道雷劫；雷劫会持续追随你直到结束"));
        player.playNotifySound(SoundEvents.BEACON_AMBIENT, SoundSource.WEATHER, 0.45F, 0.72F);
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (!active(player)) return;
        CompoundTag tag = state(player);
        if (!player.isAlive()) {
            endWithoutFailure(player);
            return;
        }
        int total = Math.max(0, tag.getInt("total_ticks") - 1);
        tag.putInt("total_ticks", total);
        player.getPersistentData().putInt("xiantu_thunder_ticks", total);
        int phaseTicks = Math.max(0, tag.getInt("phase_ticks") - 1);
        tag.putInt("phase_ticks", phaseTicks);
        if (total % 10 == 0) emitCloud(player, tag);
        switch (tag.getString("phase")) {
            case PHASE_PREPARATION -> {
                if (phaseTicks == 0) startWarning(player, tag);
            }
            case PHASE_WARNING -> {
                if (phaseTicks == 0) {
                    strike(player, tag);
                    tag.putString("phase", PHASE_AFTER);
                    tag.putInt("phase_ticks", 80);
                }
            }
            case PHASE_AFTER -> {
                if (phaseTicks == 0) {
                    int nextStrike = tag.getInt("strike_index") + 1;
                    tag.putInt("strike_index", nextStrike);
                    if (nextStrike >= tag.getInt("strike_count")) {
                        tag.putString("phase", PHASE_OBSERVE);
                        tag.putInt("phase_ticks", 40);
                        player.sendSystemMessage(Component.literal("最后一道雷劫已落下，保持存活完成结算"));
                    } else startWarning(player, tag);
                }
            }
            case PHASE_OBSERVE -> {
                if (phaseTicks == 0) succeed(player);
            }
            default -> fail(player, "天劫记录无效");
        }
        if (active(player)) player.getPersistentData().put(KEY, tag);
    }

    private static void startWarning(ServerPlayer player, CompoundTag tag) {
        int strike = tag.getInt("strike_index");
        tag.putString("phase", PHASE_WARNING);
        tag.putInt("phase_ticks", 40);
        player.sendSystemMessage(Component.literal("第" + (strike + 1) + "道雷劫即将落下，雷劫必中，无法躲避"));
        player.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.WEATHER, 0.75F, 1.35F);
    }

    private static void emitCloud(ServerPlayer player, CompoundTag tag) {
        double x = player.getX();
        double y = player.getY() + 4.25D;
        double z = player.getZ();
        var level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 5, 2.0D, 0.35D, 2.0D, 0.003D);
    }

    private static void strike(ServerPlayer player, CompoundTag tag) {
        var level = player.serverLevel();
        var lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.setVisualOnly(true);
            lightning.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
            level.addFreshEntity(lightning);
        }
        player.playNotifySound(SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.8F, 1.1F);
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DAMAGE_TYPE));
        player.hurt(source, (float) tag.getDouble("raw_damage"));
        player.sendSystemMessage(Component.literal("雷劫击中，承受" + Math.round(tag.getDouble("raw_damage")) + "点原始雷伤"));
    }

    public static void onLogout(ServerPlayer player) {
        if (!active(player)) return;
        CompoundTag tag = state(player);
        tag.putBoolean("paused", true);
        tag.putLong("paused_at", player.serverLevel().getServer().overworld().getGameTime());
        player.getPersistentData().put(KEY, tag);
    }

    public static void onLogin(ServerPlayer player) {
        if (!active(player)) return;
        CompoundTag tag = state(player);
        if (tag.getBoolean("paused")) {
            long now = player.serverLevel().getServer().overworld().getGameTime();
            long offlineTicks = Math.max(0, now - tag.getLong("paused_at"));
            if (offlineTicks > 600) {
                fail(player, "断线超过三十秒");
                return;
            }
            tag.putBoolean("paused", false);
            player.getPersistentData().put(KEY, tag);
            player.sendSystemMessage(Component.literal("天劫继续，剩余" + tag.getInt("total_ticks") / 20 + "秒"));
        }
    }

    public static void endForDeath(ServerPlayer player) { endWithoutFailure(player); }

    private static void succeed(ServerPlayer player) {
        CompoundTag tag = state(player);
        if (!active(player)) return;
        int realm = tag.getInt("realm");
        int stage = tag.getInt("stage");
        PlayerCultivationData data = player.getCapability(CultivationCapability.DATA)
                .filter(PlayerCultivationData.class::isInstance).map(PlayerCultivationData.class::cast).orElse(null);
        if (data == null || data.majorRealm() != realm || data.stage() != stage || data.progress() < data.progressMax()) {
            fail(player, "突破数据在渡劫期间发生变化");
            return;
        }
        clear(player, "success");
        BreakthroughService.commit(player, data, realm + 1, 0);
        player.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.WEATHER, 0.8F, 1.2F);
        player.sendSystemMessage(Component.literal("天劫完成，突破至 "
                + RealmRegistry.get(realm + 1, 0).majorName() + "·" + RealmRegistry.get(realm + 1, 0).minorName()));
        CultivationEvents.sync(player);
    }

    private static void fail(ServerPlayer player, String reason) {
        if (!active(player)) return;
        CompoundTag tag = state(player);
        int realm = tag.getInt("realm");
        int stage = tag.getInt("stage");
        clear(player, "failure");
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            if (!(data instanceof PlayerCultivationData mutable)
                    || mutable.majorRealm() != realm || mutable.stage() != stage) return;
            mutable.setProgress(BreakthroughRules.failureProgress((long) mutable.progressMax()));
            PhysiqueService.addInnerDemon(mutable, BreakthroughRules.failureInnerDemon(true));
            CultivationEvents.sync(player);
        });
        player.sendSystemMessage(Component.literal("天劫失败（" + reason + "），境界保留，修为回退至七成，心魔加深"));
        player.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.WEATHER, 0.7F, 0.65F);
    }

    private static void endWithoutFailure(ServerPlayer player) { clear(player, "death"); }

    private static CompoundTag state(ServerPlayer player) { return player.getPersistentData().getCompound(KEY); }

    private static void clear(ServerPlayer player, String result) {
        CompoundTag attempt = state(player);
        if (attempt.hasUUID("id")) {
            player.getPersistentData().putUUID("xiantu_thunder_last_attempt", attempt.getUUID("id"));
            player.getPersistentData().putString("xiantu_thunder_last_result", result);
        }
        player.getPersistentData().remove(KEY);
        player.getPersistentData().remove("xiantu_thunder_ticks");
    }
}
