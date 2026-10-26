package com.longju.xiantu.worldgen;
import com.longju.xiantu.terrain.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Geology follows the first real land surface, never starts again on underground cave floors. */
public final class TerrainRockLayerFeature extends Feature<NoneFeatureConfiguration> {
    public TerrainRockLayerFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var level=c.level();int ox=c.origin().getX()&~15,oz=c.origin().getZ()&~15;boolean placed=false;
        for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++){
            int x=ox+dx,z=oz+dz,y=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;
            var top=new BlockPos(x,y,z);var biome=level.getBiome(top);String rock;
            if(biome.is(TerrainBiomes.THUNDER_WASTES))rock="thunder_rock";
            else if(biome.is(TerrainBiomes.METEOR_WASTELAND))rock="meteor_rock";
            else if(biome.is(TerrainBiomes.SPIRIT_MEDICINE_PLAIN))rock="valley_streamstone";
            else continue;
            if(!level.getFluidState(top.above()).isEmpty()||!level.getFluidState(top).isEmpty())continue;
            for(int depth=0;depth<=17;depth++){
                var pos=top.below(depth);var state=level.getBlockState(pos);
                if(state.isAir()||!state.getFluidState().isEmpty())break;
                if(state.is(Blocks.STONE)||state.is(Blocks.GRANITE)||state.is(Blocks.DIORITE)
                        ||state.is(Blocks.ANDESITE)||state.is(Blocks.TUFF)||state.is(Blocks.DEEPSLATE)){
                    if(level.ensureCanWrite(pos)&&level.getBlockEntity(pos)==null)
                        placed|=level.setBlock(pos,TerrainBlockRegistry.BLOCKS_BY_ID.get(rock).get().defaultBlockState(),2);
                }
            }
        }
        return placed;
    }
}
