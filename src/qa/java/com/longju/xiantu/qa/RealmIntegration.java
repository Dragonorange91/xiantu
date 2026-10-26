package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.CultivationEvents;
import com.longju.xiantu.cultivation.PlayerCultivationData;
import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.cultivation.CultivationService;
import com.longju.xiantu.cultivation.BreakthroughService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import com.longju.xiantu.item.CultivationProbeItem;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkHooks;

public final class RealmIntegration {
    private RealmIntegration() {}

    public static int verifyAttributes(ServerPlayer player) {
        if (!isQaWorld(player)) return 0;
        PlayerCultivationData data = (PlayerCultivationData) player.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
        CompoundTag saved = new CompoundTag();
        data.save(saved);
        CompoundTag persistent=player.getPersistentData().copy();
        String active = player.getPersistentData().getString("xiantu_active_gongfa");
        float health = player.getHealth();
        var mainHand = player.getMainHandItem().copy();
        var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        var maximumHealth = player.getAttribute(Attributes.MAX_HEALTH);
        double vanillaAttack = attack.getBaseValue(), vanillaHealth = maximumHealth.getBaseValue();
        java.util.UUID fixtureId = java.util.UUID.fromString("a9e5a1e5-6a4a-4a1d-9b37-4b5c3e0e1999");
        int checks = 0;
        try {
            data.setPhysique(com.longju.xiantu.cultivation.PhysiqueType.ORDINARY);
            data.setCultivationUnlocked(true);data.setCharacterInitialized(true);data.gongfa().clear();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.getPersistentData().remove("xiantu_active_gongfa");
            attack.setBaseValue(1);
            maximumHealth.setBaseValue(20);
            com.longju.xiantu.gongfa.GongfaService.refresh(player);
            double extraAttack = attack.getValue() - (1 + data.attackBonus());
            double extraHealth = maximumHealth.getValue() - (20 + data.healthBonus());
            for (int index = 0; index < 41; index++) {
                var realm = RealmRegistry.get(index);
                data.setRealmStage(realm.majorIndex(), realm.stage());
                CultivationService.applyRealmAttributes(player, data);
                require(Math.abs(attack.getValue() - extraAttack - RealmRegistry.baseAttack(realm.majorIndex(), realm.stage())) < .0001,
                        "Attack attribute at stage " + index);
                require(Math.abs(maximumHealth.getValue() - extraHealth - RealmRegistry.baseHealth(realm.majorIndex(), realm.stage())) < .0001,
                        "Health attribute at stage " + index);
                double before = attack.getValue();
                CultivationService.applyRealmAttributes(player, data);
                require(attack.getValue() == before, "Repeated apply does not stack at stage " + index);
                checks += 3;
            }
            attack.addTransientModifier(new AttributeModifier(fixtureId, "qa_equipment_attack", 7, AttributeModifier.Operation.ADDITION));
            CultivationService.applyRealmAttributes(player, data);
            require(Math.abs(attack.getValue() - extraAttack - 340) < .0001, "Equipment attack remains additive to 333 realm attack");
            attack.removeModifier(fixtureId);
            data.setRealmStage(0, 0);
            player.setHealth(100);
            CultivationService.applyRealmAttributes(player, data);
            require(Math.abs(attack.getValue() - extraAttack - 6) < .0001, "Rollback immediately removes high-rank attack");
            require(player.getHealth() <= player.getMaxHealth(), "Rollback clamps health to the new cap");
            CompoundTag roundTrip = new CompoundTag(); data.save(roundTrip);
            data.setRealmStage(8, 3); data.load(roundTrip);
            CultivationService.applyRealmAttributes(player, data);
            require(Math.abs(attack.getValue() - extraAttack - 6) < .0001, "NBT reload derives attack instead of restoring stale cumulative values");
            checks += 4;
            player.sendSystemMessage(Component.literal("[Realm attributes QA] All " + checks + " assertions passed"));
            org.slf4j.LoggerFactory.getLogger(RealmIntegration.class).info("[Realm attributes QA] All {} assertions passed", checks);
            return checks;
        } catch (RuntimeException | LinkageError error) {
            org.slf4j.LoggerFactory.getLogger(RealmIntegration.class).error("[Realm attributes QA] FAIL", error);
            throw error;
        } finally {
            attack.removeModifier(fixtureId);
            attack.setBaseValue(vanillaAttack); maximumHealth.setBaseValue(vanillaHealth);
            data.load(saved);
            for(String key:java.util.List.copyOf(player.getPersistentData().getAllKeys())) player.getPersistentData().remove(key);
            player.getPersistentData().merge(persistent);
            player.getPersistentData().putString("xiantu_active_gongfa", active);
            player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
            com.longju.xiantu.gongfa.GongfaService.refresh(player);
            player.setHealth(Math.min(health, player.getMaxHealth()));
            CultivationEvents.sync(player);
        }
    }

