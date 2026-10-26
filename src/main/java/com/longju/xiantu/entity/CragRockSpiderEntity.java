package com.longju.xiantu.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Hostile eight-legged arthropod pilot; natural spawn and drops remain separate milestones. */
public final class CragRockSpiderEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> RUNNING =
            SynchedEntityData.defineId(CragRockSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(CragRockSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private long attackUntil;
    private long nextCircleAt;

    public CragRockSpiderEntity(EntityType<? extends CragRockSpiderEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RUNNING, false);
        entityData.define(ATTACKING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GroundCircleGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false) {
            @Override public boolean canUse() { return !isPeacefulTestMode() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isPeacefulTestMode() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
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
        if (!level().isClientSide) {
            if (isPeacefulTestMode()) {
                setTarget(null);
                entityData.set(RUNNING, false);
                entityData.set(ATTACKING, false);
                return;
            }
            entityData.set(RUNNING, getNavigation().isInProgress()
                    && getDeltaMovement().horizontalDistanceSqr() > 0.0004D);
            entityData.set(ATTACKING, level().getGameTime() < attackUntil);
        }
    }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    private boolean isPeacefulTestMode() {
        return level().getDifficulty() == Difficulty.PEACEFUL;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) attackUntil = level().getGameTime() + 18L;
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && !isPeacefulTestMode()
                && source.getEntity() instanceof LivingEntity attacker
                && attacker != this && attacker.isAlive()) {
            setTarget(attacker);
        }
        return accepted;
    }

    private final class GroundCircleGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private net.minecraft.world.phys.Vec3 center;
        private int elapsed;
        GroundCircleGoal() { setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            LivingEntity t=getTarget();
            return !isPeacefulTestMode() && t!=null && t.isAlive() && distanceToSqr(t)<=36
                    && level().getGameTime()>=nextCircleAt && getSensing().hasLineOfSight(t);
        }
        @Override public boolean canContinueToUse() { return !isPeacefulTestMode() && elapsed<40 && getTarget()!=null && getTarget().isAlive(); }
        @Override public void start() {
            center=getTarget().position(); elapsed=0; nextCircleAt=level().getGameTime()+160;
            attackUntil=level().getGameTime()+40; getNavigation().stop();
            BeastVfx.emit(CragRockSpiderEntity.this,"warn",center,net.minecraft.world.phys.Vec3.ZERO,24,false);
        }
        @Override public void tick() {
            getNavigation().stop();
            if(++elapsed!=24)return;
            BeastVfx.emit(CragRockSpiderEntity.this,"impact",center,net.minecraft.world.phys.Vec3.ZERO,16,false);
            var bounds=new net.minecraft.world.phys.AABB(center,center).inflate(2,1.5,2);
            for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,bounds,
                    e->e!=CragRockSpiderEntity.this && e.isAlive() && (e==getTarget() || e instanceof Player)
                       && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
                if(e.position().subtract(center).horizontalDistanceSqr()>4 || !getSensing().hasLineOfSight(e))continue;
                var ray=level().clip(new net.minecraft.world.level.ClipContext(center.add(0,.3,0),e.position().add(0,.3,0),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,CragRockSpiderEntity.this));
                if(ray.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)continue;
                if(e.hurt(damageSources().mobAttack(CragRockSpiderEntity.this),
                     (float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)*.9F))
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.longju.xiantu.registry.ModEffects.BEAST_SNARE.get(),40));
            }
        }
        @Override public void stop() { BeastVfx.cancel(CragRockSpiderEntity.this); attackUntil=0; elapsed=40; }
    }

    public boolean isRunning() {
        return entityData.get(RUNNING);
    }

    public boolean isAttacking() {
        return entityData.get(ATTACKING);
    }
}

