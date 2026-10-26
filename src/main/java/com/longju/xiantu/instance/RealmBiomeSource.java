package com.longju.xiantu.instance;

import com.mojang.serialization.Codec;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.biome.*;
import java.util.*;
import java.util.stream.Stream;

/** Runtime-only source owned by one session's vanilla noise delegate; serialized wrapper stays stable. */
final class RealmBiomeSource extends BiomeSource {
    private final InstanceTerrain terrain;
    private final List<Holder<Biome>> surface,caves;
    private static final String[][] SURFACE={{"forest","birch_forest","flower_forest"},{"savanna","windswept_savanna","plains"},{"swamp","forest","plains"},{"windswept_forest","meadow","windswept_hills"},{"meadow","plains","forest"},{"taiga","old_growth_pine_taiga","windswept_forest"}};
    RealmBiomeSource(RegistryAccess registry,InstanceTerrain t){
        terrain=t;var biomes=registry.registryOrThrow(Registries.BIOME);
        surface=Arrays.stream(SURFACE[t.theme()]).map(s->biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME,new ResourceLocation("xiantu:realm_"+s)))).map(h->(Holder<Biome>)h).toList();
        caves=Stream.of("lush_caves","dripstone_caves").map(s->biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME,new ResourceLocation("xiantu:realm_"+s)))).map(h->(Holder<Biome>)h).toList();
    }
    @Override protected Codec<? extends BiomeSource> codec(){throw new UnsupportedOperationException("Session delegate is not a dimension codec");}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.concat(surface.stream(),caves.stream());}
    @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){
        double v=terrain.noise(x*4,z*4,128,0x62696f6d65L);
        if(y*4<24&&terrain.noise(x*4,z*4,160,0x63617665L)>.3)return caves.get(v>0?0:1);
        return surface.get(v<-.23?0:v>.23?2:1);
    }
}
