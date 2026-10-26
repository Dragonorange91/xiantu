package com.longju.xiantu.worldgen;

import com.longju.xiantu.terrain.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

/** Inner random-patch placement has no topFeature; inspect the actual biome directly. */
public final class HabitatPlantFeature extends Feature<SimpleBlockConfiguration> {
    public HabitatPlantFeature(){super(SimpleBlockConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> context){
        var level=context.level();var pos=context.origin();
        var state=context.config().toPlace().getState(context.random(),pos);
        var biome=level.getBiome(pos);
        boolean thunder=state.is(TerrainPlantRegistry.PLANTS.get("thunder_sedge").get());
        boolean meteor=state.is(TerrainPlantRegistry.PLANTS.get("meteor_ash_grass").get())
                ||state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("impact_glass_rock").get());
        if(!biome.is(thunder?TerrainBiomes.THUNDER_WASTES:meteor?TerrainBiomes.METEOR_WASTELAND:TerrainBiomes.SPIRIT_MEDICINE_PLAIN)
                ||!level.ensureCanWrite(pos)||!level.getBlockState(pos).isAir()||!state.canSurvive(level,pos))return false;
        if(state.getBlock() instanceof DoublePlantBlock){
            if(!level.ensureCanWrite(pos.above())||!level.getBlockState(pos.above()).isAir())return false;
            DoublePlantBlock.placeAt(level,state,pos,2);
        }else level.setBlock(pos,state,2);
        return true;
    }
}
