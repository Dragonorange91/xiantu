package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import net.minecraft.commands.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.util.*;

/** Bounded candidate survey; uses base noise and structure registries, never edits blocks. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class TownWorldgenAudit {
    private static Job job;
    private static final class Job {
        final CommandSourceStack source;final ServerLevel level;final Structure town;final RandomSpreadStructurePlacement placement;
        final List<ChunkPos> cells=new ArrayList<>();final List<Map<String,Object>> accepted=new ArrayList<>();
        final Map<String,Integer> reasons=new TreeMap<>();final List<Object> relaxed=new ArrayList<>();final List<Object> terrainSamples=new ArrayList<>();int next,excluded;final long started=System.currentTimeMillis();
        Job(CommandSourceStack source){
            this.source=source;level=source.getLevel();
            town=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("xiantu","cultivation_town"));
            var set=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(new ResourceLocation("xiantu","cultivation_towns"));
            placement=(RandomSpreadStructurePlacement)set.placement();
            int cx=Math.floorDiv((int)source.getPosition().x,16*34),cz=Math.floorDiv((int)source.getPosition().z,16*34);
            for(int radius=0;radius<=8;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(Math.max(Math.abs(x),Math.abs(z))==radius)cells.add(new ChunkPos((cx+x)*34,(cz+z)*34));
        }
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantutownsurvey").requires(s->s.hasPermission(2)).executes(c->{
            if(job!=null)return 0;
            var level=c.getSource().getLevel();
            if(!level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||level.getChunkSource().getGenerator() instanceof FlatLevelSource){
                c.getSource().sendFailure(Component.literal("Use a new normal-terrain Xiantu-QA save"));return 0;
            }
            job=new Job(c.getSource());c.getSource().sendSuccess(()->Component.literal("Town survey started: 289 candidate regions, one per tick, no block edits"),false);return 1;
        }));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||job==null)return;Job j=job;
        try {
            var c=j.cells.get(j.next++);var source=j.level.getChunkSource();
            var candidate=j.placement.getPotentialStructureChunk(j.level.getSeed(),c.x,c.z);
            if(j.placement.isStructureChunk(source.getGeneratorState(),candidate.x,candidate.z)){
                var surface=new com.longju.xiantu.worldgen.town.TownSurfaceSampler(source.getGenerator(),j.level,source.randomState(),candidate.getMinBlockX()-com.longju.xiantu.worldgen.town.TownLayout.ORIGIN_OFFSET,candidate.getMinBlockZ()-com.longju.xiantu.worldgen.town.TownLayout.ORIGIN_OFFSET);
                var original=com.longju.xiantu.worldgen.town.TownTerrainPlan.create(surface,source.getGenerator().getSeaLevel(),j.level.getMaxBuildHeight(),reason->j.reasons.merge(reason,1,Integer::sum));
                if(original.isPresent())j.reasons.merge("terrain_pass",1,Integer::sum);
                var relaxed=com.longju.xiantu.worldgen.town.TownTerrainPlan.create(surface,source.getGenerator().getSeaLevel(),j.level.getMaxBuildHeight(),16,6,10,5,reason->j.reasons.merge("baseline_"+reason,1,Integer::sum));
                if(relaxed.isPresent())j.relaxed.add(List.of(candidate.x,candidate.z));
                var heights=new ArrayList<List<Integer>>();var water=new ArrayList<List<Integer>>();
                for(int x=-8;x<=152;x+=8){var row=new ArrayList<Integer>();var wetRow=new ArrayList<Integer>();for(int z=-8;z<=144;z+=8){row.add(surface.height(x,z));wetRow.add(surface.column(x,z).waterDepth());}heights.add(row);water.add(wetRow);}
                int centerY=surface.height(72,66);
                var biome=source.getGenerator().getBiomeSource().getNoiseBiome((candidate.getMinBlockX()+8)>>2,centerY>>2,(candidate.getMinBlockZ()+2)>>2,source.randomState().sampler());
                var terrainReport=new LinkedHashMap<String,Object>();terrainReport.put("chunk",List.of(candidate.x,candidate.z));terrainReport.put("heights",heights);terrainReport.put("waterDepth",water);terrainReport.put("biome",biome.unwrapKey().map(k->k.location().toString()).orElse("unknown"));terrainReport.put("biomeEligible",j.town.biomes().contains(biome));j.terrainSamples.add(terrainReport);
                var start=j.town.generate(j.level.registryAccess(),source.getGenerator(),source.getGenerator().getBiomeSource(),source.randomState(),j.level.getStructureManager(),j.level.getSeed(),candidate,0,j.level,holder->j.town.biomes().contains(holder));
                if(start.isValid()){
                    var box=start.getBoundingBox();var report=new LinkedHashMap<String,Object>();report.put("chunk",List.of(candidate.x,candidate.z));
                    var tile=(com.longju.xiantu.worldgen.town.TownTerrainPiece)start.getPieces().get(0);
                    report.put("origin",List.of(tile.originX(),tile.originZ()));
                    report.put("bounds",List.of(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()));j.accepted.add(report);
                }
            }else j.excluded++;
            if(j.next<j.cells.size())return;
            var report=new LinkedHashMap<String,Object>();report.put("seed",j.level.getSeed());report.put("candidateRegions",j.next);report.put("excludedByVillage",j.excluded);report.put("accepted",j.accepted);report.put("terrainSamples",j.terrainSamples);report.put("rejections",j.reasons);report.put("strictBaselineTerrainCandidates",j.relaxed);report.put("elapsedMs",System.currentTimeMillis()-j.started);
            report.put("vanillaVillageRegistered",j.level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(new ResourceLocation("minecraft","village_plains")));
            var path=FMLPaths.GAMEDIR.get().resolve("xiantu-qa/town-worldgen-survey.json");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));
            j.source.sendSuccess(()->Component.literal("Town survey complete: "+j.accepted.size()+" suitable / "+j.next+", village exclusions "+j.excluded),false);job=null;
        }catch(Exception error){job=null;com.mojang.logging.LogUtils.getLogger().error("Town survey failed",error);j.source.sendFailure(Component.literal("Town survey failed: "+error));}
    }
}
