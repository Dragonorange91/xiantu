package com.longju.xiantu.terrain;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class NaturalResourceOreBlock extends Block {
    public static final BooleanProperty NATURAL_NODE = BooleanProperty.create("natural_node");

    public NaturalResourceOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NATURAL_NODE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, net.minecraft.world.level.block.state.BlockState> builder) {
        builder.add(NATURAL_NODE);
    }
}
