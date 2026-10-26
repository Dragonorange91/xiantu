package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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

/** A static ruin fragment: chipped base and uneven broken crown. */
public class BrokenPillarBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final double[][] BOXES = {{2,0,2,12,2,14},{12,0,5,14,2,13},{3,2,3,13,4,13},{4,4,4,12,8,12},{4,8,4,7,13,12},{7,8,8,12,11,12},{7,8,4,10,10,8},{4,13,7,6,16,11},{10,0,0,14,2,2},{0,0,8,2,3,12},{3.2,2.65,2.7,12.7,3.25,3.05},{3.2,2.65,12.85,12.7,3.25,13.2},{3.75,4.1,3.7,4.25,7.8,4.2},{3.75,4.1,11.8,4.25,7.8,12.3},{7.75,4.1,3.7,8.25,7.8,4.2},{7.75,4.1,11.8,8.25,7.8,12.3},{11.75,4.1,3.7,12.25,7.8,4.2},{11.75,4.1,11.8,12.25,7.8,12.3},{4,12.6,4,6.5,13.25,7},{7,10,6,8.5,11.2,8},{9,10,8,11,11.75,10},{6,13,8,6.8,14.2,10}};
    private static final VoxelShape[] SHAPES = rotatedShapes(BOXES);
    private final VoxelShape[] shapes;

    protected static VoxelShape[] rotatedShapes(double[][] boxes) {
        VoxelShape[] result = new VoxelShape[4];
        for (int turn = 0; turn < 4; turn++) {
            VoxelShape shape = Shapes.empty();
            for (double[] source : boxes) {
                double[] b = source.clone();
                for (int i = 0; i < turn; i++)
                    b = new double[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
                shape = Shapes.or(shape, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
            }
            result[turn] = shape.optimize();
        }
        return result;
    }

    public BrokenPillarBlock(Properties properties) {
        this(properties, SHAPES);
    }

    protected BrokenPillarBlock(Properties properties, VoxelShape[] shapes) {
        super(properties);
        this.shapes = shapes;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[switch (state.getValue(FACING)) {
            case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3;
            default -> throw new IllegalStateException("Horizontal facing required");
        }];
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        builder.add(FACING, WATERLOGGED);
    }

    @Override public int getFlammability(BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos p,net.minecraft.core.Direction f){return getSoundType(s)==net.minecraft.world.level.block.SoundType.WOOD?20:super.getFlammability(s,l,p,f);}
    @Override public int getFireSpreadSpeed(BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos p,net.minecraft.core.Direction f){return getSoundType(s)==net.minecraft.world.level.block.SoundType.WOOD?5:super.getFireSpreadSpeed(s,l,p,f);}
}
