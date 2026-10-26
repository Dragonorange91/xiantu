package com.longju.xiantu.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** Hostile wood beast. Server owns the strike timing; the GLB only presents it. */
public class MossBackBearEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(
            MossBackBearEntity.class, EntityDataSerializers.INT);
    private int warningTicks, lostSightTicks;
    private long nextSlamAt, nextSwipeAt;
    private LivingEntity warnedTarget;

    public MossBackBearEntity(EntityType<? extends MossBackBearEntity> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(ACTION, 0); }
    private boolean peaceful() { return level().getDifficulty() == Difficulty.PEACEFUL; }
    private boolean valid(LivingEntity target) {
        return target != null && target.isAlive()
                && (!(target instanceof Player p) || (!p.isCreative() && !p.isSpectator()));
    }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BearCombatGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0) {
            @Override public boolean canUse() { return getTarget() == null && super.canUse(); }
            @Override public boolean canContinueToUse() { return getTarget() == null && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return !peaceful() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !peaceful() && super.canContinueToUse(); }
        });
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::valid) {
            @Override public boolean canUse() { return !peaceful() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !peaceful() && super.canContinueToUse(); }
        });
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        LivingEntity target = getTarget();
        if (peaceful() || !valid(target)) {
            setTarget(null); warnedTarget = null; warningTicks = lostSightTicks = 0;
            entityData.set(ACTION, 0); return;
        }
        if (target != warnedTarget) {
            warnedTarget = target; warningTicks = 30;
            playSound(SoundEvents.POLAR_BEAR_WARNING, .8F, .8F);
        }
        if (getSensing().hasLineOfSight(target)) lostSightTicks = 0;
        else lostSightTicks++;
        if (distanceToSqr(target) > 24 * 24 || lostSightTicks >= 200) {
            setTarget(null); getNavigation().stop(); entityData.set(ACTION, 0); return;
        }
        if (warningTicks > 0) { warningTicks--; getNavigation().stop(); }
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && !level().isClientSide && !peaceful()
                && source.getEntity() instanceof LivingEntity attacker && valid(attacker) && attacker != this) {
            setTarget(attacker); warnedTarget = attacker; warningTicks = 0;
        }
        return hit;
    }
    public String animationState() {
        int action = entityData.get(ACTION);
        if (action == 2) return "slam";
        if (action == 1) return "attack";
        if (action == 3) return "run";
        return getDeltaMovement().horizontalDistanceSqr() > .0004 ? "walk" : "idle";
    }
    private final class BearCombatGoal extends Goal {
        private int mode, elapsed;
        private Vec3 strikeDirection;
        BearCombatGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return !peaceful() && valid(getTarget()); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { BeastVfx.cancel(MossBackBearEntity.this); mode = elapsed = 0; entityData.set(ACTION, 0); getNavigation().stop(); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (!valid(target) || peaceful()) { stop(); return; }
            if (target != warnedTarget) {
                warnedTarget = target; warningTicks = 30;
                playSound(SoundEvents.POLAR_BEAR_WARNING, .8F, .8F);
            }
            if (mode != 0) {
                getNavigation().stop();
                elapsed++;
                if (mode == 2) {
                    
                    if (elapsed == 30) slam(target);
                    if (elapsed >= 70) { mode = 0; entityData.set(ACTION, 0); }
                } else {
                    if (elapsed == 9 && distanceToSqr(target) <= 2.2 * 2.2 && getSensing().hasLineOfSight(target)) {
                        doHurtTarget(target);
                        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, .7F, .7F);
                    }
                    if (elapsed >= 22) { mode = 0; entityData.set(ACTION, 0); }
                }
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            if (warningTicks > 0) { getNavigation().stop(); entityData.set(ACTION, 0); return; }
            long now = level().getGameTime();
            double distance = distanceToSqr(target);
            if (distance <= 16 && now >= nextSlamAt && getSensing().hasLineOfSight(target)) {
                strikeDirection = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                if (strikeDirection.lengthSqr() < .01) strikeDirection = getLookAngle().multiply(1,0,1).normalize();
                float facing = (float)Math.toDegrees(Math.atan2(-strikeDirection.x, strikeDirection.z));
                setYRot(facing); setYBodyRot(facing); setYHeadRot(facing);
                mode = 2; elapsed = 0; nextSlamAt = now + 180;
                entityData.set(ACTION, 2); getNavigation().stop();
                BeastVfx.emit(MossBackBearEntity.this, "warn", position(), strikeDirection, 30, false);
                playSound(SoundEvents.POLAR_BEAR_WARNING, 1, .65F);
            } else if (distance <= 2.2 * 2.2 && now >= nextSwipeAt && getSensing().hasLineOfSight(target)) {
                mode = 1; elapsed = 0; nextSwipeAt = now + 40;
                entityData.set(ACTION, 1); getNavigation().stop();
            } else {
                getNavigation().moveTo(target, 1.2);
                entityData.set(ACTION, getDeltaMovement().horizontalDistanceSqr() > .0004 ? 3 : 0);
            }
        }
        private void slam(LivingEntity target) {
            BeastVfx.emit(MossBackBearEntity.this, "impact", position().add(strikeDirection.scale(1.5)), strikeDirection, 16, false);
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1, .6F);
            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4, 1.5, 4),
                    e -> e != MossBackBearEntity.this && valid(e) && (e == target || e instanceof Player))) {
                Vec3 offset = victim.position().subtract(position());
                double forward = offset.x * strikeDirection.x + offset.z * strikeDirection.z;
                double lateral = offset.x * strikeDirection.z - offset.z * strikeDirection.x;
                if (forward >= 0 && forward <= 3 && Math.abs(lateral) <= 1 && Math.abs(offset.y) <= 1.5
                        && getSensing().hasLineOfSight(victim)) {
                    victim.hurt(damageSources().mobAttack(MossBackBearEntity.this), (float)getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F);
                }
            }
        }
        private void particles(boolean tell) {
            emitSlamParticles(tell, strikeDirection);
        }
    }
    protected void emitSlamParticles(boolean tell, Vec3 strikeDirection) { /* Photon owns combat presentation. */ }
}
