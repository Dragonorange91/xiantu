package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** One freestanding 2x2 cabinet; only the lower-right anchor owns its 54 slots. */
public final class JadeCabinetBlock extends PlaqueBlock implements EntityBlock {
    public static final IntegerProperty SECTION = IntegerProperty.create("part", 0, 3);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public JadeCabinetBlock(Properties properties) {
        super(properties, 2, 2);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }
    @Override public IntegerProperty partProperty() { return SECTION; }
    public BlockPos anchor(BlockPos pos, BlockState state) {
        int part = state.getValue(SECTION);
        return pos.relative(state.getValue(FACING).getClockWise(), part % 2).below(part / 2);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b); b.add(OPEN);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        for (int part = 0; part < 4; part++) {
            BlockPos p = sectionPosition(context.getClickedPos(), facing, part);
            if (!context.getLevel().isInWorldBounds(p) || !context.getLevel().getBlockState(p).canBeReplaced(context)
                    || !canSurvive(state.setValue(SECTION, part), context.getLevel(), p)) return null;
        }
        return state.setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos anchor = anchor(pos, state);
        for (int col = 0; col < 2; col++) {
            BlockPos support = sectionPosition(anchor, state.getValue(FACING), col).below();
            if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) return false;
        }
        return true;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        // Freestanding furniture must not use PlaqueBlock's wall support rule.
        if (state.getValue(BlockStateProperties.WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state;
    }
    @Override public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
        int turns = switch(s.getValue(FACING)) {case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0;};
        return JadeCabinetShapes.SHAPES[s.getValue(SECTION)+(s.getValue(OPEN)?4:0)][turns];
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SECTION) == 0 ? new JadeCabinetBlockEntity(pos, state) : null;
    }
    private JadeCabinetBlockEntity inventory(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(anchor(pos,state)) instanceof JadeCabinetBlockEntity be ? be : null;
    }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) { return inventory(level,pos,state); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(!level.isClientSide) {
            MenuProvider provider = getMenuProvider(state,level,pos);
            if(provider != null) player.openMenu(provider);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(inventory(level,pos,state));
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if(state.getValue(SECTION)==0 && level.getBlockEntity(pos) instanceof JadeCabinetBlockEntity be) be.recheckOpen();
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if(!next.is(this) && !level.isClientSide && state.getValue(SECTION)==0
                && level.getBlockEntity(pos) instanceof JadeCabinetBlockEntity be) {
            Containers.dropContents(level,pos,be);
            be.clearContent();
            level.updateNeighbourForOutputSignal(pos,this);
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
