package com.longju.xiantu.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Neutral raven: visible dive warning, collision-aware flight and a grounded recovery. */
public final class ThunderFeatherRavenEntity extends FlyingMob implements CompletionAnimated {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(
            ThunderFeatherRavenEntity.class, EntityDataSerializers.INT);
    private BlockPos home;
    private Vec3 waypoint;
    private Vec3 diveDirection;
    private int phase, phaseTicks, flightTicks, lostSightTicks;
    private long nextFlightAt, nextDiveAt, nextMeleeAt;
    private boolean diveHit, meleeHit;

    public ThunderFeatherRavenEntity(EntityType<? extends ThunderFeatherRavenEntity> type, Level level) {
        super(type, level);
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(STATE, 0); }
    @Override protected void registerGoals() {}
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override public void travel(Vec3 input) {
        // FlyingMob damps velocity but never applies gravity, including on landing.
        if (isControlledByLocalInstance() && !isNoGravity() && !isNoAi()) {
            setDeltaMovement(getDeltaMovement().add(0, -.08, 0));
        }
        super.travel(input);
    }

    @Override public String animationState() {
        return switch (entityData.get(STATE)) {
            case 1 -> "takeoff";
            case 2 -> "fly";
            case 3 -> "glide";
            case 4 -> "land";
            case 5 -> "alert";
            case 6 -> "attack";
            default -> "idle";
        };
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && !level().isClientSide && level().getDifficulty() != Difficulty.PEACEFUL
                && source.getEntity() instanceof LivingEntity attacker && attacker != this
                && (!(attacker instanceof Player p) || (!p.isCreative() && !p.isSpectator()))) {
            setTarget(attacker);
        }
        return hit;
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive() || isNoAi()) return;
        long now = level().getGameTime();
        if (home == null) home = blockPosition();
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            setTarget(null);
            if (phase == 5 || phase == 6) { BeastVfx.cancel(this); phase = 2; phaseTicks = 0; waypoint = null; }
        }
        LivingEntity target = getTarget();
        if (target != null) {
            if (!target.isAlive() || target.distanceToSqr(Vec3.atCenterOf(home)) > 24 * 24
                    || (target instanceof Player p && (p.isCreative() || p.isSpectator()))) setTarget(null);
            else if (getSensing().hasLineOfSight(target)) lostSightTicks = 0;
            else if (++lostSightTicks >= 200) setTarget(null);
        }
        target = getTarget();
        double floor = floorY();
        if (phase == 0) {
            setNoGravity(false);
            entityData.set(STATE, 0);
            if (onGround() && (target != null || now >= nextFlightAt)) {
                phase = 1; phaseTicks = 16; flightTicks = 80 + random.nextInt(80);
                waypoint = position().add(0, 2.5, 0);
                setNoGravity(true);
            }
        } else if (phase == 1) {
            entityData.set(STATE, 1); steer(waypoint, .15);
            if (--phaseTicks <= 0) { phase = 2; waypoint = null; }
        } else if (phase == 2) {
            setNoGravity(true);
            entityData.set(STATE, (tickCount / 24) % 3 == 0 ? 3 : 2);
            if (target != null && now >= nextMeleeAt && inMeleeRange(target)
                    && getSensing().hasLineOfSight(target)) {
                phase = 7; phaseTicks = 7; setDeltaMovement(Vec3.ZERO); meleeHit = false;
                diveDirection = target.getBoundingBox().getCenter().subtract(position()).normalize();
                BeastVfx.emit(this, "melee", position(), diveDirection, 20, true);
            } else if (target != null && now >= nextDiveAt && distanceToSqr(target) >= 16
                    && distanceToSqr(target) <= 144 && getSensing().hasLineOfSight(target)) {
                phase = 5; phaseTicks = 30; setDeltaMovement(Vec3.ZERO);
                diveDirection = target.getBoundingBox().getCenter().subtract(position()).normalize();
                BeastVfx.emit(this,"warn",position(),diveDirection,30,true);
                playSound(SoundEvents.PARROT_AMBIENT, .8F, .65F);
            } else {
                if (waypoint == null || distanceToSqr(waypoint) < 1 || horizontalCollision) {
                    Vec3 anchor = target == null ? Vec3.atCenterOf(home) : target.position();
                    waypoint = new Vec3(anchor.x + (random.nextDouble() - .5) * 12,
                            floor + 2 + random.nextDouble() * 4, anchor.z + (random.nextDouble() - .5) * 12);
                }
                steer(waypoint, .18);
                if (--flightTicks <= 0 && target == null) { BeastVfx.cancel(this); phase = 4; phaseTicks = 0; }
            }
        } else if (phase == 5) {
            entityData.set(STATE, 5); setDeltaMovement(getDeltaMovement().scale(.7));
            
            if (target == null) { BeastVfx.cancel(this); phase = 4; phaseTicks = 0; }
            else {
                BeastFlightSteering.aim(this, diveDirection);
                if (--phaseTicks <= 0) {
                    BeastVfx.emit(this,"trail",position(),diveDirection,20,true);
                    phase = 6; phaseTicks = 20; diveHit = false; nextDiveAt = now + 180;
                }
            }
        } else if (phase == 6) {
            entityData.set(STATE, 6);
            setDeltaMovement(diveDirection.scale(.35)); hasImpulse = true;
            
            if (!diveHit && target != null && getSensing().hasLineOfSight(target)
                    && getBoundingBox().inflate(.5).intersects(target.getBoundingBox())) {
                diveHit = target.hurt(damageSources().mobAttack(this), (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25));
                if (diveHit) BeastVfx.emit(this,"impact",target.position(),diveDirection,16,false);
            }
            if (--phaseTicks <= 0 || horizontalCollision || onGround()) { BeastVfx.cancel(this); phase = 4; phaseTicks = 0; }
        } else if (phase == 7) {
            entityData.set(STATE, 5);
            if (target == null || !target.isAlive()) { BeastVfx.cancel(this); phase = 2; phaseTicks = 0; }
            else {
                diveDirection = target.getBoundingBox().getCenter().subtract(position()).normalize();
                BeastFlightSteering.aim(this, diveDirection);
                if (--phaseTicks <= 0) { phase = 8; phaseTicks = 16; nextMeleeAt = now + 50; }
            }
        } else if (phase == 8) {
            entityData.set(STATE, 6);
            if (target == null || !target.isAlive()) { BeastVfx.cancel(this); phase = 2; phaseTicks = 0; }
            else {
                // Commit the windup direction; do not snap 180 degrees when a target passes us.
                BeastFlightSteering.steer(this, position().add(diveDirection.scale(6)), .5);
                if (!meleeHit && getSensing().hasLineOfSight(target) && getBoundingBox().inflate(.2).intersects(target.getBoundingBox())) {
                    meleeHit = target.hurt(damageSources().mobAttack(this),
                            (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                    if (meleeHit) BeastVfx.emit(this, "impact", target.getBoundingBox().getCenter(), diveDirection, 14, false);
                }
                if (--phaseTicks <= 0 || horizontalCollision || onGround()) { phase = 2; phaseTicks = 0; }
            }
        } else if (phase == 4) {
            entityData.set(STATE, 4);
            if (!onGround()) { setNoGravity(false); setDeltaMovement(getDeltaMovement().multiply(.65, 1, .65)); }
            else {
                setNoGravity(false); setDeltaMovement(Vec3.ZERO);
                if (++phaseTicks >= 40) { phase = 0; nextFlightAt = now + 40 + random.nextInt(120); }
            }
        }
        if (getY() > floor + 8) setDeltaMovement(getDeltaMovement().multiply(1, 0, 1).add(0, -.18, 0));
        BeastFlightSteering.pose(this, phase != 0 && !onGround());
    }

    private void steer(Vec3 destination, double speed) {
        if (destination == null) return;
        Vec3 delta = destination.subtract(position());
        if (!level().noCollision(this, getBoundingBox().move(delta.normalize().scale(.5)))) {
            waypoint = null; setDeltaMovement(getDeltaMovement().scale(.4)); return;
        }
        BeastFlightSteering.steer(this, destination, speed);
    }

    private boolean inMeleeRange(LivingEntity target) {
        Vec3 delta = target.getBoundingBox().getCenter().subtract(position());
        return delta.horizontalDistanceSqr() <= 16 && Math.abs(delta.y) <= 6;
    }

    private double floorY() {
        BlockPos.MutableBlockPos cursor = blockPosition().mutable();
        for (int i = 0; i < 12 && cursor.getY() > level().getMinBuildHeight(); i++) {
            if (level().getBlockState(cursor).isSolidRender(level(), cursor)) return cursor.getY() + 1;
            cursor.move(0, -1, 0);
        }
        return home == null ? getY() : home.getY();
    }

    private void sparks(int count) { /* Photon owns combat presentation. */ }
}
