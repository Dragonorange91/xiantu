package com.longju.xiantu.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Water beast: the common telegraphed pounce with ice-shard presentation. */
public final class ColdFangWolfEntity extends CrackedBladeWolfEntity {
    public ColdFangWolfEntity(EntityType<? extends ColdFangWolfEntity> type, Level level) {
        super(type, level);
    }

    @Override protected ParticleOptions pounceParticle() { return ParticleTypes.SNOWFLAKE; }

    @Override protected void onPounceHit(LivingEntity target) { /* Photon owns combat presentation. */ }
}