    public static int verifyBreakthrough(ServerPlayer player) {
        if (!isQaWorld(player)) return 0;
        PlayerCultivationData data=(PlayerCultivationData)player.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
        CompoundTag saved=new CompoundTag(); data.save(saved);
        CompoundTag persistent=player.getPersistentData().copy();
        String active=player.getPersistentData().getString("xiantu_active_gongfa");
        float health=player.getHealth(); ItemStack hand=player.getMainHandItem().copy();
        var attack=player.getAttribute(Attributes.ATTACK_DAMAGE); var maximumHealth=player.getAttribute(Attributes.MAX_HEALTH);
        double vanillaAttack=attack.getBaseValue(),vanillaHealth=maximumHealth.getBaseValue();
        int checks=0;
        try {
            data.setPhysique(com.longju.xiantu.cultivation.PhysiqueType.ORDINARY);
            data.setCultivationUnlocked(true); data.setCharacterInitialized(true); data.gongfa().clear();
            data.setRealmStage(0,0); data.setInnerDemon(0); data.setComprehension(20); data.setProgress(0);
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            player.getPersistentData().putLong("xiantu_breakthrough_cooldown",0);
            player.getPersistentData().remove("xiantu_active_gongfa");
            com.longju.xiantu.gongfa.GongfaService.refresh(player);
            require(!BreakthroughService.tryBreakthrough(player) && data.stage()==0 && data.progress()==0
                    && BreakthroughService.cooldownTicks(player)==0,"Incomplete progress is rejected without starting cooldown"); checks++;
            data.setProgress(data.progressMax());
            require(BreakthroughService.successChance(data)==1,"Prepared chance reaches deterministic one-hundred-percent cap"); checks++;
            require(BreakthroughService.tryBreakthrough(player),"Valid stage breakthrough succeeds through server transaction"); checks++;
            require(data.majorRealm()==0 && data.stage()==1 && data.progress()==0,"Successful stage transition clears progress without spending stored cultivation"); checks++;
            require(BreakthroughService.cooldownTicks(player)==200,"Accepted breakthrough starts ten-second cooldown"); checks++;
            require(!BreakthroughService.tryBreakthrough(player) && data.stage()==1 && data.progress()==0,
                    "Cooldown rejects repeat requests without duplicate progression"); checks++;
            player.sendSystemMessage(Component.literal("[Breakthrough QA] All "+checks+" assertions passed"));
            org.slf4j.LoggerFactory.getLogger(RealmIntegration.class).info("[Breakthrough QA] All {} assertions passed",checks);
            return checks;
        } catch(RuntimeException|LinkageError error) {
            org.slf4j.LoggerFactory.getLogger(RealmIntegration.class).error("[Breakthrough QA] FAIL",error); throw error;
        } finally {
            attack.setBaseValue(vanillaAttack); maximumHealth.setBaseValue(vanillaHealth);
            data.load(saved);
            for(String key:java.util.List.copyOf(player.getPersistentData().getAllKeys()))player.getPersistentData().remove(key);
            player.getPersistentData().merge(persistent); player.getPersistentData().putString("xiantu_active_gongfa",active);
            player.setItemInHand(InteractionHand.MAIN_HAND,hand); com.longju.xiantu.gongfa.GongfaService.refresh(player);
            CultivationService.applyRealmAttributes(player,data); player.setHealth(Math.min(health,player.getMaxHealth())); CultivationEvents.sync(player);
        }
    }

    private static void require(boolean ok, String label) {
        if (!ok) throw new IllegalStateException("[Realm attributes QA] " + label);
    }

    public static int openHighRealmStatus(ServerPlayer player) {
        if (!isQaWorld(player)) return 0;
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            data.setCultivationUnlocked(true);
            data.setRealmStage(8, 3);
            data.setProgress(data.progressMax());
            data.setPowerMax(300_000);
            data.setPower(275_000);
        });
        com.longju.xiantu.gongfa.GongfaService.refresh(player);
        CultivationEvents.sync(player);
        ItemStack probe = new ItemStack(ModItems.CULTIVATION_PROBE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, probe);
        ((CultivationProbeItem) probe.getItem()).use(player.level(), player, InteractionHand.MAIN_HAND);
        player.sendSystemMessage(Component.literal("[Realm QA] Opened high-realm status with 300,000 power cap for overflow check"));
        return 1;
    }

    public static int reset(ServerPlayer player) {
        if (!isQaWorld(player)) return 0;
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            if (!(data instanceof PlayerCultivationData mutable)) return;
            mutable.setCultivationUnlocked(true);mutable.setCharacterInitialized(true);
            mutable.setRealmStage(0, 0);
            mutable.setProgress(0);
            mutable.setPowerMax(100);
            mutable.setPower(100);
        });
        com.longju.xiantu.gongfa.GongfaService.refresh(player);
        CultivationEvents.sync(player);
        player.getInventory().clearOrCountMatchingItems(stack -> stack.is(ModItems.CULTIVATION_PROBE.get()), -1, player.inventoryMenu.getCraftSlots());
        player.sendSystemMessage(Component.literal("[Realm QA] Player cultivation data reset to Qi Refining layer one"));
        return 1;
    }

    private static boolean isQaWorld(ServerPlayer player) {
        String worldName = player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
        if (worldName.startsWith("Xiantu-QA")) return true;
        player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
        return false;
    }
}
