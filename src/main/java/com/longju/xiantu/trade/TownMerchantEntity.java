package com.longju.xiantu.trade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Anchored shopkeepers; the introductory guide alone is immune to gameplay damage. */
public final class TownMerchantEntity extends PathfinderMob {
    private Vec3 anchor;
    private float facing;
    private boolean deathAccounted;
    private boolean naturalMarkerRecorded;
    public TownMerchantEntity(EntityType<? extends TownMerchantEntity> type, Level level) {
        super(type, level);
        setNoAi(true); setNoGravity(true); setPersistenceRequired();
    }
    public String profession() { return ForgeRegistries.ENTITY_TYPES.getKey(getType()).getPath(); }
    public boolean guide() { return profession().equals("town_guide"); }
    public boolean wanbao() { return profession().equals("wanbao_merchant"); }
    @Override public boolean isInvulnerableTo(DamageSource source) {
        return guide() || super.isInvulnerableTo(source);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        return !guide() && super.hurt(source, amount);
    }
    @Override protected void registerGoals() {}
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public void push(Entity other) {}
    @Override public void push(double x,double y,double z) {}
    @Override public void knockback(double strength,double x,double z) {}
    @Override public void travel(Vec3 direction) {}
    @Override public void move(MoverType type, Vec3 movement) {}
    @Override public boolean startRiding(Entity entity, boolean force) { return false; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public void tick() {
        if(!level().isClientSide&&!naturalMarkerRecorded&&getPersistentData().getBoolean("TownNatural")){
            TownMerchantPlacement.markNatural((net.minecraft.server.level.ServerLevel)level(),getPersistentData().getInt("TownOriginX"),getPersistentData().getInt("TownOriginZ"),profession());
            naturalMarkerRecorded=true;
        }
        if (!level().isClientSide && anchor == null) { anchor=position(); facing=getYRot(); }
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide && anchor != null) {
            setPos(anchor); setYRot(facing); setXRot(0); setYHeadRot(facing); setYBodyRot(facing);
        }
    }
    public boolean mayTrade(Player player) {
        return isAlive() && player.isAlive() && !player.isSpectator() && player.level()==level()
                && distanceToSqr(player)<=16 && player.hasLineOfSight(this);
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && mayTrade(server)) {
            if(guide())TownJourney.grantBook(server,this);
            TownMerchantMenu.open(server,this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override protected void dropAllDeathLoot(DamageSource source) {}
    @Override public void die(DamageSource source) {
        if (!level().isClientSide && !deathAccounted) {
            deathAccounted=true;
            Entity killer=source.getEntity();
            if (killer instanceof TamableAnimal pet) killer=pet.getOwner();
            if (killer instanceof ServerPlayer player) {
                var data=com.longju.xiantu.gongfa.GongfaService.data(player);
                data.setInnerDemon(data.innerDemon()+10);
                com.longju.xiantu.cultivation.CultivationEvents.sync(player);
            }
        }
        super.die(source);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if(anchor!=null) { tag.putDouble("ShopX",anchor.x);tag.putDouble("ShopY",anchor.y);tag.putDouble("ShopZ",anchor.z);tag.putFloat("ShopYaw",facing); }
        tag.putBoolean("ShopDeathAccounted",deathAccounted);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);setNoAi(true);setNoGravity(true);setPersistenceRequired();
        if(tag.contains("ShopX")) { anchor=new Vec3(tag.getDouble("ShopX"),tag.getDouble("ShopY"),tag.getDouble("ShopZ"));facing=tag.getFloat("ShopYaw"); }
        deathAccounted=tag.getBoolean("ShopDeathAccounted");
    }
}
