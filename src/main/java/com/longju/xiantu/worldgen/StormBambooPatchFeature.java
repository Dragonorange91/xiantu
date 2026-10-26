package com.longju.xiantu.worldgen;

import com.longju.xiantu.terrain.StormBambooBlock;
import com.longju.xiantu.terrain.TerrainPlantRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class StormBambooPatchFeature extends Feature<NoneFeatureConfiguration> {
    private static final int ATTEMPTS = 12;

    public StormBambooPatchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        var block = (StormBambooBlock) TerrainPlantRegistry.PLANTS.get("storm_bamboo").get();
        BlockState lower = block.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        int placed = 0;

        for (int i = 0; i < ATTEMPTS; i++) {
            int x=context.origin().getX()+random.nextInt(5)-random.nextInt(5);
            int z=context.origin().getZ()+random.nextInt(5)-random.nextInt(5);
            BlockPos pos=new BlockPos(x,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
            if (!level.ensureCanWrite(pos.above())
                    || !level.getBiome(pos).is(com.longju.xiantu.terrain.TerrainBiomes.THUNDER_WASTES)
                    || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                    || !lower.canSurvive(level, pos)) {
                continue;
            }
            DoublePlantBlock.placeAt(level, lower, pos, 2);
            placed++;
        }
        return placed > 0;
    }
}
