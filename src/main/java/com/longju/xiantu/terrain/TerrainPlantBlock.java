package com.longju.xiantu.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class TerrainPlantBlock extends BushBlock {
    private final String soilId;
    public TerrainPlantBlock(Properties properties, String soilId) {
        super(properties);
        this.soilId = soilId;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.FARMLAND) || state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get(soilId).get());
    }

}
