package com.longju.xiantu.trade;

import com.longju.xiantu.worldgen.town.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Bounded main-thread structure-start search. No full town/chunk population or off-thread world access. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class TownLocator {
    private static final Map<UUID,Search> JOBS=new LinkedHashMap<>();
    private record Candidate(ChunkPos chunk,double lowerBound) {}
    private static final class Search {
        BlockPos origin,best;double distance=Double.MAX_VALUE;int index;
        List<Candidate> candidates=new ArrayList<>();
    }
    private TownLocator(){}
    public static void cancel(UUID player){JOBS.remove(player);}
    public static void request(ServerPlayer player){
        if(JOBS.containsKey(player.getUUID())||TownJourney.arrived(player))return;
        var data=TownJourney.state(player);
        if(data.contains("Target")||data.getString("SearchState").equals("unavailable"))return;
        ServerLevel level=player.server.overworld();
        var registry=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var holder=registry.getHolder(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,new ResourceLocation("xiantu","cultivation_town")));
        if(holder.isEmpty()||!player.server.getWorldData().worldGenOptions().generateStructures()){data.putString("SearchState","unavailable");return;}
        Search job=new Search();job.origin=BlockPos.of(data.getLong("Origin"));
        Set<Long> seen=new HashSet<>();
        for(var placement:level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder.get()))if(placement instanceof RandomSpreadStructurePlacement spread){
            int rx=Math.floorDiv(job.origin.getX()>>4,spread.spacing()),rz=Math.floorDiv(job.origin.getZ()>>4,spread.spacing());
            // 32 placement regions (~17km for the default spacing). Sort by a strict lower bound
            // including the town's nine alternative offsets, then stop only when none can beat best.
            for(int x=rx-32;x<=rx+32;x++)for(int z=rz-32;z<=rz+32;z++){
                var cp=spread.getPotentialStructureChunk(level.getSeed(),x*spread.spacing(),z*spread.spacing());
                BlockPos center=new BlockPos(cp.getMinBlockX()-TownLayout.ORIGIN_OFFSET+TownLayout.CENTER_X,64,cp.getMinBlockZ()-TownLayout.ORIGIN_OFFSET+TownLayout.CENTER_Z);
                if(!level.getWorldBorder().isWithinBounds(center)||!seen.add(cp.toLong()))continue;
                double dx=Math.max(0,Math.abs(center.getX()-job.origin.getX())-32),dz=Math.max(0,Math.abs(center.getZ()-job.origin.getZ())-32);
                job.candidates.add(new Candidate(cp,dx*dx+dz*dz));
            }
        }
        job.candidates.sort(Comparator.comparingDouble(Candidate::lowerBound));
        if(job.candidates.isEmpty()){data.putString("SearchState","unavailable");return;}
        JOBS.put(player.getUUID(),job);data.putString("SearchState","searching");
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){JOBS.clear();}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||JOBS.isEmpty())return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null||server.getTickCount()%5!=0)return;
        UUID id=JOBS.keySet().iterator().next();Search s=JOBS.remove(id);ServerPlayer player=server.getPlayerList().getPlayer(id);
        if(player==null||TownJourney.arrived(player))return;
        if(s.index>=s.candidates.size()||s.candidates.get(s.index).lowerBound()>s.distance){
            var data=TownJourney.state(player);
            if(s.best!=null){data.putLong("Target",s.best.asLong());data.putString("SearchState","found");}
            else data.putString("SearchState","unavailable");
            TownJourney.syncStone(player);player.inventoryMenu.broadcastChanges();return;
        }
        Candidate candidate=s.candidates.get(s.index++);ServerLevel level=server.overworld();
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("xiantu","cultivation_town"));
        var chunk=level.getChunk(candidate.chunk.x,candidate.chunk.z,ChunkStatus.STRUCTURE_STARTS);
        var start=chunk.getStartForStructure(structure);
        if(start!=null&&start.isValid())for(var piece:start.getPieces())if(piece instanceof TownTerrainPiece tile&&tile.covers(TownLayout.CENTER_X,TownLayout.CENTER_Z)){
            BlockPos actual=new BlockPos(tile.originX()+TownLayout.CENTER_X,tile.plannedHeight(TownLayout.CENTER_X,TownLayout.CENTER_Z)+2,tile.originZ()+TownLayout.CENTER_Z);
            double dx=actual.getX()-s.origin.getX(),dz=actual.getZ()-s.origin.getZ(),d=dx*dx+dz*dz;
            if(d<s.distance){s.distance=d;s.best=actual;}break;
        }
        JOBS.put(id,s);
    }
}
