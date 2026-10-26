package com.longju.xiantu.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Metal elite: telegraphed two-paw strike with a golden mane halo and ground ripples. */
public final class GoldManeLionEntity extends MossBackBearEntity {
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, .72F, .16F), .8F);

    public GoldManeLionEntity(EntityType<? extends GoldManeLionEntity> type, Level level) { super(type, level); }

    @Override protected void emitSlamParticles(boolean tell, Vec3 direction) { /* Photon owns combat presentation. */ }
}
