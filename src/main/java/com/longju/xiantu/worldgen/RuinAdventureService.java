package com.longju.xiantu.worldgen;

import com.google.gson.*;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.building.RewardChestBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu")
public final class RuinAdventureService {
    public static final String BOSS="xiantu_ruin_boss",WAVE="xiantu_ruin_wave";
    public static RuinAdventureData.Instance observe(ServerLevel l,String root,String name,BlockPos origin){
        if(!RuinEncounterService.key(l,name,origin).equals(root))throw new IllegalArgumentException("Invalid ruin identity");
        var result=RuinAdventureData.get(l).ensure(root,name,origin,RuinSpecs.load(l.getServer(),name));if(!RuinInstanceData.get(l).placed(root))RuinInstanceData.get(l).place(root);return result;
    }
    public static CompoundTag binding(String root,String name,BlockPos origin){var n=new CompoundTag();n.putString("RuinRoot",root);n.putString("RuinBlueprint",name);n.putLong("RuinOrigin",origin.asLong());return n;}
    /** Pure block-entity configuration, also safe to use on a worldgen region's own chunk. */
    public static CompoundTag sealData(String root,String name,BlockPos origin,JsonObject e){
        var n=binding(root,name,origin);String wave=e.get("id").getAsString();n.putString("Instance",RuinSpecs.waveKey(root,wave));n.putString("RuinWave",wave);
        n.putInt("Count",e.get("count").getAsInt());n.putInt("MaxRealm",e.get("maxRealm").getAsInt());n.putInt("MinRealm",e.has("minRealm")?e.get("minRealm").getAsInt():0);
        if(e.has("bounds")){int[] bounds=new int[6];var a=e.getAsJsonArray("bounds");for(int i=0;i<6;i++)bounds[i]=a.get(i).getAsInt()+switch(i%3){case 0->origin.getX();case 1->origin.getY();default->origin.getZ();};n.putIntArray("RoomBounds",bounds);}
        var list=new ListTag();for(var s:e.getAsJsonArray("species"))list.add(StringTag.valueOf(s.getAsString()));n.put("Pool",list);return n;
    }
    public static CompoundTag chestData(String root,String name,BlockPos origin,JsonObject m){
        var n=binding(root,name,origin);n.putString("RuinChest",m.get("id").getAsString());n.putString("RuinGrade",m.get("grade").getAsString());
        n.putString("LootTable",m.get("loot").getAsString());n.putLong("LootTableSeed",UUID.fromString(RuinSpecs.waveKey(root,m.get("id").getAsString())).getLeastSignificantBits());return n;
    }
    public static void initialize(ServerLevel l,BlockPos origin,JsonObject spec,String root){
        var name=spec.get("id").getAsString();var instance=RuinAdventureData.get(l).ensure(root,name,origin,spec);
        RuinProtection.internal(()->{
            if(!instance.state.getBoolean("ResourcesInitialized")&&spec.has("resourceNodes"))for(var v:spec.getAsJsonArray("resourceNodes")){var m=v.getAsJsonObject();var at=RuinSpecs.offset(origin,m.getAsJsonArray("position"));
                var ore=m.has("resource")&&m.get("resource").getAsString().equals("spirit_gold_essence")?com.longju.xiantu.refining.QitianMaterials.ORE.get():com.longju.xiantu.mining.AdvancedMinerals.ORES.get("causality_stone").get();
                l.setBlock(at,ore.defaultBlockState(),3);
            }
            for(var v:spec.getAsJsonArray("encounters")){var e=v.getAsJsonObject();var p=RuinSpecs.offset(origin,e.getAsJsonArray("position"));l.setBlock(p,ModBlocks.RUIN_ENCOUNTER.get().defaultBlockState(),3);if(l.getBlockEntity(p)instanceof RuinEncounterBlockEntity be){be.load(sealData(root,name,origin,e));be.setChanged();}}
            for(var v:spec.getAsJsonArray("markers")){var m=v.getAsJsonObject();if(!m.get("type").getAsString().equals("reward"))continue;var p=RuinSpecs.offset(origin,m.getAsJsonArray("position"));if(l.getBlockEntity(p)instanceof RewardChestBlockEntity chest){chest.load(chestData(root,name,origin,m));chest.setChanged();}}
            return true;
        });instance.state.putBoolean("ResourcesInitialized",true);RuinAdventureData.get(l).setDirty();RuinInstanceData.get(l).place(root);
    }
    public static boolean mayTrigger(ServerLevel l,RuinEncounterBlockEntity be){
        var i=RuinAdventureData.get(l).find(be.root);if(i==null)return false;
        if(i.boss()&&i.spec.getAsJsonObject("bossPlan").get("trial").getAsString().equals("elements")){
            for(String theme:List.of("metal","wood","water","fire","earth"))for(var v:i.spec.getAsJsonArray("encounters")){var e=v.getAsJsonObject();if(!e.get("lootTheme").getAsString().equals(theme))continue;String wave=e.get("id").getAsString();if(!i.waveDone(wave))return be.wave.equals(wave);}
            return false;
        }return !i.waveDone(be.wave);
    }
    public static void waveSpawned(ServerLevel l,RuinEncounterBlockEntity be,List<Mob> mobs){
        var d=RuinAdventureData.get(l);var i=d.find(be.root);if(i==null)return;var waves=i.state.getCompound("Waves");var wave=new CompoundTag();var alive=new ListTag();for(var m:mobs)alive.add(StringTag.valueOf(m.getUUID().toString()));wave.put("Alive",alive);wave.putBoolean("Spawned",true);waves.put(be.wave,wave);i.state.put("Waves",waves);d.setDirty();
    }
    public static int realm(ServerPlayer p){return p.getCapability(CultivationCapability.DATA).map(d->d.cultivationUnlocked()?d.majorRealm():-1).orElse(-1);}
    public static boolean legal(ServerPlayer p,RuinAdventureData.Instance i){if(!p.isAlive()||p.isCreative()||p.isSpectator())return false;var r=i.spec.getAsJsonObject("bossPlan").getAsJsonArray("realmRange");int realm=realm(p);return realm>=r.get(0).getAsInt()&&realm<=r.get(1).getAsInt();}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e){
        if(!(e.getEntity().level()instanceof ServerLevel l))return;var n=e.getEntity().getPersistentData();if(!n.hasUUID(RuinEncounterService.SOURCE))return;
        var d=RuinAdventureData.get(l);var i=d.find(n.getUUID(RuinEncounterService.SOURCE).toString());if(i==null)return;
        if(n.getBoolean(BOSS)){
            if(i.defeated()||!i.state.hasUUID("BossUUID")||!i.state.getUUID("BossUUID").equals(e.getEntity().getUUID()))return;
            i.state.putBoolean("Defeated",true);i.state.putLong("DefeatedAt",l.getGameTime());
            // Only valid player kills receive the separate boss material transaction.
            if(e.getSource().getEntity()instanceof ServerPlayer p&&legal(p,i)&&!i.state.getBoolean("BossLoot")){
                i.state.putBoolean("BossLoot",true);i.state.putBoolean("RewardUnlocked",true);d.setDirty();String theme=i.blueprint.contains("starfall")?"thunder":i.blueprint.contains("water_curtain")?"wood":"earth";
                var cfg=i.spec.getAsJsonObject("bossPlan");var resource=cfg.has("loot")?new ResourceLocation(cfg.get("loot").getAsString()):new ResourceLocation("xiantu","chests/grand_core_"+theme);
                var table=l.getServer().getLootData().getLootTable(resource);
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(l).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,e.getEntity().position()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                for(var stack:table.getRandomItems(params))e.getEntity().spawnAtLocation(stack);
                var spell=com.longju.xiantu.spell.SpellSources.boss(p,i,e.getEntity());if(!spell.isEmpty())e.getEntity().spawnAtLocation(spell);
            }
        }else if(n.contains(WAVE)){
            String key=n.getString(WAVE);var waves=i.state.getCompound("Waves");var wave=waves.getCompound(key);var old=wave.getList("Alive",Tag.TAG_STRING);var next=new ListTag();
            for(var t:old)if(!t.getAsString().equals(e.getEntity().getUUID().toString()))next.add(t.copy());wave.put("Alive",next);if(wave.getBoolean("Spawned")&&next.isEmpty())wave.putBoolean("Done",true);waves.put(key,wave);i.state.put("Waves",waves);
        }d.setDirty();
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.level instanceof ServerLevel l)||l.getGameTime()%10!=0)return;
        var data=RuinAdventureData.get(l);var active=new LinkedHashSet<RuinAdventureData.Instance>();
        for(var p:l.players())for(var i:data.at(p.blockPosition()))if(i.bounds.inflate(8).contains(p.position()))active.add(i);
        for(var i:data.all())if(i.boss()&&i.state.getBoolean("BossCommitted")&&l.hasChunkAt(i.absolute(i.spec.getAsJsonObject("bossPlan").getAsJsonArray("position"))))active.add(i);
        for(var i:active){RuinVegetation.tick(l,i);if(i.boss()&&!i.defeated())bossTick(l,i);}
    }
    static void bossTick(ServerLevel l,RuinAdventureData.Instance i){
        var cfg=i.spec.getAsJsonObject("bossPlan");var state=i.state;var arena=i.arena();var home=i.absolute(cfg.getAsJsonArray("position"));
        for(var p:l.players())if(arena.contains(p.position())&&!p.isCreative()&&!p.isSpectator()&&!legal(p,i)){
            var entry=i.absolute(i.spec.getAsJsonArray("entry"));p.teleportTo(l,entry.getX()+.5,entry.getY(),entry.getZ()+.5,p.getYRot(),p.getXRot());p.displayClientMessage(Component.literal("此遗迹首领区域有境界限制"),true);
        }
        var party=l.players().stream().filter(p->legal(p,i)&&arena.contains(p.position())).toList();
        RuinTrials.tick(l,i,party);
        if(l.getDifficulty()==Difficulty.PEACEFUL)return;
        Mob boss=state.hasUUID("BossUUID")&&l.getEntity(state.getUUID("BossUUID"))instanceof Mob m?m:null;
        if(boss==null&&!state.getBoolean("BossSpawned")){
            if(!state.getBoolean("BossCommitted")&&(!i.guardiansDone()||!state.getBoolean("TrialComplete")||party.isEmpty()))return;
            if(!state.getBoolean("BossCommitted")){
                state.putBoolean("BossCommitted",true);state.putUUID("BossUUID",UUID.fromString(RuinSpecs.waveKey(i.id,"boss")));state.putInt("BossRealm",Math.max(3,party.stream().mapToInt(RuinAdventureService::realm).max().orElse(cfg.getAsJsonArray("realmRange").get(0).getAsInt())));state.putInt("Prepare",30);
                l.playSound(null,home,net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.HOSTILE,.8F,.65F);RuinAdventureData.get(l).setDirty();return;
            }
            int left=state.getInt("Prepare")-10;state.putInt("Prepare",left);if(left>0){RuinAdventureData.get(l).setDirty();return;}
            var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(cfg.get("entity").getAsString()));if(type==null||!(type.create(l)instanceof Mob m))return;
            m.setUUID(state.getUUID("BossUUID"));m.moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,0,0);m.setPersistenceRequired();if(!l.noCollision(m)){m.discard();return;}
            m.getPersistentData().putUUID(RuinEncounterService.SOURCE,UUID.fromString(i.id));m.getPersistentData().putBoolean(BOSS,true);KillCultivationService.registerEncounter(m,state.getInt("BossRealm"));
            if(!l.addFreshEntity(m)){m.discard();return;}state.putBoolean("BossSpawned",true);boss=m;
        }
        if(boss!=null&&boss.isAlive()){
            int count=Math.min(6,Math.max(1,party.size())),r=Math.max(3,state.getInt("BossRealm")),stage=boss.getPersistentData().getInt(com.longju.xiantu.entity.BeastRealmService.STAGE);double max=com.longju.xiantu.entity.BossBalance.health(r,stage)*RuinBossRules.healthScale(count),attack=com.longju.xiantu.entity.BossBalance.attack(r,stage)*RuinBossRules.attackScale(count);
            var hp=boss.getAttribute(Attributes.MAX_HEALTH);if(hp!=null&&hp.getBaseValue()!=max){float fraction=boss.getHealth()/boss.getMaxHealth();boolean fresh=!state.getBoolean("StatsApplied");hp.setBaseValue(max);boss.setHealth(fresh?boss.getMaxHealth():boss.getMaxHealth()*fraction);state.putBoolean("StatsApplied",true);}
            var damage=boss.getAttribute(Attributes.ATTACK_DAMAGE);if(damage!=null)damage.setBaseValue(attack);
            if(party.isEmpty()){state.putInt("EmptyTicks",state.getInt("EmptyTicks")+10);boss.setTarget(null);boss.getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1);if(state.getInt("EmptyTicks")>=300){boss.setHealth(boss.getMaxHealth());if(boss instanceof CompletionLandEntity c)c.resetRuinEncounter();}}
            else{state.putInt("EmptyTicks",0);if(boss.getTarget()==null||!(boss.getTarget()instanceof ServerPlayer p)||!party.contains(p)){final Mob bm=boss;boss.setTarget(party.stream().filter(p->bm.getSensing().hasLineOfSight(p)).min(Comparator.comparingDouble(bm::distanceToSqr)).orElse(null));}}
            if(!arena.deflate(1,0,1).contains(boss.position())){boss.setTarget(null);boss.getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1);boss.setDeltaMovement(boss.getDeltaMovement().multiply(.2,1,.2));}
        }RuinAdventureData.get(l).setDirty();
    }
    private RuinAdventureService(){}
}
