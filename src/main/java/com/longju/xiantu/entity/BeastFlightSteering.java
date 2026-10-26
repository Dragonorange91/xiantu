package com.longju.xiantu.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Bounded heading changes; airspeed follows the beak, rather than an old lateral impulse. */
public final class BeastFlightSteering {
    private BeastFlightSteering() {}

    public static void steer(Mob bird, Vec3 destination, double speed) {
        Vec3 delta = destination.subtract(bird.position());
        double horizontal = delta.horizontalDistance();
        float desired = horizontal > .08 ? yaw(delta) : bird.getYRot();
        float next = Mth.approachDegrees(bird.getYRot(), desired, 7.0F);
        double remaining = Math.abs(Mth.wrapDegrees(desired - next));
        double horizontalSpeed = speed * horizontal / Math.max(.001, delta.length());
        // Brake into tight turns instead of sliding backwards while looking at the next waypoint.
        horizontalSpeed *= Mth.clamp(1.0 - remaining / 150.0, .16, 1.0);
        horizontalSpeed = Mth.lerp(.28, bird.getDeltaMovement().horizontalDistance(), horizontalSpeed);
        double heading = Math.toRadians(next);
        double vertical = Mth.lerp(.22, bird.getDeltaMovement().y,
                Mth.clamp(delta.y * .10, -speed * .75, speed * .75));
        bird.setYRot(next);
        bird.yBodyRot = next;
        bird.setYHeadRot(next);
        bird.setDeltaMovement(-Math.sin(heading) * horizontalSpeed, vertical,
                Math.cos(heading) * horizontalSpeed);
        bird.hasImpulse = true;
    }

    public static void aim(Mob bird, Vec3 direction) {
        if (direction == null || direction.horizontalDistanceSqr() < .0001) return;
        bird.setYRot(Mth.approachDegrees(bird.getYRot(), yaw(direction), 9.0F));
        bird.yBodyRot = bird.getYRot();
        bird.setYHeadRot(bird.getYRot());
    }

    public static void pose(Mob bird, boolean airborne) {
        Vec3 velocity = bird.getDeltaMovement();
        // Steer/aim already establishes heading. Coasting and dives must agree with actual motion.
        if (airborne && velocity.horizontalDistanceSqr() > .0004) {
            bird.setYRot(yaw(velocity));
            bird.yBodyRot = bird.getYRot();
            bird.setYHeadRot(bird.getYRot());
        }
        float pitch = airborne && velocity.horizontalDistanceSqr() > .001
                ? (float) Mth.clamp(-Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance())), -28, 38)
                : 0;
        bird.setXRot(Mth.approachDegrees(bird.getXRot(), pitch, 3.0F));
    }

    private static float yaw(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }
}
