package com.longju.xiantu.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Neutral earth beast: visible horn tell, fixed-direction charge and wall stop. */
public final class ThickHornRhinoEntity extends RockBackBoarEntity {
    public ThickHornRhinoEntity(EntityType<? extends ThickHornRhinoEntity> type, Level level) { super(type, level); }

    @Override protected void emitChargeParticles(boolean tell, Vec3 direction) { /* Photon owns combat presentation. */ }
}
