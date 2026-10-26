package com.longju.xiantu.entity;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.worldgen.town.CultivationTownStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Bed protection includes offline players; town boundaries come from native structure starts. */
@Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID)
public final class BeastSpawnProtection extends SavedData {
    private final Map<UUID,CompoundTag> beds=new HashMap<>();
    private record TownCache(long until,List<BoundingBox> boxes){}
    private static final Map<ServerLevel,Map<Long,TownCache>> TOWNS=new WeakHashMap<>();
    private static BeastSpawnProtection get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(BeastSpawnProtection::load,BeastSpawnProtection::new,"xiantu_beast_beds");}
    public static BeastSpawnProtection load(CompoundTag tag){var result=new BeastSpawnProtection();for(var v:tag.getList("beds",Tag.TAG_COMPOUND)){var t=(CompoundTag)v;if(t.hasUUID("player"))result.beds.put(t.getUUID("player"),t.copy());}return result;}
    @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();beds.values().forEach(t->list.add(t.copy()));tag.put("beds",list);return tag;}
    private static void record(ServerPlayer p,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,BlockPos pos){
        var data=get(p.serverLevel());
        if(pos==null)data.beds.remove(p.getUUID());else{var t=new CompoundTag();t.putUUID("player",p.getUUID());t.putString("dim",dim.location().toString());t.putLong("pos",pos.asLong());data.beds.put(p.getUUID(),t);}data.setDirty();
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void bed(PlayerSetSpawnEvent e){if(e.getEntity() instanceof ServerPlayer p)record(p,e.getSpawnLevel(),e.getNewSpawn());}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)record(p,p.getRespawnDimension(),p.getRespawnPosition());}
    public static boolean nearBed(ServerLevel level,BlockPos pos){
        for(var t:get(level).beds.values())if(t.getString("dim").equals(level.dimension().location().toString())){
            var bed=BlockPos.of(t.getLong("pos"));long dx=(long)bed.getX()-pos.getX(),dz=(long)bed.getZ()-pos.getZ();if(dx*dx+dz*dz<=24*24)return true;
        }return false;
    }
    public static boolean inTown(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos))return true;
        var chunk=new ChunkPos(pos);var cache=TOWNS.computeIfAbsent(level,l->new HashMap<>());
        var entry=cache.get(chunk.toLong());long now=level.getGameTime();
        if(entry==null||entry.until<now){
            if(cache.size()>1024)cache.clear();
            var loaded=level.getChunkSource().getChunkNow(chunk.x,chunk.z);
            if(loaded==null)return true;
            List<BoundingBox> boxes=new ArrayList<>();
            loaded.getAllStarts().forEach((structure,start)->{if(structure instanceof CultivationTownStructure&&start.isValid())boxes.add(start.getBoundingBox());});
            for(var ref:loaded.getAllReferences().entrySet())if(ref.getKey() instanceof CultivationTownStructure){
                for(long key:ref.getValue()){
                    var origin=new ChunkPos(key);var startChunk=level.getChunkSource().getChunkNow(origin.x,origin.z);
                    // Unknown referenced starts are protected until loaded normally. Never request a chunk.
                    if(startChunk==null)return true;
                    var start=startChunk.getStartForStructure(ref.getKey());if(start!=null&&start.isValid())boxes.add(start.getBoundingBox());
                }
            }
            entry=new TownCache(now+200,boxes);cache.put(chunk.toLong(),entry);
        }
        for(var b:entry.boxes)if(pos.getX()>=b.minX()&&pos.getX()<=b.maxX()&&pos.getZ()>=b.minZ()&&pos.getZ()<=b.maxZ())return true;
        return false;
    }
    private BeastSpawnProtection(){}
}
