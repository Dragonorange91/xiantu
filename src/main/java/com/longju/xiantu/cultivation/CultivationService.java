package com.longju.xiantu.cultivation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;
import com.longju.xiantu.config.XiantuConfig;

public final class CultivationService {
    private static final UUID HEALTH_ID = UUID.fromString("a9e5a1e5-6a4a-4a1d-9b37-4b5c3e0e1001");
    private static final UUID ATTACK_ID = UUID.fromString("a9e5a1e5-6a4a-4a1d-9b37-4b5c3e0e1002");
    private static final UUID ARMOR_ID = UUID.fromString("a9e5a1e5-6a4a-4a1d-9b37-4b5c3e0e1003");
    private static final UUID PHYSIQUE_HEALTH_ID = UUID.fromString("a9e5a1e5-6a4a-4a1d-9b37-4b5c3e0e1004");
    private static final UUID PHYSIQUE_ARMOR_ID = UUID.fromString("a9e5a1e5-6a4a-4a1d-9b37-4b5c3e0e1005");
    private CultivationService() {}

    public static int rollSpiritRootCount(int percentileRoll) {
        int roll = Math.max(0, Math.min(99, percentileRoll));
        return roll < 5 ? 1 : roll < 20 ? 2 : roll < 50 ? 3 : roll < 80 ? 4 : 5;
    }

    public static int rollSpiritRootMask(net.minecraft.util.RandomSource random) {
        int count = rollSpiritRootCount(random.nextInt(100));
        SpiritRootType[] roots = SpiritRootType.values();
        java.util.List<SpiritRootType> shuffled = new java.util.ArrayList<>(java.util.List.of(roots));
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            java.util.Collections.swap(shuffled, i, j);
        }
        int mask = 0;
        for (int i = 0; i < count; i++) mask |= 1 << shuffled.get(i).ordinal();
        return mask;
    }

    public static void settleOrdinaryDeath(Player player, PlayerCultivationData original,
                                           PlayerCultivationData restored) {
        restored.setRealmStage(0, 0);
        restored.setProgress(0);
        restored.setPowerMax(restored.cultivationUnlocked() ? RealmRegistry.get(0, 0).powerCap() : 0);
        restored.setPowerSpeed(CultivationConfig.BASE_POWER_SPEED);
        restored.setPower(0);
        restored.gongfa().clear();
        restored.resetSpiritRoots();
        int rootMask = original.deathSpiritRootMask(player.getRandom());
        for (SpiritRootType root : SpiritRootType.values()) {
            if ((rootMask & (1 << root.ordinal())) != 0) restored.addSpiritRoot(root);
        }
        restored.clearDeathSpiritRootMask();
        PhysiqueService.deathClone(player, original, restored);
    }

    public static void initialize(Player player) {
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            if (data instanceof PlayerCultivationData mutable) PhysiqueService.initialize(player,mutable);
            if (data instanceof PlayerCultivationData mutable && !mutable.characterInitialized()) {
                mutable.setCharacterInitialized(true);
                mutable.setRealm(0, 0);
                mutable.setPowerMax(data.cultivationUnlocked() ? RealmRegistry.get(data.majorRealm(), data.stage()).powerCap() : 0);
                mutable.setPowerSpeed(CultivationConfig.BASE_POWER_SPEED);
                mutable.setPower(0);
                if (data.spiritRootCount() == 0) {
                    mutable.resetSpiritRoots();
                    int mask = rollSpiritRootMask(player.getRandom());
                    for (SpiritRootType root : SpiritRootType.values())
                        if ((mask & (1 << root.ordinal())) != 0) mutable.addSpiritRoot(root);
                }
            }
            if (player.tickCount <= 1) applyRealmAttributes(player, data);
        });
    }
    public static void applyCultivationTick(Player player) {
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            if (!data.cultivationUnlocked()) return;
            if (data.power() < data.powerMax() && !(player.getVehicle() instanceof com.longju.xiantu.spell.SpiritSwordEntity)) {
                double recovery=data.powerSpeed() * (1+com.longju.xiantu.refining.EquipmentEffects.recoveryBonus(player)) * XiantuConfig.POWER_CHECK_INTERVAL_TICKS.get() / 20.0D * (com.longju.xiantu.alchemy.MedicineService.injured(player,"meridian")?.7:1) * (com.longju.xiantu.alchemy.MedicineService.injured(player,"miasma")?.9:1);
                // Ordinary regeneration fills the reservoir; it must never manufacture overflow backlash.
                data.setPower(Math.min(data.powerMax(),data.power()+recovery));
            }
            if (data.power() > data.powerMax() * XiantuConfig.POWER_OVERFLOW_RATIO.get()) {
                player.hurt(player.damageSources().magic(), (float) (player.getMaxHealth() * XiantuConfig.POWER_OVERFLOW_DAMAGE_RATIO.get()));
                data.setPower(data.powerMax());
            } else if (data.power() > data.powerMax()) data.setPower(data.powerMax());
        });
    }

    public static void applyRealmAttributes(Player player, CultivationData data) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        addModifier(serverPlayer, Attributes.MAX_HEALTH, HEALTH_ID, "xiantu_realm_health", data.healthBonus());
        addModifier(serverPlayer, Attributes.ATTACK_DAMAGE, ATTACK_ID, "xiantu_realm_attack", data.attackBonus());
        addModifier(serverPlayer, Attributes.ARMOR, ARMOR_ID, "xiantu_realm_defense", data.defenseBonus());
        var health=serverPlayer.getAttribute(Attributes.MAX_HEALTH);
        if (health!=null) {
            health.removeModifier(PHYSIQUE_HEALTH_ID);
            if (data.physique().healthBonus!=0) health.addPermanentModifier(new AttributeModifier(
                    PHYSIQUE_HEALTH_ID,"xiantu_physique_health",data.physique().healthBonus,AttributeModifier.Operation.MULTIPLY_BASE));
        }
        addModifier(serverPlayer,Attributes.ARMOR,PHYSIQUE_ARMOR_ID,"xiantu_physique_armor",data.physique().armor);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void addModifier(ServerPlayer player, Attribute attribute, UUID id, String name, double amount) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }
}
