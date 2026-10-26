package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.entity.InnerDemonEntity;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Disposable-world challenge smoke test; every player/entity mutation is restored in finally. */
public final class InnerDemonIntegration {
    private InnerDemonIntegration() {}

    public static int run(ServerPlayer player) {
        String world = player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .normalize().getFileName().toString();
        if (!world.startsWith("Xiantu-QA")) {
            player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
            return 0;
        }
        PlayerCultivationData data = (PlayerCultivationData) player.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
        CompoundTag savedData = new CompoundTag(); data.save(savedData);
        CompoundTag savedPersistent = player.getPersistentData().copy();
        GameType savedMode = player.gameMode.getGameModeForPlayer();
        float savedHealth = player.getHealth();
        var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        double savedAttack = attack.getBaseValue();
        List<ItemStack> equipment = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) equipment.add(player.getItemBySlot(slot).copy());
        int checks = 0;
        try {
            player.setGameMode(GameType.SURVIVAL);
            data.setCultivationUnlocked(true); data.setCharacterInitialized(true);
            data.setRealmStage(3, 3); data.setProgress(data.progressMax()); data.setInnerDemon(60); data.setComprehension(20);
            data.gongfa().clear(); data.gongfa().learn("tai_qing_hun_yuan_jing");
            data.gongfa().commit("tai_qing_hun_yuan_jing", java.util.UUID.randomUUID());
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD); sword.enchant(Enchantments.SHARPNESS, 3);
            player.setItemSlot(EquipmentSlot.MAINHAND, sword);
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            attack.setBaseValue(1000);
            player.getPersistentData().putLong("xiantu_breakthrough_cooldown", 0);
            player.getPersistentData().remove("xiantu_thunder_ticks");
            GongfaService.refresh(player);
            CultivationService.applyRealmAttributes(player, data);
            double[] expected = {player.getAttributeValue(Attributes.MAX_HEALTH), player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                    player.getAttributeValue(Attributes.ARMOR), player.getAttributeValue(Attributes.MOVEMENT_SPEED)};

            require(!BreakthroughService.tryBreakthrough(player) && InnerDemonService.active(player), "Sixty inner demon begins the major-breakthrough challenge"); checks++;
            InnerDemonEntity phantom = player.serverLevel().getEntitiesOfClass(InnerDemonEntity.class,
                    new AABB(player.blockPosition()).inflate(32), entity -> player.getUUID().equals(entity.ownerId())).stream().findFirst().orElseThrow();
            net.minecraft.world.entity.LivingEntity phantomLiving = phantom;
            require(ItemStack.matches(phantomLiving.getMainHandItem(), sword), "Phantom receives the enchanted main-hand item with NBT"); checks++;
            require(ItemStack.matches(phantomLiving.getOffhandItem(), player.getOffhandItem()), "Phantom receives the offhand item"); checks++;
            require(ItemStack.matches(phantomLiving.getItemBySlot(EquipmentSlot.HEAD), player.getItemBySlot(EquipmentSlot.HEAD))
                    && ItemStack.matches(phantomLiving.getItemBySlot(EquipmentSlot.CHEST), player.getItemBySlot(EquipmentSlot.CHEST))
                    && ItemStack.matches(phantomLiving.getItemBySlot(EquipmentSlot.LEGS), player.getItemBySlot(EquipmentSlot.LEGS))
                    && ItemStack.matches(phantomLiving.getItemBySlot(EquipmentSlot.FEET), player.getItemBySlot(EquipmentSlot.FEET)), "Phantom receives all four armor pieces"); checks++;
            for (EquipmentSlot slot : EquipmentSlot.values()) require(phantom.mirroredEquipmentDropChance(slot) == 0, "Mirrored slot cannot drop: " + slot);
            checks += EquipmentSlot.values().length;
            net.minecraft.world.entity.ai.attributes.Attribute[] attrs = {Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.MOVEMENT_SPEED};
            String[] attrNames = {"max_health", "attack_damage", "armor", "movement_speed"};
            for (int i=0;i<attrs.length;i++) {
                double actual = phantomLiving.getAttributeValue(attrs[i]);
                double target = Math.max(.1D, expected[i]*.8D);
                require(Math.abs(actual-target)<.02D, "Phantom effective attribute is 80 percent with vanilla minimum: " + attrNames[i]
                        + " expected=" + target + " actual=" + actual);
            }
            checks += attrs.length;

            phantomLiving.setHealth(1);
            require(phantomLiving.hurt(player.damageSources().playerAttack(player), 1000), "Only the owner can defeat the phantom in the fixture"); checks++;
            require(!InnerDemonService.active(player), "Victory resolves the unique challenge and releases the inventory lock"); checks++;
            require(data.innerDemon() <= 50, "Victory reduces inner demon before the single resumed breakthrough roll"); checks++;
            // A successful resumed major breakthrough now starts the deferred tribulation transaction.
            player.getPersistentData().remove(ThunderTribulationService.STATE_KEY);
            player.getPersistentData().remove("xiantu_thunder_ticks");

