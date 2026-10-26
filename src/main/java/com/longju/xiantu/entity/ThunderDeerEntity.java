package com.longju.xiantu.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Peaceful thunder deer: flee from harm without acquiring an attack target. */
public final class ThunderDeerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> RUNNING = SynchedEntityData.defineId(
            ThunderDeerEntity.class, EntityDataSerializers.BOOLEAN);
    private int fleeTicks;

    public ThunderDeerEntity(EntityType<? extends ThunderDeerEntity> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RUNNING, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.65) {
            @Override public boolean canUse() { return (fleeTicks > 0 || isOnFire()) && super.canUse(); }
            @Override public boolean canContinueToUse() { return (fleeTicks > 0 || isOnFire()) && super.canContinueToUse(); }
        });
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide) {
            fleeTicks = 100;
            navigation.stop();
        }
        return accepted;
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (fleeTicks > 0 && --fleeTicks == 0) setLastHurtByMob(null);
            entityData.set(RUNNING, !isNoAi() && (fleeTicks > 0 || isOnFire())
                    && !navigation.isDone() && getDeltaMovement().horizontalDistanceSqr() > .0004);
        }
    }

    public String animationState() {
        if (getDeltaMovement().horizontalDistanceSqr() <= .0004) return "idle";
        return entityData.get(RUNNING) ? "run" : "walk";
    }
}
