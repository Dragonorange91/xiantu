package com.longju.xiantu.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** A private breakthrough opponent. Its only valid target is its challenge owner. */
public final class InnerDemonEntity extends Zombie {
    private UUID ownerId;
    private UUID transactionId;
    private long expiresAt;

    public InnerDemonEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 0;
        setPersistenceRequired();
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public void bind(UUID owner, UUID transaction, long expiry) {
        ownerId = owner;
        transactionId = transaction;
        expiresAt = expiry;
    }

    public UUID ownerId() { return ownerId; }
    public UUID transactionId() { return transactionId; }
    public long expiresAt() { return expiresAt; }
    public float mirroredEquipmentDropChance(net.minecraft.world.entity.EquipmentSlot slot) { return getEquipmentDropChance(slot); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        if (ownerId == null || transactionId == null) {
            discard();
            return;
        }
        if (server.getGameTime() >= expiresAt) {
            com.longju.xiantu.cultivation.InnerDemonService.timeout(this);
            return;
        }
        ServerPlayer owner = server.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != level() || !owner.isAlive()) {
            setTarget(null);
            return;
        }
        setTarget(owner);
    }

    @Override public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return target instanceof ServerPlayer player && ownerId != null && ownerId.equals(player.getUUID())
                && super.canAttack(target);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (ownerId == null) return false;
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (attacker instanceof net.minecraft.world.entity.projectile.Projectile projectile)
            attacker = projectile.getOwner();
        return attacker instanceof ServerPlayer player && ownerId.equals(player.getUUID())
                && super.hurt(source, amount);
    }

    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {}

    @Override protected boolean isSunSensitive() { return false; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("xiantu_owner", ownerId);
        if (transactionId != null) tag.putUUID("xiantu_transaction", transactionId);
        tag.putLong("xiantu_expires", expiresAt);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("xiantu_owner") ? tag.getUUID("xiantu_owner") : null;
        transactionId = tag.hasUUID("xiantu_transaction") ? tag.getUUID("xiantu_transaction") : null;
        expiresAt = tag.getLong("xiantu_expires");
        xpReward = 0;
        setPersistenceRequired();
    }
}
