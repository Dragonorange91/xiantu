package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static stone pedestal / torch-lit shrine lantern, without a block entity or fuel tick. */
public final class StoneLampBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape STAND = Shapes.or(
            box(2, 0, 2, 14, 3, 14), box(4, 3, 4, 12, 14, 12), box(2, 14, 2, 14, 16, 14));
    // The lamp keeps its low built-in base; only the separate pedestal is a full block tall.
    private static final VoxelShape LAMP = Shapes.or(
            box(2, 0, 2, 14, 3, 14), box(4, 3, 4, 12, 7, 12), box(3, 7, 3, 13, 9, 13),
            box(6, 9, 6, 10, 13, 10),
            box(4, 9, 4, 5, 13, 5), box(11, 9, 4, 12, 13, 5),
            box(4, 9, 11, 5, 13, 12), box(11, 9, 11, 12, 13, 12),
            box(1, 13, 1, 15, 14, 15), box(3, 14, 3, 13, 15, 13), box(5, 15, 5, 11, 16, 11));
    private final boolean lamp;

    public StoneLampBlock(Properties properties, boolean lamp) {
        super(properties);
        this.lamp = lamp;
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return lamp ? LAMP : STAND;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
}
