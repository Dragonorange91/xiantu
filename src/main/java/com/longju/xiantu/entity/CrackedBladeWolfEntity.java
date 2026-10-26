package com.longju.xiantu.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Hostile metal-beast animation and combat pilot. Natural spawn and loot are intentionally not wired yet. */
public class CrackedBladeWolfEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> ALERTED =
            SynchedEntityData.defineId(CrackedBladeWolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RUNNING =
            SynchedEntityData.defineId(CrackedBladeWolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(CrackedBladeWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int WARNING_TICKS = 30;
    private static final int MAX_LOST_SIGHT_TICKS = 200;
    private long warningUntil;
    private long nextPounceAt;
    private int lostSightTicks;
    private int pouncePhase;
    private int phaseTicks;
    private boolean pounceHit;
    private Vec3 pounceDirection = Vec3.ZERO;

    public CrackedBladeWolfEntity(EntityType<? extends CrackedBladeWolfEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ALERTED, false);
        entityData.define(RUNNING, false);
        entityData.define(ATTACKING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PounceGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false) {
            @Override public boolean canUse() {
                return !isPeacefulTestMode() && !isAlerted() && pouncePhase == 0 && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return !isPeacefulTestMode() && !isAlerted() && pouncePhase == 0 && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override public boolean canUse() {
                return getTarget() == null && !isAlerted() && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return getTarget() == null && !isAlerted() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return !isPeacefulTestMode() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isPeacefulTestMode() && super.canContinueToUse(); }
        });
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 16, true, false,
                target -> target instanceof Player player && player.isAlive()
                        && !player.isCreative() && !player.isSpectator()) {
            @Override public boolean canUse() { return !isPeacefulTestMode() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isPeacefulTestMode() && super.canContinueToUse(); }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        LivingEntity target = getTarget();
        long now = level().getGameTime();
        if (isPeacefulTestMode()) {
            setTarget(null);
            warningUntil = 0L;
            lostSightTicks = 0;
            entityData.set(ALERTED, false);
            entityData.set(ATTACKING, false);
            entityData.set(RUNNING, false);
            return;
        }
        boolean canSeeTarget = target != null && target.isAlive() && getSensing().hasLineOfSight(target);
        if (target instanceof Player player && canSeeTarget
                && distanceToSqr(player) <= 16.0D * 16.0D) {
            if (warningUntil == 0L) {
                warningUntil = now + WARNING_TICKS;
                playSound(SoundEvents.WOLF_GROWL, 0.8F, 0.75F);
            }
            entityData.set(ALERTED, now < warningUntil);
            lostSightTicks = 0;
        } else if (target != null) {
            entityData.set(ALERTED, false);
            warningUntil = 0L;
            if (!target.isAlive() || distanceToSqr(target) > 24.0D * 24.0D
                    || ++lostSightTicks >= MAX_LOST_SIGHT_TICKS) {
                setTarget(null);
                lostSightTicks = 0;
            }
        } else {
            entityData.set(ALERTED, false);
            warningUntil = 0L;
            lostSightTicks = 0;
        }

        entityData.set(ATTACKING, pouncePhase == 1 || pouncePhase == 2);
        if (isAlerted()) getNavigation().stop();
        entityData.set(RUNNING, getTarget() != null && !isAlerted() && pouncePhase == 0
                && getNavigation().isInProgress() && getDeltaMovement().horizontalDistanceSqr() > 0.0004D);
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
        return true;
    }

    public boolean isAlerted() { return entityData.get(ALERTED); }
    public boolean isRunning() { return entityData.get(RUNNING); }
    public boolean isAttacking() { return entityData.get(ATTACKING); }

    protected net.minecraft.core.particles.ParticleOptions pounceParticle() { return ParticleTypes.CRIT; }

    protected void onPounceHit(LivingEntity target) {}

    private final class PounceGoal extends Goal {
        @Override public boolean requiresUpdateEveryTick() { return true; }
        private PounceGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (isPeacefulTestMode() || target == null || !target.isAlive() || isAlerted() || pouncePhase != 0
                    || level().getGameTime() < nextPounceAt || !getSensing().hasLineOfSight(target)) return false;
            double distance = distanceToSqr(target);
            return distance >= 2.0D * 2.0D && distance <= 7.0D * 7.0D;
        }

        @Override
        public boolean canContinueToUse() { return !isPeacefulTestMode() && pouncePhase != 0; }

        @Override
        public void start() {
            pounceDirection = getTarget().position().subtract(position()).multiply(1,0,1).normalize();
            BeastVfx.emit(CrackedBladeWolfEntity.this,"warn",position(),pounceDirection,16,false);
            pouncePhase = 1;
            phaseTicks = 16;
            pounceHit = false;
            nextPounceAt = level().getGameTime() + 120L;
            getNavigation().stop();
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 0.7F, 1.3F);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if ((target == null || !target.isAlive()) && pouncePhase != 3) {
                BeastVfx.cancel(CrackedBladeWolfEntity.this);
                pouncePhase = 3; phaseTicks = 20;
            }
            if (target != null && pouncePhase == 0) lookAt(target, 360.0F, 30.0F);
            getNavigation().stop();

            if (pouncePhase == 1) {
                
                if (--phaseTicks <= 0) {
                    Vec3 direction = pounceDirection;
                    setDeltaMovement(direction.scale(0.5D).add(0.0D, 0.28D, 0.0D));
                    hasImpulse = true;
                    pouncePhase = 2;
                    phaseTicks = 12;
                    BeastVfx.emit(CrackedBladeWolfEntity.this,"trail",position(),pounceDirection,12,true);
                }
            } else if (pouncePhase == 2) {
                // Maintain the advertised six-block leap instead of losing all speed to ground friction.
                setDeltaMovement(pounceDirection.x*.5,getDeltaMovement().y,pounceDirection.z*.5);
                hasImpulse=true;
                if (horizontalCollision) phaseTicks=1;
                if (!pounceHit && target != null && getSensing().hasLineOfSight(target) && target.getBoundingBox().inflate(0.35D).intersects(getBoundingBox())) {
                    float damage = (float) (getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.2D);
                    pounceHit = target.hurt(damageSources().mobAttack(CrackedBladeWolfEntity.this), damage);
                    if (pounceHit) {
                        onPounceHit(target);
                        BeastVfx.emit(CrackedBladeWolfEntity.this,"impact",target.position(),pounceDirection,16,false);
                        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.85F);
                    }
                }
                if (--phaseTicks <= 0) {
                    pouncePhase = 3;
                    phaseTicks = 20;
                    setDeltaMovement(getDeltaMovement().multiply(0.25D, 0.0D, 0.25D));
                    BeastVfx.cancel(CrackedBladeWolfEntity.this);
                }
            } else if (--phaseTicks <= 0) {
                pouncePhase = 0;
            }
        }

        @Override
        public void stop() {
            BeastVfx.cancel(CrackedBladeWolfEntity.this);
            if (pouncePhase != 0) {
                pouncePhase = 0;
                phaseTicks = 0;
            }
        }

        private void emitParticles(net.minecraft.core.particles.ParticleOptions particle, int count,
                                   double spread, double speed) { /* Photon owns combat presentation. */ }
    }
}
