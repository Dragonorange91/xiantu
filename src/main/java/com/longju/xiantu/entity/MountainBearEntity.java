package com.longju.xiantu.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Earth elite: shares the telegraphed heavy-strike contract, with rock debris. */
public final class MountainBearEntity extends MossBackBearEntity {
    public MountainBearEntity(EntityType<? extends MountainBearEntity> type, Level level) { super(type, level); }

    @Override protected void emitSlamParticles(boolean tell, Vec3 direction) { /* Photon owns combat presentation. */ }
}
