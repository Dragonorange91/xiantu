package com.longju.xiantu.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import java.util.List;

/** Deterministic candidate exclusion: never loads adjacent chunks or depends on exploration order. */
public final class RuinSiteExclusions {
    private record Site(Structure structure,long seed,long chunk) {}
    // No world references in values, and no locks held during recursive generation.
    private static final java.util.Map<net.minecraft.world.level.levelgen.RandomState,java.util.Map<Site,Boolean>> CACHE=new java.util.WeakHashMap<>();
    public static synchronized void clear(){CACHE.clear();}
    private static boolean viable(Structure.GenerationContext c,Structure structure,net.minecraft.world.level.ChunkPos candidate){
        Site key=new Site(structure,c.seed(),candidate.toLong());
        synchronized(RuinSiteExclusions.class){var map=CACHE.get(c.randomState());if(map!=null&&map.containsKey(key))return map.get(key);}
        boolean valid=structure.generate(c.registryAccess(),c.chunkGenerator(),c.chunkGenerator().getBiomeSource(),c.randomState(),c.structureTemplateManager(),c.seed(),candidate,0,c.heightAccessor(),structure.biomes()::contains).isValid();
        synchronized(RuinSiteExclusions.class){
            var map=CACHE.computeIfAbsent(c.randomState(),ignored->new java.util.LinkedHashMap<Site,Boolean>(256,.75F,true){
                @Override protected boolean removeEldestEntry(java.util.Map.Entry<Site,Boolean> eldest){return size()>8192;}
            });map.put(key,valid);
        }return valid;
    }
    public static boolean blocked(Structure.GenerationContext c,String blueprint){
        int priority=blueprint.endsWith("_grand")&&!blueprint.startsWith("fallen_")?0:blueprint.equals("fallen_cultivator_necropolis")?1:
            List.of("cloudsea_palace","skyship_wreck","floating_herb_garden","ninefold_star_observatory","mysterious_bird_nest").contains(blueprint)?2:
            List.of("ancient_cultivator_cave","earthvein_mine_palace","taixu_tomb_passage").contains(blueprint)?3:4;
        var sets=c.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        var names=List.of("xiantu:ruins_boss","xiantu:ruins_grand","xiantu:ruins_floating","xiantu:ruins_underground","xiantu:ruins_surface");
        var checks=new java.util.ArrayList<String>(List.of("minecraft:villages","xiantu:cultivation_towns"));
        checks.addAll(names.subList(0,priority));
        for(String name:checks){var set=sets.get(new ResourceLocation(name));if(set==null||!(set.placement() instanceof RandomSpreadStructurePlacement placement))continue;
            int radius=name.contains("town")?12:name.contains("villages")?10:name.endsWith("underground")?6:name.endsWith("floating")?8:12,spacing=placement.spacing();
            int minX=Math.floorDiv(c.chunkPos().x-radius,spacing),maxX=Math.floorDiv(c.chunkPos().x+radius,spacing);
            int minZ=Math.floorDiv(c.chunkPos().z-radius,spacing),maxZ=Math.floorDiv(c.chunkPos().z+radius,spacing);
            for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
                var candidate=placement.getPotentialStructureChunk(c.seed(),x*spacing,z*spacing);
                if(Math.abs(candidate.x-c.chunkPos().x)>radius||Math.abs(candidate.z-c.chunkPos().z)>radius)continue;
                for(var option:set.structures()){
                    if(viable(c,option.structure().value(),candidate))return true;
                }
            }
        }return false;
    }
    private RuinSiteExclusions(){}
}
