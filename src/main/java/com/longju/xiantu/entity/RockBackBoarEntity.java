package com.longju.xiantu.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Neutral rock boar. A fixed, telegraphed charge can be dodged and never breaks blocks. */
public class RockBackBoarEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(
            RockBackBoarEntity.class, EntityDataSerializers.INT);
    private Vec3 origin;
    private int angerTicks, lostSightTicks, calmTicks;
    private long nextChargeAt, nextBiteAt;

    public RockBackBoarEntity(EntityType<? extends RockBackBoarEntity> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(ACTION, 0); }
    private boolean peaceful() { return level().getDifficulty() == Difficulty.PEACEFUL; }
    private boolean valid(LivingEntity target) {
        return target != null && target.isAlive() && target != this
                && (!(target instanceof Player p) || (!p.isCreative() && !p.isSpectator()));
    }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BoarCombatGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0) {
            @Override public boolean canUse() { return getTarget() == null && super.canUse(); }
            @Override public boolean canContinueToUse() { return getTarget() == null && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && !level().isClientSide && !peaceful() && source.getEntity() instanceof LivingEntity attacker && valid(attacker)) {
            setTarget(attacker); angerTicks = 240; calmTicks = 0;
        }
        return hit;
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (origin == null) origin = position();
        LivingEntity target = getTarget();
        if (valid(target) && !peaceful()) {
            calmTicks = 0;
            if (getSensing().hasLineOfSight(target) && !getNavigation().isStuck()) lostSightTicks = 0;
            else lostSightTicks++;
            if (--angerTicks <= 0 || distanceToSqr(target) > 24 * 24 || position().distanceToSqr(origin) > 24 * 24 || lostSightTicks >= 200) disengage();
        } else {
            if (target != null || entityData.get(ACTION) != 0) disengage();
            lostSightTicks = 0;
            if (++calmTicks > 400 && calmTicks % 20 == 0) heal(getMaxHealth() * .05F);
        }
    }
    private void disengage() {
        setTarget(null); angerTicks = 0; entityData.set(ACTION, 0); getNavigation().stop();
        if (origin != null && position().distanceToSqr(origin) > 4) getNavigation().moveTo(origin.x, origin.y, origin.z, 1.0);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (origin != null) { tag.putDouble("OriginX",origin.x); tag.putDouble("OriginY",origin.y); tag.putDouble("OriginZ",origin.z); }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("OriginX")) origin = new Vec3(tag.getDouble("OriginX"),tag.getDouble("OriginY"),tag.getDouble("OriginZ"));
    }
    public String animationState() {
        return switch(entityData.get(ACTION)) {
            case 1 -> "windup";
            case 2 -> "run";
            case 4 -> "attack";
            case 5 -> getDeltaMovement().horizontalDistanceSqr() > .0004 ? "run" : "idle";
            default -> getDeltaMovement().horizontalDistanceSqr() > .0004 ? "walk" : "idle";
        };
    }
    protected void emitChargeParticles(boolean tell, Vec3 direction) { /* Photon owns combat presentation. */ }
    private final class BoarCombatGoal extends Goal {
        private int mode, elapsed;
        private Vec3 direction, start;
        private final Set<UUID> victims = new HashSet<>();
        BoarCombatGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return !peaceful() && angerTicks > 0 && valid(getTarget()); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { BeastVfx.cancel(RockBackBoarEntity.this); mode = elapsed = 0; entityData.set(ACTION,0); getNavigation().stop(); halt(); }
        private void halt() { setDeltaMovement(0,getDeltaMovement().y,0); }
        private void face() {
            float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            setYRot(yaw); setYHeadRot(yaw); setYBodyRot(yaw);
        }
        private void recover() { BeastVfx.cancel(RockBackBoarEntity.this); mode=3; elapsed=0; entityData.set(ACTION,3); halt(); }
        @Override public void tick() {
            LivingEntity target=getTarget();
            if (!canUse()) { stop(); return; }
            if (mode != 0) {
                getNavigation().stop(); elapsed++;
                if (mode == 1) {
                    halt(); face();
                    
                    if (elapsed >= 24) { mode=2; elapsed=0; start=position(); victims.clear(); entityData.set(ACTION,2); BeastVfx.emit(RockBackBoarEntity.this,"trail",position(),direction,18,true); }
                } else if (mode == 2) {
                    face();
                    double progress=position().subtract(start).dot(direction);
                    if (progress >= 8 || elapsed > 30 || horizontalCollision) { recover(); return; }
                    double step=Math.min(.6,8-progress);
                    Vec3 from=position().add(0,.65,0), to=from.add(direction.scale(step+getBbWidth()*.5));
                    if (level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,RockBackBoarEntity.this)).getType()!=HitResult.Type.MISS) { recover(); return; }
                    setDeltaMovement(direction.x*step,getDeltaMovement().y,direction.z*step);
                    
                    // The swept segment prevents a fast charge from skipping a narrow target.
                    Vec3 a=position(), b=a.add(direction.scale(step));
                    for (LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().expandTowards(direction.scale(step)).inflate(.75,.3,.75),
                            e->valid(e) && (e==target || e instanceof Player))) {
                        Vec3 offset=e.position().subtract(a);
                        double along=Math.max(0,Math.min(step,offset.dot(direction)));
                        Vec3 closest=a.add(direction.scale(along));
                        if (e.position().subtract(closest).horizontalDistanceSqr() <= Math.pow(.75+e.getBbWidth()*.5,2)
                                && Math.abs(offset.y)<1.5 && getSensing().hasLineOfSight(e) && victims.add(e.getUUID())) {
                            if (e.hurt(damageSources().mobAttack(RockBackBoarEntity.this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*1.35F)) {
                                e.knockback(1.5,-direction.x,-direction.z);
                                BeastVfx.emit(RockBackBoarEntity.this,"impact",e.position(),direction,16,false);
                                playSound(SoundEvents.IRON_GOLEM_ATTACK,.7F,.65F);
                            }
                        }
                    }
                } else if (mode == 3) {
                    halt(); if(elapsed>=30) { mode=0; entityData.set(ACTION,0); }
                } else if (mode == 4) {
                    halt();
                    // BodyRotationControl may otherwise keep the last charge/navigation yaw
                    // while LookControl turns only the head toward a nearby attacker.
                    direction=target.position().subtract(position()).multiply(1,0,1).normalize();
                    if(direction.lengthSqr()>.001)face();
                    if (elapsed==9 && distanceToSqr(target)<=2.2*2.2 && getSensing().hasLineOfSight(target)) doHurtTarget(target);
                    if (elapsed>=18) { mode=0; entityData.set(ACTION,0); }
                }
                return;
            }
            getLookControl().setLookAt(target,30,30);
            double distance=distanceToSqr(target); long now=level().getGameTime();
            if (distance>=9 && distance<=100 && now>=nextChargeAt && getSensing().hasLineOfSight(target)) {
                direction=target.position().subtract(position()).multiply(1,0,1).normalize();
                face(); mode=1; elapsed=0; nextChargeAt=now+160; entityData.set(ACTION,1); getNavigation().stop();
                BeastVfx.emit(RockBackBoarEntity.this,"warn",position(),direction,24,false);
                playSound(SoundEvents.HOGLIN_ANGRY,.8F,.9F);
            } else if(distance<=2.2*2.2 && now>=nextBiteAt && getSensing().hasLineOfSight(target)) {
                direction=target.position().subtract(position()).multiply(1,0,1).normalize();
                if(direction.lengthSqr()>.001)face();
                mode=4; elapsed=0; nextBiteAt=now+40; entityData.set(ACTION,4); getNavigation().stop();
            } else { getNavigation().moveTo(target,1.15); entityData.set(ACTION,5); }
        }
        private void dust(boolean tell) {
            emitChargeParticles(tell, direction);
        }
    }
}
