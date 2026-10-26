package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.entity.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Real hurt/death events on disposable entities, with the connected player's state restored. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class CultivationCombatAudit {
    private static final List<Entity> fixtures=new ArrayList<>();
    private static int checks;
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void drops(LivingDropsEvent event) {
        if(fixtures.contains(event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void xp(LivingExperienceDropEvent event) {
        if(fixtures.contains(event.getEntity()))event.setDroppedExperience(0);
    }
    private static void check(boolean ok,String label) {checks++;if(!ok)throw new IllegalStateException(label);}
    private static Mob spawn(ServerPlayer player,String id,MobSpawnType source,boolean elite) {
        ServerLevel level=player.serverLevel();
        var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        Mob mob=(Mob)Objects.requireNonNull(type).create(level);Entity entity=mob;LivingEntity living=mob;
        entity.setPos(player.getX()+8,player.getY()+4,player.getZ()+8);mob.setNoAi(true);
        fixtures.add(entity);
        ForgeEventFactory.onFinalizeSpawn(mob,level,level.getCurrentDifficultyAt(entity.blockPosition()),source,null,null);
        level.addFreshEntity(entity);
        if(BeastRealmService.isBeast(entity)) {
            entity.getPersistentData().putInt(BeastResourceDrops.REALM,0);
            entity.getPersistentData().putInt(BeastRealmService.STAGE,0);
            entity.getPersistentData().putBoolean(BeastRealmService.ELITE,elite);
        }
        living.setHealth(1);return mob;
    }
    private static void kill(ServerPlayer player,PlayerCultivationData data,String id,MobSpawnType source,
                             boolean elite,boolean fought,double expected) {
        LivingEntity victim=spawn(player,id,source,elite);
        if(fought)MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(player,player.serverLevel().damageSources().mobAttack(victim),1));
        double before=data.progress();DamageSource damage=player.serverLevel().damageSources().playerAttack(player);
        check(victim.hurt(damage,1000000F)&&victim.isDeadOrDying(),"real death "+id);
        check(Math.abs(data.progress()-before-expected)<.0011,"reward "+id+" expected="+expected+" actual="+(data.progress()-before));
        double settled=data.progress();MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(victim,damage));
        check(data.progress()==settled,"duplicate death cannot award again");
    }
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiantucultivationaudit").requires(s->s.hasPermission(2)).executes(c->{
            var player=c.getSource().getPlayerOrException();var level=player.serverLevel();
            if(!level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||player.getVehicle()!=null) {
                c.getSource().sendFailure(Component.literal("Cultivation audit requires Xiantu-QA world and an unmounted player"));return 0;
            }
            var data=(PlayerCultivationData)player.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
            CompoundTag saved=new CompoundTag();data.save(saved);var mode=player.gameMode.getGameModeForPlayer();
            checks=0;fixtures.clear();boolean passed=false;String error="";
            try {
                player.setGameMode(GameType.SURVIVAL);data.setCharacterInitialized(true);data.setCultivationUnlocked(true);
                data.setRealmStage(0,0);data.setProgress(0);data.gongfa().clear();data.setInnerDemon(0);data.setPhysique(PhysiqueType.ORDINARY);
                data.resetSpiritRoots();for(var root:SpiritRootType.values())data.addSpiritRoot(root);
                check(Math.abs(CultivationEfficiency.personal(data)-.6)<1e-9,"jade five roots .60");
                data.resetSpiritRoots();data.addSpiritRoot(SpiritRootType.METAL);
                check(Math.abs(CultivationEfficiency.personal(data)-2)<1e-9,"jade one root 2.00");
                kill(player,data,"xiantu:yanwei_fox",MobSpawnType.NATURAL,false,false,1.65);
                kill(player,data,"xiantu:yanwei_fox",MobSpawnType.NATURAL,true,false,4.95);
                kill(player,data,"xiantu:yanwei_fox",MobSpawnType.COMMAND,false,false,0);
                kill(player,data,"xiantu:yanwei_fox",MobSpawnType.SPAWN_EGG,false,false,0);
                kill(player,data,"xiantu:vine_deer",MobSpawnType.NATURAL,false,false,0);
                kill(player,data,"xiantu:rock_back_boar",MobSpawnType.NATURAL,false,false,0);
                kill(player,data,"xiantu:rock_back_boar",MobSpawnType.NATURAL,false,true,1.65);
                kill(player,data,"minecraft:zombie",MobSpawnType.SPAWNER,false,false,1.65);
                kill(player,data,"minecraft:zombie",MobSpawnType.COMMAND,false,false,0);
                kill(player,data,"minecraft:enderman",MobSpawnType.NATURAL,false,false,0);
                data.setCultivationUnlocked(false);kill(player,data,"minecraft:zombie",MobSpawnType.NATURAL,false,false,0);
                data.setCultivationUnlocked(true);player.setGameMode(GameType.CREATIVE);
                kill(player,data,"minecraft:zombie",MobSpawnType.NATURAL,false,false,0);player.setGameMode(GameType.SURVIVAL);
                data.setProgress(data.progressMax()-.2);kill(player,data,"minecraft:zombie",MobSpawnType.NATURAL,false,false,.2);
                kill(player,data,"minecraft:zombie",MobSpawnType.NATURAL,false,false,0);data.setProgress(0);
                LivingEntity environmental=spawn(player,"minecraft:zombie",MobSpawnType.NATURAL,false);
                MinecraftForge.EVENT_BUS.post(new LivingDamageEvent(environmental,level.damageSources().playerAttack(player),1));
                check(environmental.hurt(level.damageSources().generic(),1000000F),"environmental death");
                check(data.progress()==0,"environmental final damage grants no cultivation");
                Wolf wolf=(Wolf)spawn(player,"minecraft:wolf",MobSpawnType.COMMAND,false);TamableAnimal pet=wolf;pet.setOwnerUUID(player.getUUID());pet.setTame(true);
                LivingEntity petVictim=spawn(player,"minecraft:zombie",MobSpawnType.NATURAL,false);
                check(petVictim.hurt(level.damageSources().mobAttack(wolf),1000000F)&&petVictim.isDeadOrDying(),"pet actual fatal damage");
                check(Math.abs(data.progress()-1.65)<.0011,"pet credits online nearby owner");
                // The event adapter rejects a stale owner record and a final hit from another source.
                LivingEntity stale=spawn(player,"minecraft:zombie",MobSpawnType.NATURAL,false);
                MinecraftForge.EVENT_BUS.post(new LivingDamageEvent(stale,level.damageSources().playerAttack(player),1));
                ((Entity)stale).getPersistentData().putLong("xiantu_cultivation_hit_at",level.getGameTime()-601);
                double before=data.progress();MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(stale,level.damageSources().playerAttack(player)));
                check(data.progress()==before,"stale attribution rejected");
                passed=true;c.getSource().sendSuccess(()->Component.literal("Cultivation combat audit PASS: "+checks),false);
                return checks;
            } catch(Exception ex) {error=ex.toString();ex.printStackTrace();c.getSource().sendFailure(Component.literal("Cultivation audit FAIL: "+ex));return 0;}
            finally {
                fixtures.forEach(Entity::discard);fixtures.clear();data.load(saved);player.setGameMode(mode);CultivationEvents.sync(player);
                try {
                    var result=new com.google.gson.JsonObject();result.addProperty("passed",passed);result.addProperty("checks",checks);result.addProperty("error",error);
                    java.nio.file.Files.writeString(level.getServer().getServerDirectory().toPath().resolve("xiantu-cultivation-audit.json"),result.toString());
                }catch(Exception ex){ex.printStackTrace();}
            }
        }));
    }
}
