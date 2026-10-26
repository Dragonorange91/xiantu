package com.longju.xiantu.item;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.CultivationData;
import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.config.XiantuConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpiritStoneAbsorption {
    private static final int DURATION_TICKS = 40;
    private static final long POWER_SCALE = 1_000L;
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private static final String BACKLASH_PULSES = "xiantu_spirit_stone_backlash_pulses";

    private SpiritStoneAbsorption() {}

    public static boolean begin(ServerPlayer player, SpiritStoneTier tier, ItemStack stack) {
        CultivationData data = player.getCapability(CultivationCapability.DATA).orElse(null);
        long chargeMilli = SpiritStoneItem.chargeMilli(stack, tier);
        if (data == null || chargeMilli <= 0) {
            player.displayClientMessage(Component.translatable("message.xiantu.stone.invalid"), true);
            return false;
        }
        long powerMilli = Math.round(data.power() * POWER_SCALE);
        long powerCapMilli = Math.round(data.powerMax() * POWER_SCALE);
        int realmRank = SpiritStoneAbsorptionRules.stoneRankForRealm(data.majorRealm());
        SpiritStoneAbsorptionRules.Plan plan = SpiritStoneAbsorptionRules.evaluate(
                tier.rank(), chargeMilli, realmRank, powerMilli, powerCapMilli);
        switch (plan.decision()) {
            case FULL -> player.displayClientMessage(Component.translatable("message.xiantu.stone.full"), true);
            case INVALID -> player.displayClientMessage(Component.translatable("message.xiantu.stone.invalid"), true);
            case READY -> {
                ACTIVE.put(player.getUUID(), new Session(tier, plan, chargeMilli, player.position(),
                        player.level().dimension(), player.getAbilities().instabuild));
                if (plan.overRank()) {
                    player.displayClientMessage(Component.translatable("message.xiantu.stone.backlash"), false);
                }
                player.displayClientMessage(Component.translatable("message.xiantu.stone.absorbing"), true);
                return true;
            }
        }
        return false;
    }

    public static boolean canContinue(ServerPlayer player, SpiritStoneTier tier, ItemStack stack) {
        Session session = ACTIVE.get(player.getUUID());
        if (session == null || session.tier != tier || stack.getCount() != 1
                || !stack.is(com.longju.xiantu.registry.ModItems.getSpiritStone(tier))
                || !player.level().dimension().equals(session.dimension)
                || player.position().distanceToSqr(session.startPos) > 0.04D) {
            cancel(player, stack);
            return false;
        }
        return true;
    }

    public static void onStarted(ServerPlayer player, SpiritStoneTier tier) {
        Session session = ACTIVE.get(player.getUUID());
        if (session == null || session.tier != tier || !session.plan.overRank()) return;
        int realmRank = player.getCapability(CultivationCapability.DATA)
                .map(data -> SpiritStoneAbsorptionRules.stoneRankForRealm(data.majorRealm())).orElse(1);
        applyBacklashPulse(player, session, tier.rank() - realmRank);
    }

    public static boolean tick(ServerPlayer player, SpiritStoneTier tier, ItemStack stack, int elapsedTicks) {
        if (!canContinue(player, tier, stack)) return false;
        Session session = ACTIVE.get(player.getUUID());
        CultivationData data = player.getCapability(CultivationCapability.DATA).orElse(null);
        if (data == null) {
            cancel(player, stack);
            return false;
        }

        int currentRealmRank = SpiritStoneAbsorptionRules.stoneRankForRealm(data.majorRealm());
        boolean overRankNow = tier.rank() - currentRealmRank >= 2;
        if (overRankNow != session.plan.overRank()) {
            cancel(player, stack);
            return false;
        }

        int elapsed = Math.max(0, Math.min(DURATION_TICKS, elapsedTicks));
        int elapsedSinceLastTick = Math.max(0, elapsed - session.lastProcessedTick);
        session.lastProcessedTick = Math.max(session.lastProcessedTick, elapsed);
        long requestedDrain = SpiritStoneAbsorptionRules.absorptionAmountMilli(
                RealmRegistry.spiritStoneAbsorptionPerSecond(data.majorRealm(), data.stage()), elapsedSinceLastTick);
        requestedDrain = Math.min(requestedDrain,
                Math.max(0L, session.startChargeMilli - session.drainedMilli));
        long currentPower = Math.round(data.power() * POWER_SCALE);
        long powerCap = Math.round(data.powerMax() * POWER_SCALE);
        long availableRoom = Math.max(0L, powerCap - currentPower);
        long absorbed = Math.min(requestedDrain, availableRoom);
        if (session.plan.overRank()) {
            int rankDifference = tier.rank() - currentRealmRank;
            applyBacklash(player, session, elapsed, rankDifference);
        }
        if (absorbed > 0L) {
            data.setPower((currentPower + absorbed) / (double) POWER_SCALE);
            if (!session.creative) {
                ((SpiritStoneItem) stack.getItem()).setChargeMilli(stack,
                        Math.max(0L, session.startChargeMilli - session.drainedMilli - absorbed));
                player.getInventory().setChanged();
            }
            session.drainedMilli += absorbed;
            session.restoredMilli += absorbed;
        }

        if (elapsed == DURATION_TICKS || elapsed % 2 == 0) {
            long remainingCharge = session.creative
                    ? session.startChargeMilli : ((SpiritStoneItem) stack.getItem()).chargeMilli(stack);
            player.displayClientMessage(Component.translatable("message.xiantu.stone.absorbing_progress",
                    formatPower(remainingCharge), formatPower(session.startChargeMilli)), true);
        }
        return true;
    }

    public static ItemStack complete(ServerPlayer player, SpiritStoneTier tier, ItemStack stack) {
        if (!tick(player, tier, stack, DURATION_TICKS)) return stack;
        Session session = ACTIVE.remove(player.getUUID());
        if (session == null) return stack;

        long wasted = Math.max(0L, session.drainedMilli - session.restoredMilli);
        Component wastedText = wasted > 0L
                ? Component.translatable("message.xiantu.stone.wasted", formatPower(wasted))
                : Component.empty();
        player.displayClientMessage(Component.translatable("message.xiantu.stone.restored",
                formatPower(session.restoredMilli), wastedText), false);
        if (session.creative) return stack;
        return ((SpiritStoneItem) stack.getItem()).chargeMilli(stack) == 0L ? ItemStack.EMPTY : stack;
    }

    public static void cancel(ServerPlayer player) {
        cancel(player, player.getUseItem());
    }

    public static void cancel(ServerPlayer player, ItemStack stack) {
        Session session = ACTIVE.remove(player.getUUID());
        if (session != null) {
            player.displayClientMessage(Component.translatable("message.xiantu.stone.cancelled",
                    formatPower(session.drainedMilli),
                    formatPower(session.creative ? session.startChargeMilli
                            : Math.max(0L, session.startChargeMilli - session.drainedMilli))), true);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session session = ACTIVE.get(player.getUUID());
        if (session != null && !canContinue(player, session.tier, player.getUseItem())) player.stopUsingItem();
    }

    @SubscribeEvent
    public static void playerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ACTIVE.containsKey(player.getUUID())) {
            Session session = ACTIVE.get(player.getUUID());
            if (event.getSource() == session.backlashSource) return;
            cancel(player, player.getUseItem());
            player.stopUsingItem();
        }
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player, player.getUseItem());
    }

    private static String formatPower(long milliPower) {
        long whole = milliPower / POWER_SCALE;
        long fraction = milliPower % POWER_SCALE;
        if (fraction == 0) return Long.toString(whole);
        String digits = String.format(java.util.Locale.ROOT, "%03d", fraction).replaceFirst("0+$", "");
        return whole + "." + digits;
    }

    private static void applyBacklash(ServerPlayer player, Session session, int elapsed, int rankDifference) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, true));
        if (elapsed > session.lastBacklashTick && elapsed > 0 && elapsed % 10 == 0) {
            session.lastBacklashTick = elapsed;
            applyBacklashPulse(player, session, rankDifference);
        }
    }

    private static void applyBacklashPulse(ServerPlayer player, Session session, int rankDifference) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, true));
        DamageSource source = player.damageSources().magic();
        session.backlashSource = source;
        try {
            long pulses = player.getPersistentData().getLong(BACKLASH_PULSES);
            player.getPersistentData().putLong(BACKLASH_PULSES,
                    pulses == Long.MAX_VALUE ? pulses : Math.max(0L, pulses) + 1L);
            double damage = SpiritStoneAbsorptionRules.backlashDamage(
                    XiantuConfig.SPIRIT_STONE_BACKLASH_START_DAMAGE.get(),
                    XiantuConfig.SPIRIT_STONE_BACKLASH_DAMAGE_MULTIPLIER.get(), pulses, rankDifference);
            player.invulnerableTime = 0;
            player.hurt(source, (float) Math.min(Float.MAX_VALUE, damage));
        } finally {
            session.backlashSource = null;
        }
    }

    private static final class Session {
        private final SpiritStoneTier tier;
        private final SpiritStoneAbsorptionRules.Plan plan;
        private final long startChargeMilli;
        private final Vec3 startPos;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final boolean creative;
        private long drainedMilli;
        private long restoredMilli;
        private DamageSource backlashSource;
        private int lastBacklashTick;
        private int lastProcessedTick;

        private Session(SpiritStoneTier tier, SpiritStoneAbsorptionRules.Plan plan, long startChargeMilli,
                        Vec3 startPos, net.minecraft.resources.ResourceKey<Level> dimension, boolean creative) {
            this.tier = tier;
            this.plan = plan;
            this.startChargeMilli = startChargeMilli;
            this.startPos = startPos;
            this.dimension = dimension;
            this.creative = creative;
        }
    }
}
