package com.longju.xiantu.terrain;

import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;

public final class NaturalFallingResourceOreBlock extends FallingBlock {
    public NaturalFallingResourceOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NaturalResourceOreBlock.NATURAL_NODE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,
            net.minecraft.world.level.block.state.BlockState> builder) {
        builder.add(NaturalResourceOreBlock.NATURAL_NODE);
    }
}
