package com.longju.xiantu.entity;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

/** Saved server identity; fresh natural spawns may use the nearest player's realm. */
@Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID)
public final class BeastRealmService {
    public static final String STAGE="xiantu_beast_stage",PROGRESS="xiantu_beast_progress",ELITE="xiantu_beast_elite",
        ELEMENT="xiantu_beast_element",SPECIES="xiantu_beast_species",HOME="xiantu_beast_home",INITIALIZED="xiantu_beast_initialized";
    private static final String CALM="xiantu_beast_calm_since",LOST="xiantu_beast_lost_since",DRY="xiantu_beast_dry_since";
    public static void setup(FMLCommonSetupEvent e){e.enqueueWork(()->{
        var attribute=(RangedAttribute)Attributes.MAX_HEALTH;
        attribute.maxValue=Math.max(attribute.getMaxValue(),200000);
    });}
    public static String species(Entity e){var key=ForgeRegistries.ENTITY_TYPES.getKey(e.getType());return key!=null&&key.getNamespace().equals("xiantu")&&BeastResourceRules.SPECIES.contains(key.getPath())?key.getPath():"";}
    public static boolean isBeast(Entity e){return !species(e).isEmpty();}
    public static int realm(Entity e){return e.getPersistentData().getInt(BeastResourceDrops.REALM);}
    public static boolean elite(Entity e){return e.getPersistentData().getBoolean(ELITE);}
    public static boolean valid(Entity e){var d=e.getPersistentData();return d.getBoolean(INITIALIZED)&&d.contains(BeastResourceDrops.REALM,Tag.TAG_INT)&&BeastRealmRules.valid(realm(e),d.getInt(STAGE));}
    public static void initialize(Mob mob){
        if(!(mob.level() instanceof ServerLevel level)||!isBeast(mob))return;
        var d=mob.getPersistentData();boolean fresh=!d.getBoolean(INITIALIZED);
        if(fresh){
            var s=BeastEcology.species(species(mob));if(s==null)return;
            int tier=BeastEcology.tier(level,mob.blockPosition(),s.element());
            int realm=NearbySpawnRealms.roll(mob,BeastEcology.profile().weights().get(tier));
            if(realm<0)realm=BeastRealmRules.roll(BeastEcology.profile().weights().get(tier),mob.getRandom().nextDouble());
            int stage=mob.getRandom().nextInt(RealmRegistry.STAGE_COUNTS[realm]);
            d.putInt(BeastResourceDrops.REALM,realm);d.putInt(STAGE,stage);
            d.putLong(PROGRESS,mob.getRandom().nextInt((int)RealmRegistry.get(realm,stage).progressCap()));
            d.putString(SPECIES,s.id());d.putString(ELEMENT,s.element());d.putBoolean(ELITE,s.elite());
            d.putLong(HOME,mob.blockPosition().asLong());d.putLong(CALM,level.getGameTime());d.putBoolean(INITIALIZED,true);
        }
        if(!valid(mob))return; // Never reroll a corrupted/unsupported saved realm.
        int realm=realm(mob),stage=d.getInt(STAGE);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BeastRealmRules.health(realm,stage,elite(mob)));
        mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BeastRealmRules.attack(realm,stage,elite(mob)));
        if(fresh)mob.setHealth(mob.getMaxHealth());
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){if(!e.getLevel().isClientSide&&e.getEntity() instanceof Mob m)initialize(m);}
    @SubscribeEvent public static void track(PlayerEvent.StartTracking e){
        if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof Mob m&&isBeast(m)){
            initialize(m);if(valid(m))XiantuNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),BeastRealmPacket.of(m));
        }
    }
    public static boolean peacefulSpecies(String id){
        if(java.util.Set.of("vine_deer","thunder_deer","iron_tooth_badger").contains(id))return true;
        return CompletionBestiary.ALL.stream().anyMatch(d->d.id().equals(id)&&d.passive()&&!d.boss());
    }
    public static boolean mayTarget(Mob mob,LivingEntity target){
        if(!(mob.level() instanceof ServerLevel level)||!valid(mob))return true;
        if(BeastSpawnProtection.inTown(level,mob.blockPosition())||BeastSpawnProtection.inTown(level,target.blockPosition()))return false;
        if(!(target instanceof ServerPlayer player))return true;
        if(mob.getLastHurtByMob()==player&&mob.tickCount-mob.getLastHurtByMobTimestamp()<=200)return true;
        if(mob.getPersistentData().getString(ELEMENT).equals("wood")&&level.getBiome(mob.blockPosition()).is(new net.minecraft.resources.ResourceLocation("minecraft:mushroom_fields")))return false;
        int playerRealm=player.getCapability(CultivationCapability.DATA).map(d->d.cultivationUnlocked()?d.majorRealm():-1).orElse(-1);
        return realm(mob)-playerRealm<2||mob.distanceToSqr(player)<=4;
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent e){if(e.getEntity() instanceof Mob m&&isBeast(m)&&e.getNewTarget()!=null&&!mayTarget(m,e.getNewTarget()))e.setNewTarget(null);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(LivingAttackEvent e){
        if(e.getEntity().level() instanceof ServerLevel level){
            if(e.getSource().getEntity() instanceof Mob m&&isBeast(m)&&!mayTarget(m,e.getEntity())){e.setCanceled(true);return;}
            var d=e.getEntity().getPersistentData();if(isBeast(e.getEntity())&&d.getString(ELEMENT).equals("fire")
                &&(e.getSource().is(DamageTypes.IN_FIRE)||e.getSource().is(DamageTypes.ON_FIRE)||e.getSource().is(DamageTypes.LAVA)||e.getSource().is(DamageTypes.HOT_FLOOR)))e.setCanceled(true);
        }
    }
    @SubscribeEvent public static void hurt(LivingDamageEvent e){
        if(!e.getEntity().level().isClientSide&&isBeast(e.getEntity())&&e.getAmount()>0)e.getEntity().getPersistentData().putLong(CALM,e.getEntity().level().getGameTime());
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent e){
        if(!(e.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level)||!isBeast(mob)||mob.tickCount%20!=0||!mob.isAlive())return;
        if(!valid(mob))initialize(mob);if(!valid(mob))return;
        var d=mob.getPersistentData();long now=level.getGameTime();var home=BlockPos.of(d.getLong(HOME));
        var target=mob.getTarget();
        if(target!=null){
            d.putLong(CALM,now);
            // Ranged attackers deliberately stop navigating while casting; that is not a failed path.
            boolean unreachable=!mob.getSensing().hasLineOfSight(target);
            if(!unreachable)d.remove(LOST);else if(!d.contains(LOST))d.putLong(LOST,now);
            if(!mayTarget(mob,target)||mob.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(home))>24*24
                ||(d.contains(LOST)&&now-d.getLong(LOST)>=200)){
                mob.setTarget(null);mob.getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1);
            }
        }else{
            d.remove(LOST);
            if(mob.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(home))>24*24)mob.getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1);
            if(now-d.getLong(CALM)>=400)mob.heal(mob.getMaxHealth()*.05F);
            if(d.getBoolean(BeastResourceDrops.NATURAL)&&!mob.hasCustomName()&&!mob.isPersistenceRequired()){
                var nearest=level.getNearestPlayer(mob,-1);if(nearest==null||nearest.distanceToSqr(mob)>128*128){mob.discard();return;}
            }
        }
        if(d.getString(ELEMENT).equals("water")&&mob.isInWaterOrBubble())mob.setAirSupply(mob.getMaxAirSupply());
        if(java.util.Set.of("tide_pattern_ray","azure_scale_python").contains(species(mob))){
            if(mob.isInWaterOrBubble())d.remove(DRY);
            else if(!d.contains(DRY))d.putLong(DRY,now);
            else if(now-d.getLong(DRY)>=600)mob.hurt(level.damageSources().dryOut(),2);
        }
        if(d.getString(ELEMENT).equals("fire"))mob.clearFire();
    }
    private BeastRealmService(){}
}
