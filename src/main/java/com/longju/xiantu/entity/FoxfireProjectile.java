package com.longju.xiantu.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Straight, finite foxfire: no homing, explosions, block damage or ignition. */
public final class FoxfireProjectile extends Projectile implements ItemSupplier {
    private float damage = 4;
    public FoxfireProjectile(EntityType<? extends FoxfireProjectile> type, Level level) { super(type, level); }
    public void setDamage(float damage) { this.damage = Math.max(0, damage); }
    @Override protected void defineSynchedData() {}
    @Override public ItemStack getItem() { return new ItemStack(Items.FIRE_CHARGE); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); damage=tag.getFloat("FoxfireDamage"); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putFloat("FoxfireDamage", damage); }
    @Override public void tick() {
        super.tick();
        Vec3 from=position(), to=from.add(getDeltaMovement());
        if (!level().isClientSide) {
            if (tickCount>30 || getOwner()==null || !getOwner().isAlive()) { discard(); return; }
            BlockHitResult block=level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            Vec3 limit=block.getType()==HitResult.Type.MISS ? to : block.getLocation();
            LivingEntity victim=null; Vec3 hit=limit; double nearest=from.distanceToSqr(limit);
            for (LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().expandTowards(getDeltaMovement()).inflate(.25),
                    e->e!=getOwner() && e.isAlive() && !e.isSpectator() && canHitEntity(e))) {
                var intersection=e.getBoundingBox().inflate(.25).clip(from,limit);
                Vec3 point=e.getBoundingBox().inflate(.25).contains(from)?from:intersection.orElse(null);
                if(point!=null && from.distanceToSqr(point)<=nearest){victim=e;hit=point;nearest=from.distanceToSqr(point);}
            }
            if(victim!=null || block.getType()!=HitResult.Type.MISS) {
                if(victim!=null) victim.hurt(damageSources().mobProjectile(this,(LivingEntity)getOwner()),damage);
                BeastVfx.emit(getOwner(),"yanwei_fox","impact",hit,getDeltaMovement(),16,false);
                discard();return;
            }
        }
        setPos(to); updateRotation();
    }
}
