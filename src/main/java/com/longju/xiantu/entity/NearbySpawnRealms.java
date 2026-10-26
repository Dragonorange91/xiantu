package com.longju.xiantu.entity;

import com.longju.xiantu.cultivation.CultivationCapability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Only fresh Overworld natural spawns adapt; saved identities and encounter tiers stay authoritative. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class NearbySpawnRealms {
    private static final String NATURAL="xiantu_nearby_realm_natural";
    public static final double RADIUS=96;

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn event){
        Mob mob=event.getEntity();
        if(mob.level().dimension()!=Level.OVERWORLD||BeastRealmService.valid(mob)
            ||mob.getPersistentData().contains(BeastResourceDrops.REALM)
            ||HostileRealmService.boss(mob))return;
        if(event.getSpawnType()!=MobSpawnType.NATURAL&&event.getSpawnType()!=MobSpawnType.CHUNK_GENERATION)return;
        if(BeastRealmService.isBeast(mob)||HostileRealmService.eligible(mob))mob.getPersistentData().putBoolean(NATURAL,true);
    }

    /** -1 means no local context: caller falls back to the existing ecology distribution. */
    public static int roll(Mob mob,double[] regionWeights){
        if(!(mob.level() instanceof ServerLevel level)||level.dimension()!=Level.OVERWORLD
            ||!mob.getPersistentData().getBoolean(NATURAL)||HostileRealmService.boss(mob))return -1;
        ServerPlayer nearest=null;double distance=RADIUS*RADIUS;
        for(var player:level.players()){
            if(!player.isAlive()||player.isSpectator())continue;
            double candidate=player.distanceToSqr(mob);
            if(candidate>distance)continue;
            if(nearest==null||candidate<distance||player.getUUID().compareTo(nearest.getUUID())<0){nearest=player;distance=candidate;}
        }
        if(nearest==null)return -1;
        int realm=nearest.getCapability(CultivationCapability.DATA)
            .map(data->data.cultivationUnlocked()?data.majorRealm():0).orElse(0);
        return BeastRealmRules.naturalRealm(realm,regionWeights,mob.getRandom().nextDouble());
    }
    private NearbySpawnRealms(){}
}
