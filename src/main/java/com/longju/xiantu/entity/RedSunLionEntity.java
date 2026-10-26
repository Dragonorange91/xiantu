package com.longju.xiantu.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Fire elite: visible mane ignition and a bounded molten-colour impact ring. */
public final class RedSunLionEntity extends MossBackBearEntity {
    private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(1F, .22F, .035F), .9F);
    public RedSunLionEntity(EntityType<? extends RedSunLionEntity> type, Level level) { super(type, level); }

    @Override protected void emitSlamParticles(boolean tell, Vec3 direction) { /* Photon owns combat presentation. */ }
}
