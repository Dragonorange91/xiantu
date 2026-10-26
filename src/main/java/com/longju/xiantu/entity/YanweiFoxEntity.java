package com.longju.xiantu.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Hostile fire-beast pilot; no natural spawns or provisional demon-core loot. */
public final class YanweiFoxEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> ALERTED =
            SynchedEntityData.defineId(YanweiFoxEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RUNNING =
            SynchedEntityData.defineId(YanweiFoxEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(YanweiFoxEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double PLAYER_ALERT_DISTANCE = 16.0D;
    private static final int WARNING_TICKS = 30;
    private static final int LOST_TARGET_TICKS = 200;
    private static final int RANGED_ATTACK_COOLDOWN = 80;
    private long warningUntil;
    private long nextRangedAttack;
    private int lostSightTicks;
    private int attackAnimationTicks;

    public YanweiFoxEntity(EntityType<? extends YanweiFoxEntity> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ALERTED, false);
        entityData.define(RUNNING, false);
        entityData.define(ATTACKING, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FoxfireAttackGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false) {
            @Override public boolean canUse() { return !isPeacefulTestMode() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isPeacefulTestMode() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return !isPeacefulTestMode() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isPeacefulTestMode() && super.canContinueToUse(); }
        });
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 16, true, false,
                entity -> entity instanceof Player player && player.isAlive()
                        && !player.isCreative() && !player.isSpectator()) {
            @Override public boolean canUse() { return !isPeacefulTestMode() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isPeacefulTestMode() && super.canContinueToUse(); }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (isPeacefulTestMode()) {
                setTarget(null);
                warningUntil = 0L;
                lostSightTicks = 0;
                attackAnimationTicks = 0;
                entityData.set(ALERTED, false);
                entityData.set(RUNNING, false);
                entityData.set(ATTACKING, false);
                return;
            }
            LivingEntity target = getTarget();
            long now = level().getGameTime();
            if (target instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator()
                    && distanceToSqr(player) <= PLAYER_ALERT_DISTANCE * PLAYER_ALERT_DISTANCE
                    && getSensing().hasLineOfSight(player)) {
                if (warningUntil == 0L) warningUntil = now + WARNING_TICKS;
                entityData.set(ALERTED, now < warningUntil);
                lostSightTicks = 0;
            } else if (target != null) {
                entityData.set(ALERTED, false);
                warningUntil = 0L;
                if (!target.isAlive() || distanceToSqr(target) > 24.0D * 24.0D || ++lostSightTicks >= LOST_TARGET_TICKS) {
                    setTarget(null);
                    lostSightTicks = 0;
                }
            } else {
                entityData.set(ALERTED, false);
                warningUntil = 0L;
                lostSightTicks = 0;
            }
            entityData.set(ATTACKING, attackAnimationTicks > 0);
            entityData.set(RUNNING, getTarget() != null && !isWarning() && getNavigation().isInProgress()
                    && getDeltaMovement().horizontalDistanceSqr() > 0.0004D);
            if (attackAnimationTicks > 0) attackAnimationTicks--;
        }
    }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    private boolean isPeacefulTestMode() {
        return level().getDifficulty() == Difficulty.PEACEFUL;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (!accepted || level().isClientSide || isPeacefulTestMode()
                || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == this || !attacker.isAlive()) return accepted;
        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) return accepted;
        setTarget(attacker);
        warningUntil = level().getGameTime();
        lostSightTicks = 0;
        entityData.set(ALERTED, false);
        return accepted;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide) attackAnimationTicks = 10;
        return hit;
    }

    public boolean isAlerted() {
        return entityData.get(ALERTED);
    }

    public boolean isRunning() {
        return entityData.get(RUNNING);
    }

    public boolean isAttacking() {
        return entityData.get(ATTACKING);
    }

    private boolean isWarning() {
        return entityData.get(ALERTED) && level().getGameTime() < warningUntil;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.FOX_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.FOX_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.FOX_DEATH; }

    private final class FoxfireAttackGoal extends Goal {
        private LivingEntity target;
        private int castTicks;
        private Vec3 lockedAim;

        private FoxfireAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isPeacefulTestMode()) return false;
            target = getTarget();
            return target != null && target.isAlive()
                    && (warningUntil == 0L || isWarning() || distanceToSqr(target) >= 4.0D * 4.0D);
        }

        @Override
        public boolean canContinueToUse() {
            return !isPeacefulTestMode() && target != null && target.isAlive() && getTarget() == target
                    && distanceToSqr(target) <= 24.0D * 24.0D
                    && (castTicks > 0 || isWarning() || distanceToSqr(target) >= 4.0D * 4.0D);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (warningUntil == 0L && target instanceof Player player
                    && distanceToSqr(player) <= PLAYER_ALERT_DISTANCE * PLAYER_ALERT_DISTANCE
                    && getSensing().hasLineOfSight(player)) {
                warningUntil = level().getGameTime() + WARNING_TICKS;
                entityData.set(ALERTED, true);
            }
            if (castTicks > 0) {
                getNavigation().stop();
                if (--castTicks == 0) fireFoxfire(target);
                return;
            }
            double distance = distanceToSqr(target);
            if (isWarning()) {
                getNavigation().stop();
                return;
            }
            if (distance > 12.0D * 12.0D || !getSensing().hasLineOfSight(target)) {
                getNavigation().moveTo(target, 1.0D);
                return;
            }
            getNavigation().stop();
            if (distance >= 4.0D * 4.0D && level().getGameTime() >= nextRangedAttack
                    && getSensing().hasLineOfSight(target)) {
                lockedAim = target.getBoundingBox().getCenter().subtract(getEyePosition().add(0,-.1,0)).normalize();
                castTicks = 20; attackAnimationTicks = 20;
                BeastVfx.emit(YanweiFoxEntity.this,"warn",position(),lockedAim,20,true);
                nextRangedAttack = level().getGameTime() + RANGED_ATTACK_COOLDOWN;
            }
        }

        @Override
        public void stop() {
            BeastVfx.cancel(YanweiFoxEntity.this);
            castTicks=0; target = null;
            getNavigation().stop();
        }

        private void fireFoxfire(LivingEntity victim) {
            if (!(level() instanceof ServerLevel serverLevel)) return;
            FoxfireProjectile ball = new FoxfireProjectile(com.longju.xiantu.registry.ModEntities.FOXFIRE.get(),level());
            ball.setOwner(YanweiFoxEntity.this);
            ball.setDamage((float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            ball.setPos(getX(), getEyeY() - .1, getZ());
            ball.setDeltaMovement(lockedAim.scale(.4));
            serverLevel.addFreshEntity(ball);
            attackAnimationTicks = 10;
            playSound(SoundEvents.BLAZE_SHOOT, 0.8F, 1.35F);
        }
    }

}