            data.setRealmStage(3, 3); data.setProgress(data.progressMax()); data.setInnerDemon(60);
            player.getPersistentData().putLong("xiantu_breakthrough_cooldown", 0);
            CultivationService.applyRealmAttributes(player, data);
            require(!BreakthroughService.tryBreakthrough(player) && InnerDemonService.active(player), "Nascent Soul major breakthrough starts a fresh challenge"); checks++;
            InnerDemonEntity expiring = player.serverLevel().getEntitiesOfClass(InnerDemonEntity.class,
                    new AABB(player.blockPosition()).inflate(32), entity -> player.getUUID().equals(entity.ownerId())).stream().findFirst().orElseThrow();
            net.minecraft.world.entity.Entity expiringEntity = expiring;
            java.util.UUID expiringId = expiringEntity.getUUID();
            expiring.bind(player.getUUID(), expiring.transactionId(), player.serverLevel().getGameTime() - 1);
            expiringEntity.tick();
            require(!InnerDemonService.active(player) && player.serverLevel().getEntity(expiringId) == null, "Timeout clears the transaction and removes the phantom"); checks++;
            require(data.progress() == BreakthroughRules.failureProgress((long) data.progressMax()) && data.innerDemon() == 70,
                    "Timeout applies the challenge-failure settlement exactly once"); checks++;
            InnerDemonService.timeout(expiring);
            require(data.innerDemon() == 70, "Repeated timeout cleanup cannot settle the transaction twice"); checks++;

            data.setProgress(data.progressMax()); data.setInnerDemon(60);
            player.getPersistentData().putLong("xiantu_breakthrough_cooldown", 0);
            player.setHealth(player.getMaxHealth() * .30F);
            CultivationService.applyRealmAttributes(player, data);
            require(!BreakthroughService.tryBreakthrough(player) && InnerDemonService.active(player), "Low-health fixture starts a new heart-demon challenge"); checks++;
            InnerDemonEntity attacker = player.serverLevel().getEntitiesOfClass(InnerDemonEntity.class,
                    new AABB(player.blockPosition()).inflate(32), entity -> player.getUUID().equals(entity.ownerId())).stream().findFirst().orElseThrow();
            float healthBeforeHit = player.getHealth();
            float boundaryDamage = healthBeforeHit - player.getMaxHealth() * InnerDemonRules.FAILURE_HEALTH_RATIO;
            net.minecraftforge.event.entity.living.LivingDamageEvent lowHealthHit = new net.minecraftforge.event.entity.living.LivingDamageEvent(
                    player, player.damageSources().mobAttack(attacker), boundaryDamage + 1.0F);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(lowHealthHit);
            require(!InnerDemonService.active(player), "Crossing below twenty percent resolves the challenge as a failure"); checks++;
            require(Math.abs(lowHealthHit.getAmount() - boundaryDamage) < .01F, "Lethal heart-demon damage is capped at the twenty-percent boundary"); checks++;
            player.setHealth(Math.max(0.0F, healthBeforeHit - lowHealthHit.getAmount()));
            require(player.isAlive() && Math.abs(player.getHealth() - player.getMaxHealth() * .20F) < .02F,
                    "Low-health failure leaves the player alive at twenty percent health"); checks++;
            require(data.progress() == BreakthroughRules.failureProgress((long) data.progressMax()) && data.innerDemon() == 70,
                    "Low-health failure applies the normal challenge penalty once"); checks++;
            player.sendSystemMessage(Component.literal("[Inner Demon QA] All " + checks + " assertions passed"));
            org.slf4j.LoggerFactory.getLogger(InnerDemonIntegration.class).info("[Inner Demon QA] All {} assertions passed", checks);
            return checks;
        } catch (RuntimeException | LinkageError error) {
            org.slf4j.LoggerFactory.getLogger(InnerDemonIntegration.class).error("[Inner Demon QA] FAIL", error);
            throw error;
        } finally {
            try {
                for (InnerDemonEntity phantom : player.serverLevel().getEntitiesOfClass(InnerDemonEntity.class,
                        new AABB(player.blockPosition()).inflate(64), entity -> player.getUUID().equals(entity.ownerId()))) {
                    net.minecraft.world.entity.Entity entity = phantom;
                    entity.discard();
                }
            } catch (RuntimeException | LinkageError cleanupError) {
                org.slf4j.LoggerFactory.getLogger(InnerDemonIntegration.class).error("[Inner Demon QA] Phantom cleanup failed", cleanupError);
            } finally {
                restorePlayer(player, savedData, savedPersistent, savedMode, savedHealth, attack, savedAttack, equipment);
            }
        }
    }

    private static void restorePlayer(ServerPlayer player, CompoundTag savedData, CompoundTag savedPersistent, GameType savedMode,
                                      float savedHealth, net.minecraft.world.entity.ai.attributes.AttributeInstance attack,
                                      double savedAttack, List<ItemStack> equipment) {
        for (String key : List.copyOf(player.getPersistentData().getAllKeys())) player.getPersistentData().remove(key);
        player.getPersistentData().merge(savedPersistent);
        player.getCapability(CultivationCapability.DATA).ifPresent(current -> {
            ((PlayerCultivationData) current).load(savedData);
            CultivationService.applyRealmAttributes(player, current);
        });
        attack.setBaseValue(savedAttack);
        EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i=0;i<slots.length;i++) player.setItemSlot(slots[i], equipment.get(i));
        player.setGameMode(savedMode);
        player.setHealth(Math.min(savedHealth, player.getMaxHealth()));
        com.longju.xiantu.network.XiantuNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new com.longju.xiantu.network.InnerDemonStatePacket(false));
        GongfaService.refresh(player);
        CultivationEvents.sync(player);
    }

    private static void require(boolean ok, String label) {
        if (!ok) throw new IllegalStateException("[Inner Demon QA FAIL] " + label);
    }
}
