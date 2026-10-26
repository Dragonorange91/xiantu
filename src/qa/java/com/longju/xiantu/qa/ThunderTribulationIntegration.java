package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Disposable-world runtime checks for persisted tribulation transactions. */
public final class ThunderTribulationIntegration {
    private ThunderTribulationIntegration() {}

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
        float savedAbsorption = player.getAbsorptionAmount();
        boolean savedInvulnerable = player.isInvulnerable();
        LivingEntity livingPlayer = player;
        java.util.List<MobEffectInstance> savedEffects = livingPlayer.getActiveEffects().stream()
                .map(MobEffectInstance::new).toList();
        ServerLevel originalLevel = player.serverLevel();
        double originalX = player.getX(), originalY = player.getY(), originalZ = player.getZ();
        float originalYaw = player.getYRot(), originalPitch = player.getXRot();
        BlockPos ceilingPos = null;
        BlockState ceilingState = null;
        int checks = 0;
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.setInvulnerable(true);
            data.setCultivationUnlocked(true); data.setCharacterInitialized(true);
            data.setRealmStage(1, 3); data.setProgress(data.progressMax()); data.setInnerDemon(0);
            player.getPersistentData().remove(ThunderTribulationService.STATE_KEY);
            player.getPersistentData().remove("xiantu_thunder_ticks");
            require(!ThunderTribulationService.begin(player, 0, RealmRegistry.STAGE_COUNTS[0] - 1, .75D),
                    "Qi Refining-to-Foundation breakthrough cannot start a tribulation"); checks++;
            require(ThunderTribulationService.canBegin(player, 1, RealmRegistry.STAGE_COUNTS[1] - 1),
                    "Foundation-to-Golden-Core breakthrough is eligible anywhere"); checks++;
            require(ThunderTribulationService.begin(player, 1, 3, .75D), "Valid Foundation-to-Golden-Core attempt starts"); checks++;
            require(data.majorRealm() == 1 && data.stage() == 3 && data.progress() == data.progressMax(),
                    "Realm and full progress remain unchanged while the attempt is pending"); checks++;
            for (int tick = 0; tick < 100; tick++) ThunderTribulationService.tick(player);
            require(player.getPersistentData().getCompound(ThunderTribulationService.STATE_KEY).getString("phase").equals("warning"),
                    "Five-second cloud preparation advances to the first warning"); checks++;
            CompoundTag serialized = player.getPersistentData().getCompound(ThunderTribulationService.STATE_KEY).copy();
            player.getPersistentData().remove(ThunderTribulationService.STATE_KEY);
            player.getPersistentData().put(ThunderTribulationService.STATE_KEY, serialized);
            require(ThunderTribulationService.active(player) && serialized.getInt("phase_ticks") == 40,
                    "The active attempt and warning countdown survive NBT serialization"); checks++;
            for (int tick = 100; tick < ThunderTribulationRules.totalTicks(1); tick++) ThunderTribulationService.tick(player);
            require(data.majorRealm() == 2 && data.stage() == 0 && data.progress() == 0,
                    "Only the completed three-strike attempt commits the realm once"); checks++;
            require(!ThunderTribulationService.active(player)
                    && player.getPersistentData().getString("xiantu_thunder_last_result").equals("success"),
                    "Success clears the transaction with a terminal result"); checks++;
            ThunderTribulationService.tick(player);
            require(data.majorRealm() == 2 && data.stage() == 0, "Repeated tick after completion cannot promote twice"); checks++;

