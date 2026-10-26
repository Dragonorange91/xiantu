package com.longju.xiantu.cultivation;

import com.longju.xiantu.entity.*;
import com.longju.xiantu.gongfa.*;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Locale;

@Mod.EventBusSubscriber(modid="xiantu")
public final class KillCultivationService {
    private KillCultivationService() {}
    private static final String SOURCE="xiantu_cultivation_spawn_allowed", HIT_BY="xiantu_cultivation_hit_by",
        HIT_AT="xiantu_cultivation_hit_at", SETTLED="xiantu_cultivation_death_settled",
        FOUGHT="xiantu_cultivation_fought", FOUGHT_AT="xiantu_cultivation_fought_at",
        ENCOUNTER="xiantu_cultivation_encounter";
    public record Target(int realm, boolean elite, boolean boss) {}

    /** Provenance is recorded at spawn, never inferred from a name or the player's realm. */
    @SubscribeEvent public static void spawned(MobSpawnEvent.FinalizeSpawn event) {
        var mob=event.getEntity();var tag=mob.getPersistentData();
        if(tag.contains(SOURCE))return;
        boolean natural=event.getSpawnType()==MobSpawnType.NATURAL||event.getSpawnType()==MobSpawnType.CHUNK_GENERATION;
        boolean vanillaSource=switch(event.getSpawnType()) {
            case NATURAL, CHUNK_GENERATION, SPAWNER, STRUCTURE, PATROL, EVENT, REINFORCEMENT -> true;
            case MOB_SUMMONED -> mob instanceof WitherBoss;
            default -> false;
        };
        tag.putBoolean(SOURCE,BeastRealmService.isBeast(mob)?natural:vanillaSource);
    }
    /** Future structure/event spawners call this only after their unique encounter is committed. */
    public static void registerEncounter(Mob boss,int realm) {
        if(boss.level().isClientSide||realm<0||realm>8)throw new IllegalArgumentException("Invalid boss encounter");
        var id=ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());
        if(id==null||!id.getNamespace().equals("xiantu")||CompletionBestiary.ALL.stream().noneMatch(d->d.boss()&&d.id().equals(id.getPath())))
            throw new IllegalArgumentException("Unregistered encounter boss");
        boss.getPersistentData().putBoolean(ENCOUNTER,true);
        boss.getPersistentData().putInt(BeastResourceDrops.REALM,Math.max(3,realm));
    }
    private static ServerPlayer owner(Entity attacker,LivingEntity victim) {
        ServerPlayer p=attacker instanceof ServerPlayer player?player:
                attacker instanceof TamableAnimal pet&&pet.getOwner() instanceof ServerPlayer player&&player.distanceToSqr(victim)<=4096?player:null;
        return p!=null&&!(p instanceof FakePlayer)&&p.isAlive()&&p.level()==victim.level()
                &&p.getServer()!=null&&p.getServer().getPlayerList().getPlayer(p.getUUID())==p?p:null;
    }
    /** A neutral must actually try to hit this player (or their pet), not merely gain a target. */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void attacked(LivingAttackEvent event) {
        if(event.getEntity().level().isClientSide||event.getAmount()<=0||!(event.getSource().getEntity() instanceof Mob attacker))return;
        ServerPlayer player=owner(event.getEntity(),attacker);
        if(player!=null&&!player.isCreative()&&!player.isSpectator()) {
            attacker.getPersistentData().putUUID(FOUGHT,player.getUUID());
            attacker.getPersistentData().putLong(FOUGHT_AT,attacker.level().getGameTime());
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damaged(LivingDamageEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||event.getAmount()<=0)return;
        var player=owner(event.getSource().getEntity(),victim);
        if(player!=null) {
            victim.getPersistentData().putUUID(HIT_BY,player.getUUID());
            victim.getPersistentData().putLong(HIT_AT,victim.level().getGameTime());
        }
    }
    public static Target target(LivingEntity victim,ServerPlayer player) {
        var tag=victim.getPersistentData();
        if(victim instanceof net.minecraft.world.entity.player.Player || tag.getBoolean("xiantu_no_method_experience"))return null;
        if(BeastRealmService.isBeast(victim)) {
            String species=BeastRealmService.species(victim);
            if(!BeastRealmService.valid(victim)||!BeastResourceDrops.eligibleSource(victim)||BeastRealmService.peacefulSpecies(species))return null;
            var definition=CompletionBestiary.ALL.stream().filter(d->d.id().equals(species)).findFirst().orElse(null);
            boolean neutral=definition!=null?definition.behavior().equals("N"):
                    java.util.Set.of("rock_back_boar","thunder_feather_raven","thick_horn_rhino").contains(species);
            if(neutral&&!fought(victim,player))return null;
            return new Target(BeastRealmService.realm(victim),BeastRealmService.elite(victim),false);
        }
        if(tag.getBoolean(ENCOUNTER)&&tag.contains(BeastResourceDrops.REALM,Tag.TAG_INT)) {
            var id=ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
            if(id!=null&&id.getNamespace().equals("xiantu")&&CompletionBestiary.ALL.stream().anyMatch(d->d.boss()&&d.id().equals(id.getPath())))
                return new Target(tag.getInt(BeastResourceDrops.REALM),false,true);
        }
        boolean dragonEncounter=!tag.contains(SOURCE)&&victim instanceof EnderDragon dragon
                &&dragon.getDragonFight()!=null&&victim.getUUID().equals(dragon.getDragonFight().getDragonUUID());
        if(!tag.getBoolean(SOURCE)&&!dragonEncounter)return null;
        if(victim instanceof NeutralMob&&!fought(victim,player))return null;
        var registered=GongfaEvents.target(victim);
        if(registered.eligible())return new Target(registered.realmRank()-1,registered.elite(),registered.boss());
        var id=ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        return id!=null&&id.getNamespace().equals("minecraft")&&victim instanceof Enemy?new Target(0,false,false):null;
    }
    private static boolean fought(LivingEntity victim,ServerPlayer player) {
        var tag=victim.getPersistentData();long elapsed=victim.level().getGameTime()-tag.getLong(FOUGHT_AT);
        return tag.hasUUID(FOUGHT)&&tag.getUUID(FOUGHT).equals(player.getUUID())&&elapsed>=0&&elapsed<=600;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        var victim=event.getEntity();if(victim.level().isClientSide)return;
        var tag=victim.getPersistentData();if(tag.getBoolean(SETTLED))return;
        var player=owner(event.getSource().getEntity(),victim);
        if(player==null||!tag.hasUUID(HIT_BY)||!tag.getUUID(HIT_BY).equals(player.getUUID()))return;
        long elapsed=victim.level().getGameTime()-tag.getLong(HIT_AT);
        if(elapsed<0||elapsed>600)return;
        Target target=target(victim,player);if(target==null)return;
        // A death is consumed independently of player capability/rollback or a full stage.
        tag.putBoolean(SETTLED,true);
        if(player.isCreative()||player.isSpectator()||GongfaService.busy(player)
                ||ThunderTribulationService.active(player)||InnerDemonService.active(player))return;
        var capability=player.getCapability(CultivationCapability.DATA).orElse(null);
        if(!(capability instanceof PlayerCultivationData data)||!GongfaService.canMeditate(data))return;
        double gain=KillCultivationRules.reward(data.progressMax(),data.progress(),data.majorRealm(),target.realm,target.elite,target.boss);
        if(gain<=0){
            if(data.progress()>=data.progressMax())player.displayClientMessage(Component.literal("修为已满"),true);
            return;
        }
        double before=data.progress();data.accumulateMeditation(gain);CultivationEvents.sync(player);
        player.displayClientMessage(Component.literal(String.format(Locale.ROOT,"修为 +%.2f",data.progress()-before)),true);
    }
}
