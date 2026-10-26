package com.longju.xiantu.alchemy;
import com.longju.xiantu.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import java.util.*;

/** Stationary visual lure. No AI, combat, equipment drops or persistent/offline lifetime. */
public final class PillDecoyEntity extends Zombie {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(PillDecoyEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private long expiry;
    public PillDecoyEntity(EntityType<? extends Zombie> type,Level level){super(type,level);setNoAi(true);xpReward=0;setPersistenceRequired();}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(OWNER,Optional.empty());}
    public UUID ownerId(){return entityData.get(OWNER).orElse(null);}
    public static void spawn(ServerPlayer p,int seconds){
        var d=ModEntities.PILL_DECOY.get().create(p.serverLevel());if(d==null)throw new IllegalStateException("Cannot create decoy");
        d.entityData.set(OWNER,Optional.of(p.getUUID()));d.expiry=MedicineService.now(p)+seconds*20L;
        d.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),0);d.setYHeadRot(p.getYHeadRot());
        for(var slot:EquipmentSlot.values()){d.setItemSlot(slot,p.getItemBySlot(slot).copy());d.setDropChance(slot,0);}
        d.addTag("xiantu_illusion");p.serverLevel().addFreshEntity(d);
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        var server=(net.minecraft.server.level.ServerLevel)level();var p=ownerId()==null?null:server.getServer().getPlayerList().getPlayer(ownerId());
        if(p==null||!p.isAlive()||p.level()!=level()||MedicineService.now(p)>=expiry){discard();return;}
        if(tickCount%10==0)for(Mob mob:level().getEntitiesOfClass(Mob.class,getBoundingBox().inflate(16))){
            String id=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getPath();
            if(mob==this||mob.getTarget()!=p||!idAllowed(id)||!mob.hasLineOfSight(this))continue;
            mob.setTarget(this);
        }
    }
    private static boolean idAllowed(String id){return com.longju.xiantu.entity.BeastResourceRules.SPECIES.contains(id);}
    @Override protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source){ }
    @Override public boolean doHurtTarget(Entity e){return false;}
    @Override public boolean shouldBeSaved(){return false;}
    @Override public boolean isBaby(){return false;}
    @Override protected boolean isSunSensitive(){return false;}
    @Override protected boolean convertsInWater(){return false;}
}