            data.setRealmStage(1, 3); data.setProgress(data.progressMax()); data.setInnerDemon(0);
            require(ThunderTribulationService.begin(player, 1, 3, .75D), "A new attempt can start after terminal cleanup"); checks++;
            for (int tick = 0; tick < 100; tick++) ThunderTribulationService.tick(player);
            CompoundTag warning = player.getPersistentData().getCompound(ThunderTribulationService.STATE_KEY);
            warning.putInt("phase_ticks", 1);
            player.setAbsorptionAmount(0.0F);
            player.setInvulnerable(false);
            player.setHealth(player.getMaxHealth());
            double strikeX = player.getX(), strikeY = player.getY(), strikeZ = player.getZ();
            player.teleportTo(strikeX + 128.0D, strikeY + 32.0D, strikeZ);
            float healthBeforeStrike = player.getHealth();
            ThunderTribulationService.tick(player);
            require(player.getHealth() < healthBeforeStrike,
                    "A guaranteed lightning strike hits after moving far above and away from the launch point"); checks++;
            DamageSource source = new DamageSource(player.serverLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(ThunderTribulationService.DAMAGE_TYPE));
            require(!source.is(DamageTypeTags.BYPASSES_ARMOR), "Tribulation damage does not bypass armor"); checks++;
            require(!source.is(DamageTypeTags.BYPASSES_EFFECTS)
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    && !source.is(DamageTypeTags.BYPASSES_SHIELD),
                    "Tribulation damage does not bypass effects, invulnerability, or shield checks"); checks++;
            AttributeInstance armor = livingPlayer.getAttribute(Attributes.ARMOR);
            require(armor != null, "Player exposes the vanilla armor attribute"); checks++;
            Entity damageTarget = player;
            player.hurtTime = 0;
            player.invulnerableTime = 0;
            livingPlayer.setHealth(livingPlayer.getMaxHealth());
            damageTarget.hurt(source, 10.0F);
            float baseDamage = livingPlayer.getMaxHealth() - livingPlayer.getHealth();
            AttributeModifier armorModifier = new AttributeModifier(java.util.UUID.randomUUID(),
                    "xiantu.qa.tribulation_armor", 20.0D, AttributeModifier.Operation.ADDITION);
            try {
                armor.addTransientModifier(armorModifier);
                player.hurtTime = 0;
                player.invulnerableTime = 0;
                livingPlayer.setHealth(livingPlayer.getMaxHealth());
                damageTarget.hurt(source, 10.0F);
                float armoredDamage = livingPlayer.getMaxHealth() - livingPlayer.getHealth();
                require(baseDamage > 0.0F && armoredDamage > 0.0F && armoredDamage < baseDamage,
                        "Vanilla armor reduces tribulation damage through the normal hurt pipeline"); checks++;
            } finally {
                armor.removeModifier(armorModifier);
            }
            livingPlayer.removeAllEffects();
            player.hurtTime = 0;
            player.invulnerableTime = 0;
            livingPlayer.setHealth(livingPlayer.getMaxHealth());
            damageTarget.hurt(source, 10.0F);
            float effectBaseDamage = livingPlayer.getMaxHealth() - livingPlayer.getHealth();
            livingPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false, false));
            player.hurtTime = 0;
            player.invulnerableTime = 0;
            livingPlayer.setHealth(livingPlayer.getMaxHealth());
            damageTarget.hurt(source, 10.0F);
            float resistedDamage = livingPlayer.getMaxHealth() - livingPlayer.getHealth();
            require(effectBaseDamage > 0.0F && resistedDamage > 0.0F && resistedDamage < effectBaseDamage,
                    "Resistance effects reduce tribulation damage through the normal hurt pipeline"); checks++;
            livingPlayer.removeAllEffects();
            player.teleportTo(strikeX, strikeY, strikeZ);
            ServerLevel strikeLevel = player.serverLevel();
            for (int offset = 3; offset <= 8; offset++) {
                BlockPos candidate = player.blockPosition().above(offset);
                if (candidate.getY() < strikeLevel.getMaxBuildHeight() && strikeLevel.isEmptyBlock(candidate)) {
                    ceilingPos = candidate;
                    ceilingState = strikeLevel.getBlockState(candidate);
                    break;
                }
            }
            require(ceilingPos != null, "QA fixture has a free block position above the player"); checks++;
            strikeLevel.setBlock(ceilingPos, Blocks.BEDROCK.defaultBlockState(), 3);
            CompoundTag blockedStrike = player.getPersistentData().getCompound(ThunderTribulationService.STATE_KEY);
            blockedStrike.putString("phase", "warning");
            blockedStrike.putInt("phase_ticks", 1);
            player.hurtTime = 0;
            player.invulnerableTime = 0;
            player.setHealth(player.getMaxHealth());
            float healthBeforeBlockedStrike = player.getHealth();
            ThunderTribulationService.tick(player);
            require(player.getHealth() < healthBeforeBlockedStrike,
                    "A solid block above the player does not block the guaranteed strike"); checks++;
            strikeLevel.setBlock(ceilingPos, ceilingState, 3);
            ceilingPos = null;
            ceilingState = null;
            ThunderTribulationService.onLogout(player);
            ThunderTribulationService.onLogin(player);
            require(ThunderTribulationService.active(player), "Reconnection inside the grace period resumes the saved attempt"); checks++;
            CompoundTag paused = player.getPersistentData().getCompound(ThunderTribulationService.STATE_KEY);
            paused.putBoolean("paused", true);
            paused.putLong("paused_at", player.getServer().overworld().getGameTime() - 601);
            ThunderTribulationService.onLogin(player);
            require(!ThunderTribulationService.active(player) && data.progress() == BreakthroughRules.failureProgress((long) data.progressMax())
                    && data.innerDemon() == 20, "Reconnect beyond thirty seconds settles one major-breakthrough failure"); checks++;

            data.setRealmStage(1, 3); data.setProgress(data.progressMax()); data.setInnerDemon(0);
            require(ThunderTribulationService.begin(player, 1, 3, .75D), "Death-boundary attempt starts"); checks++;
            ThunderTribulationService.endForDeath(player);
            require(!ThunderTribulationService.active(player) && data.progress() == data.progressMax() && data.innerDemon() == 0,
                    "Death removes the attempt without overriding unified death settlement"); checks++;

            data.setRealmStage(1, 3); data.setProgress(data.progressMax()); data.setInnerDemon(0);
            require(ThunderTribulationService.begin(player, 1, 3, .75D), "Unbounded attempt starts"); checks++;
            ServerLevel nether = player.getServer().getLevel(Level.NETHER);
            require(nether != null, "The vanilla Nether is available for the dimension-transition fixture"); checks++;
            BlockPos netherSpawn = nether.getSharedSpawnPos();
            player.teleportTo(nether, netherSpawn.getX() + 0.5D, netherSpawn.getY(), netherSpawn.getZ() + 0.5D,
                    java.util.Set.of(), player.getYRot(), player.getXRot());
            CompoundTag unbounded = player.getPersistentData().getCompound(ThunderTribulationService.STATE_KEY);
            require(ThunderTribulationService.active(player) && player.serverLevel() == nether,
                    "The pending tribulation survives a dimension change"); checks++;
            unbounded.putString("phase", "warning");
            unbounded.putInt("phase_ticks", 1);
            player.setInvulnerable(false);
            player.hurtTime = 0;
            player.invulnerableTime = 0;
            player.setHealth(player.getMaxHealth());
            float healthBeforeNetherStrike = player.getHealth();
            ThunderTribulationService.tick(player);
            require(player.getHealth() < healthBeforeNetherStrike,
                    "A guaranteed lightning strike damages the player in the new dimension"); checks++;
            player.teleportTo(player.getX() + 512.0D, player.getY() + 96.0D, player.getZ());
            ThunderTribulationService.tick(player);
            require(ThunderTribulationService.active(player) && unbounded.getInt("total_ticks") > 0,
                    "The active attempt continues after leaving the launch area"); checks++;
            ThunderTribulationService.endForDeath(player);
            player.sendSystemMessage(Component.literal("[Thunder tribulation QA] All " + checks + " assertions passed"));
            org.slf4j.LoggerFactory.getLogger(ThunderTribulationIntegration.class)
                    .info("[Thunder tribulation QA] All {} assertions passed", checks);
            return checks;
        } catch (RuntimeException | LinkageError error) {
            org.slf4j.LoggerFactory.getLogger(ThunderTribulationIntegration.class).error("[Thunder tribulation QA] FAIL", error);
            throw error;
        } finally {
            if (ceilingPos != null && ceilingState != null && ceilingPos.getY() < originalLevel.getMaxBuildHeight())
                originalLevel.setBlock(ceilingPos, ceilingState, 3);
            if (player.serverLevel() != originalLevel)
                player.teleportTo(originalLevel, originalX, originalY, originalZ, java.util.Set.of(), originalYaw, originalPitch);
            else player.teleportTo(originalLevel, originalX, originalY, originalZ, originalYaw, originalPitch);
            for (String key : java.util.List.copyOf(player.getPersistentData().getAllKeys())) player.getPersistentData().remove(key);
            player.getPersistentData().merge(savedPersistent);
            data.load(savedData);
            livingPlayer.removeAllEffects();
            savedEffects.forEach(livingPlayer::addEffect);
            player.setInvulnerable(savedInvulnerable);
            player.setGameMode(savedMode);
            CultivationService.applyRealmAttributes(player, data);
            com.longju.xiantu.gongfa.GongfaService.refresh(player);
            player.setAbsorptionAmount(savedAbsorption);
            player.setHealth(Math.min(savedHealth, player.getMaxHealth()));
            CultivationEvents.sync(player);
        }
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("[Thunder tribulation QA FAIL] " + label);
    }
}
