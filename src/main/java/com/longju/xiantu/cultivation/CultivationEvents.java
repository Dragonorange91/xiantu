package com.longju.xiantu.cultivation;

import com.longju.xiantu.XiantuMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.longju.xiantu.config.XiantuConfig;

public final class CultivationEvents {
    private static final ResourceLocation KEY = new ResourceLocation(XiantuMod.MOD_ID, "cultivation");
    private CultivationEvents() {}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) { CultivationCommands.register(event.getDispatcher()); }
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            CultivationCapability.Provider provider = new CultivationCapability.Provider();
            event.addCapability(KEY, provider);
            event.addListener(provider::invalidate);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        CultivationService.initialize(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ThunderTribulationService.onLogin(serverPlayer);
            InnerDemonService.syncCurrentLock(serverPlayer);
            serverPlayer.getCapability(CultivationCapability.DATA).ifPresent(data -> CultivationService.applyRealmAttributes(serverPlayer, data));
            com.longju.xiantu.gongfa.GongfaService.refresh(serverPlayer);
            sync(serverPlayer);
        }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(CultivationCapability.DATA).ifPresent(data -> CultivationService.applyRealmAttributes(serverPlayer, data));
            com.longju.xiantu.gongfa.GongfaService.refresh(serverPlayer);
            sync(serverPlayer);
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        com.longju.xiantu.refining.WeaponConditions.copyCooldowns(event.getOriginal(),event.getEntity());
        com.longju.xiantu.refining.QitianSkills.copy(event.getOriginal(),event.getEntity());
        com.longju.xiantu.refining.EquipmentConditions.copy(event.getOriginal(),event.getEntity(),event.isWasDeath());
        com.longju.xiantu.mining.SpiritVeinProgress.copy(event.getOriginal().getPersistentData(),
                event.getEntity().getPersistentData());
        event.getOriginal().getCapability(CultivationCapability.DATA).ifPresent(oldData ->
                event.getEntity().getCapability(CultivationCapability.DATA).ifPresent(newData -> {
                    if (newData instanceof PlayerCultivationData mutable) mutable.copyFrom(oldData);
                    if (event.isWasDeath()) {
                        newData.medicine().clearTemporary();
                        if (oldData instanceof PlayerCultivationData old && newData instanceof PlayerCultivationData restored)
                            CultivationService.settleOrdinaryDeath(event.getEntity(), old, restored);
                        if (oldData instanceof PlayerCultivationData old && newData instanceof PlayerCultivationData restored)
                            com.longju.xiantu.item.ReincarnationStoneItem.prepareDeath(old, restored, event.getEntity().getUUID());
                    }
                }));
        if (event.isWasDeath()) {
            var tag = event.getEntity().getPersistentData();
            tag.remove("xiantu_active_gongfa");
            tag.remove("xiantu_gongfa_signature");
            tag.remove("xiantu_applied_gongfa");
            tag.remove("xiantu_gongfa_effect_state");
            tag.putDouble("xiantu_gongfa_power_max_bonus", 0);
            tag.putDouble("xiantu_gongfa_power_speed_bonus", 0);
            tag.putDouble("xiantu_physique_power_max_bonus", 0);
            tag.putDouble("xiantu_pill_power_bonus", 0);
            tag.putDouble("xiantu_equipment_power_bonus", 0);
        } else {
            String activeGongfa = event.getOriginal().getPersistentData().getString("xiantu_active_gongfa");
            if (!activeGongfa.isEmpty()) event.getEntity().getPersistentData().putString("xiantu_active_gongfa", activeGongfa);
            event.getEntity().getPersistentData().putDouble("xiantu_gongfa_power_max_bonus",
                    event.getOriginal().getPersistentData().getDouble("xiantu_gongfa_power_max_bonus"));
            event.getEntity().getPersistentData().putDouble("xiantu_gongfa_power_speed_bonus",
                    event.getOriginal().getPersistentData().getDouble("xiantu_gongfa_power_speed_bonus"));
            event.getEntity().getPersistentData().putDouble("xiantu_physique_power_max_bonus",
                    event.getOriginal().getPersistentData().getDouble("xiantu_physique_power_max_bonus"));
            for (String key : java.util.List.of("xiantu_pill_power_bonus", "xiantu_equipment_power_bonus"))
                event.getEntity().getPersistentData().putDouble(key, event.getOriginal().getPersistentData().getDouble(key));
        }
        event.getOriginal().invalidateCaps();
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            com.longju.xiantu.gongfa.GongfaService.refresh(player);
            sync(player);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ThunderTribulationService.onLogout(player);
    }
    @SubscribeEvent public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ThunderTribulationService.endForDeath(player);
    }
    @SubscribeEvent public static void playerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.player.level().isClientSide
                || !event.player.isAlive() || !event.player.getCapability(CultivationCapability.DATA).isPresent()) return;
        // The first server tick repairs capabilities that were not ready during login.
        CultivationService.initialize(event.player);
        if (event.player.tickCount % XiantuConfig.POWER_CHECK_INTERVAL_TICKS.get() == 0) CultivationService.applyCultivationTick(event.player);
        if (event.player instanceof ServerPlayer serverPlayer) {
            com.longju.xiantu.alchemy.MedicineService.tick(serverPlayer);
            ThunderTribulationService.tick(serverPlayer);
            com.longju.xiantu.gongfa.GongfaService.tick(serverPlayer);
            BreakthroughService.tryAutomaticMinorBreakthrough(serverPlayer);
        }
        if (event.player.tickCount % 20 == 0 && event.player instanceof ServerPlayer serverPlayer) sync(serverPlayer);
    }
    public static void sync(ServerPlayer player) {
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            RealmAdvancements.check(player,data);
            int roots = 0;
            for (SpiritRootType root : SpiritRootType.values()) if (data.hasSpiritRoot(root)) roots |= 1 << root.ordinal();
            com.longju.xiantu.network.XiantuNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.longju.xiantu.network.CultivationSyncPacket(data.majorRealm(), data.stage(), Math.round(data.power() * 1000.0D), Math.round(data.powerMax() * 1000.0D), Math.round(data.progress() * 1000.0D), Math.round(data.progressMax()), roots, data.medicine().totalDoses(), data.medicine().toxicity(), data.cultivationUnlocked(), data.gongfa().save(),data.physique().id,com.longju.xiantu.alchemy.MedicineService.effect(player,"true_sight")>0));
        });
    }
}

