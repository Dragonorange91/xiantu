package com.longju.xiantu.worldgen;
import com.google.gson.*;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.registry.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class RuinEncounterService {
    public static final String SOURCE="xiantu_ruin_instance";
    public static String key(ServerLevel l,String name,BlockPos origin){return UUID.nameUUIDFromBytes((l.dimension().location()+":"+name+":"+origin.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();}
    public static void validate(JsonObject spec){
        if(spec.has("encounters")){for(var value:spec.getAsJsonArray("encounters")){var single=spec.deepCopy();single.remove("encounters");single.add("encounter",value);validate(single);}return;}
        var e=spec.getAsJsonObject("encounter");if(e==null)throw new IllegalArgumentException("Missing encounter configuration");
        int count=e.get("count").getAsInt(),cap=e.get("maxRealm").getAsInt();
        int floor=e.has("minRealm")?e.get("minRealm").getAsInt():0;
        if(floor<0||floor>cap)throw new IllegalArgumentException("Invalid guardian minimum realm");
        if(e.has("bounds")){
            var bounds=e.getAsJsonArray("bounds");if(bounds.size()!=6)throw new IllegalArgumentException("Invalid guardian room bounds");
            for(int i=0;i<3;i++)if(bounds.get(i).getAsInt()>bounds.get(i+3).getAsInt())throw new IllegalArgumentException("Inverted guardian room bounds");
        }
        if(count<3||count>12||cap<0||cap>8||e.getAsJsonArray("species").isEmpty())throw new IllegalArgumentException("Invalid guardian count or realm cap");
        for(var v:e.getAsJsonArray("species")){String id=v.getAsString();
            if(BeastEcology.species(id)==null||BeastRealmService.peacefulSpecies(id))throw new IllegalArgumentException("Invalid ruin guardian "+id);
        }
        var pos=e.getAsJsonArray("position");var size=spec.getAsJsonArray("size");
        for(int i=0;i<3;i++)if(pos.get(i).getAsInt()<0||pos.get(i).getAsInt()>=size.get(i).getAsInt())throw new IllegalArgumentException("Encounter outside blueprint");
        if(!Set.of("metal","wood","water","fire","earth","thunder").contains(e.get("lootTheme").getAsString()))throw new IllegalArgumentException("Invalid loot theme");
    }
    public static void initialize(ServerLevel l,BlockPos o,JsonObject spec,String instance){
        if(spec.has("encounters")){RuinAdventureService.initialize(l,o,spec,instance);return;}
        var encounter=spec.getAsJsonObject("encounter");if(encounter==null)return;
        var p=encounter.getAsJsonArray("position");BlockPos anchor=o.offset(p.get(0).getAsInt(),p.get(1).getAsInt(),p.get(2).getAsInt());
        List<String> pool=new ArrayList<>();for(var v:encounter.getAsJsonArray("species")){
            String id=v.getAsString();var s=BeastEcology.species(id);if(s==null||BeastRealmService.peacefulSpecies(id))throw new IllegalArgumentException("Invalid ruin guardian "+id);pool.add(id);
        }
        if(pool.isEmpty())throw new IllegalArgumentException("Empty ruin guardian pool");
        l.setBlock(anchor,ModBlocks.RUIN_ENCOUNTER.get().defaultBlockState(),3);
        if(l.getBlockEntity(anchor) instanceof RuinEncounterBlockEntity be)be.configure(instance,pool,encounter.get("count").getAsInt(),encounter.get("maxRealm").getAsInt());
        // Native loot is generated once on first open and then saved by the container.
        for(var v:spec.getAsJsonArray("markers")){
            JsonObject m=v.getAsJsonObject();if(!m.get("type").getAsString().equals("reward"))continue;
            var a=m.getAsJsonArray("position");var at=o.offset(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());
            if(l.getBlockEntity(at) instanceof RandomizableContainerBlockEntity chest)chest.setLootTable(new ResourceLocation("xiantu","chests/ruin_"+encounter.get("lootTheme").getAsString()),l.random.nextLong());
        }
        RuinInstanceData.get(l).place(instance);
    }
    public static void spawnWave(ServerLevel l,BlockPos anchor,RuinEncounterBlockEntity be){
        var ledger=RuinInstanceData.get(l);if(ledger.triggered(be.instance))return;
        // Prepare the whole wave before committing. No partial wave or repeated spawning per tick.
        List<Mob> ready=new ArrayList<>();
        for(int index=0;index<be.count;index++){
            String id=be.pool.get(index%be.pool.size());var species=BeastEcology.species(id);
            var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("xiantu",id));if(type==null||species==null)return;
            if(!(type.create(l) instanceof Mob mob)){ready.forEach(Entity::discard);return;}
            Mob candidate=null;
            search:for(int radius=1;radius<=10;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                for(int dy:new int[]{1,0,2,-1,3,-2}){
                    BlockPos p=anchor.offset(dx,dy,dz);if(!l.hasChunkAt(p)||!l.getFluidState(p).isEmpty()||!l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP))continue;
                    mob.moveTo(p.getX()+.5,p.getY()+(species.mode().equals("fly")?2:0),p.getZ()+.5,l.random.nextFloat()*360,0);
                    AABB box=mob.getBoundingBox();
                    if(!be.inRoom(box.minX,box.minY,box.minZ)||!be.inRoom(box.maxX,box.maxY,box.maxZ))continue;
                    if(!l.getWorldBorder().isWithinBounds(box)||!l.hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))continue;
                    if(!l.noCollision(mob)||l.containsAnyLiquid(box)||ready.stream().anyMatch(other->other.getBoundingBox().inflate(.25).intersects(box)))continue;
                    ForgeEventFactory.onFinalizeSpawn(mob,l,l.getCurrentDifficultyAt(p),MobSpawnType.STRUCTURE,null,null);
                    if(mob.isSpawnCancelled()){mob.discard();ready.forEach(Entity::discard);return;}
                    int realm=be.minRealm+l.random.nextInt(be.maxRealm-be.minRealm+1),stage=l.random.nextInt(com.longju.xiantu.cultivation.RealmRegistry.STAGE_COUNTS[realm]);
                    var n=mob.getPersistentData();n.putUUID(SOURCE,UUID.fromString(be.root.isEmpty()?be.instance:be.root));if(!be.root.isEmpty()){n.putString("xiantu_ruin_wave",be.wave);mob.setPersistenceRequired();}n.putInt(BeastResourceDrops.REALM,realm);n.putInt(BeastRealmService.STAGE,stage);n.putLong(BeastRealmService.PROGRESS,0);n.putString(BeastRealmService.SPECIES,id);n.putString(BeastRealmService.ELEMENT,species.element());n.putBoolean(BeastRealmService.ELITE,species.elite());n.putLong(BeastRealmService.HOME,p.asLong());n.putBoolean(BeastRealmService.INITIALIZED,true);
                    BeastRealmService.initialize(mob);mob.setHealth(mob.getMaxHealth());
                    // One-shot wave members persist through unload so unfinished cores cannot deadlock.
                    candidate=mob;break search;
                }
            }
            if(candidate==null){mob.discard();ready.forEach(Entity::discard);return;}
            ready.add(candidate);
        }
        // Entity-join hooks may cancel an insertion. Roll back this prepared wave,
        // leaving the seal available, instead of committing an incomplete encounter.
        for(Mob mob:ready)if(!l.addFreshEntity(mob)){ready.forEach(Entity::discard);return;}
        if(!ledger.trigger(be.instance)){ready.forEach(Entity::discard);return;}
        if(!be.root.isEmpty())RuinAdventureService.waveSpawned(l,be,ready);
        RuinProtection.internal(()->{if(!be.root.isEmpty())l.setBlock(anchor.below(),net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),3);return l.removeBlock(anchor,false);});
        org.slf4j.LoggerFactory.getLogger(RuinEncounterService.class).info("[Xiantu ruin] Committed {} guardians for instance {} at {}",ready.size(),be.instance,anchor);
    }
    private RuinEncounterService(){}
}
