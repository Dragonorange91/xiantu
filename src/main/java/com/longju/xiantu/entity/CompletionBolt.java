package com.longju.xiantu.entity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
/** Bat's straight, colliding projectile; no tracking and no terrain edits. */
public final class CompletionBolt extends ThrowableItemProjectile {
    public CompletionBolt(EntityType<? extends CompletionBolt> type,Level level){super(type,level);}
    @Override protected Item getDefaultItem(){return Items.AIR;}
    @Override protected float getGravity(){return 0;}
    @Override public void tick(){super.tick();if(!level().isClientSide && tickCount>60)discard();}
    @Override protected void onHitEntity(EntityHitResult hit){
        super.onHitEntity(hit);
        if(!level().isClientSide && getOwner() instanceof LivingEntity owner && hit.getEntity()!=owner)
            hit.getEntity().hurt(damageSources().thrown(this,owner),(float)owner.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }
    @Override protected void onHit(HitResult hit){super.onHit(hit);if(!level().isClientSide){
        if(getOwner()!=null)BeastVfx.emit(getOwner(),"purple_lightning_bat","impact",hit.getLocation(),getDeltaMovement(),16,false);
        discard();}}
}
