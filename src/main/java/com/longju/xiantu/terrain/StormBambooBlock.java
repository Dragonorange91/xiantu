package com.longju.xiantu.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class StormBambooBlock extends DoublePlantBlock {
    private final String soilId;

    public StormBambooBlock(Properties properties, String soilId) {
        super(properties);
        this.soilId = soilId;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get(soilId).get());
    }
}
