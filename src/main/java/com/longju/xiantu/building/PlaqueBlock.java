package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.material.PushReaction;

/** A non-ticking decorative signboard backing, attached to a solid wall. */
public class PlaqueBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final int WIDTH = 4;
    public static final int PART_COUNT = WIDTH * 2;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, PART_COUNT - 1);
    private final int width;
    private final int height;
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape NORTH = Block.box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 3, 16, 16);
    private static final VoxelShape WEST = Block.box(13, 0, 0, 16, 16, 16);

    public PlaqueBlock(Properties properties) {
        this(properties, WIDTH, 2);
    }

    protected PlaqueBlock(Properties properties, int width, int height) {
        super(properties);
        this.width = width;
        this.height = height;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(partProperty(), 0));
    }

    public IntegerProperty partProperty() { return PART; }
    public int width() { return width; }
    public int height() { return height; }
    public int partCount() { return width * height; }
    public BlockPos sectionPosition(BlockPos anchor, Direction facing, int part) {
        return anchor.relative(facing.getCounterClockWise(), part % width).above(part / width);
    }

    public static final class Medium extends PlaqueBlock {
        private static final IntegerProperty SECTION = IntegerProperty.create("part", 0, 11);
        public Medium(Properties properties) { super(properties, 6, 2); }
        @Override public IntegerProperty partProperty() { return SECTION; }
    }

    public static final class Large extends PlaqueBlock {
        private static final IntegerProperty SECTION = IntegerProperty.create("part", 0, 23);
        public Large(Properties properties) { super(properties, 8, 3); }
        @Override public IntegerProperty partProperty() { return SECTION; }
    }

    public static final class Vertical extends PlaqueBlock {
        public Vertical(Properties properties) { super(properties, 2, 4); }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite())
                        .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
                boolean clear = true;
                for (int part = 0; part < partCount(); part++) {
                    BlockPos target = sectionPosition(context.getClickedPos(), state.getValue(FACING), part);
                    if (!context.getLevel().isInWorldBounds(target)
                            || !context.getLevel().getBlockState(target).canBeReplaced(context)
                            || !state.canSurvive(context.getLevel(), target)) { clear = false; break; }
                }
                if (clear) return state;
            }
        }
        return null;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    public static BlockPos partPosition(BlockPos anchor, Direction facing, int part) {
        return anchor.relative(facing.getCounterClockWise(), part % WIDTH).above(part / WIDTH);
    }

    private BlockPos anchorPosition(BlockPos pos, BlockState state) {
        int part = state.getValue(partProperty());
        return pos.relative(state.getValue(FACING).getClockWise(), part % width).below(part / width);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        for (int part = 1; part < partCount(); part++) {
            BlockPos target = sectionPosition(pos, state.getValue(FACING), part);
            level.setBlock(target, state.setValue(partProperty(), part)
                    .setValue(WATERLOGGED, level.getFluidState(target).getType() == Fluids.WATER), 3);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(partProperty()) != 0) {
            BlockPos anchor = anchorPosition(pos, state);
            if (level.getBlockState(anchor).is(this)) level.destroyBlock(anchor, !player.isCreative(), player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this) && !level.isClientSide) {
            BlockPos anchor = anchorPosition(pos, state);
            if (state.getValue(partProperty()) != 0) {
                if (level.getBlockState(anchor).is(this)) level.destroyBlock(anchor, true);
            } else {
                for (int part = 1; part < partCount(); part++) {
                    BlockPos target = sectionPosition(anchor, state.getValue(FACING), part);
                    BlockState other = level.getBlockState(target);
                    if (other.is(this) && other.getValue(partProperty()) == part && other.getValue(FACING) == state.getValue(FACING))
                        level.setBlock(target, other.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> throw new IllegalStateException("Plaque facing must be horizontal");
        };
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        int part = state.getValue(partProperty());
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
                .setValue(partProperty(), part / width * width + width - 1 - part % width);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, partProperty());
    }
}
