package com.longju.xiantu.formation;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FormationPartBlock extends Block {
    public enum Part { BASE, RUNE, PILLAR }
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 3);
    public static final IntegerProperty TRACE = IntegerProperty.create("trace", 0, 28);
    private final Part part;
    private final int tier;

    public FormationPartBlock(BlockBehaviour.Properties properties, Part part) {
        this(properties, part, 1);
    }

    public FormationPartBlock(BlockBehaviour.Properties properties, Part part, int tier) {
        super(properties);
        this.part = part;
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SECTION, 0).setValue(TRACE,0));
    }

    public Part part() { return part; }
    public int tier() { return tier; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SECTION, TRACE);
    }
    private int section(BlockGetter level, BlockPos pos) {
        boolean below = level.getBlockState(pos.below()).is(this);
        boolean above = level.getBlockState(pos.above()).is(this);
        return below ? (above ? 2 : 3) : (above ? 1 : 0);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(SECTION, part == Part.PILLAR ? section(context.getLevel(), context.getClickedPos()) : 0);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return part == Part.PILLAR && side.getAxis().isVertical()
                ? state.setValue(SECTION, section(level, pos)) : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return part == Part.PILLAR ? FormationVisualShapes.pillar(tier, state.getValue(SECTION))
                : super.getShape(state, level, pos, context);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        int turns=switch(rotation) {case NONE->0;case CLOCKWISE_90->1;case CLOCKWISE_180->2;case COUNTERCLOCKWISE_90->3;};
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(TRACE,FormationTrace.transform(state.getValue(TRACE),turns,false,false));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING,mirror.mirror(state.getValue(FACING)))
                .setValue(TRACE,FormationTrace.transform(state.getValue(TRACE),0,mirror==Mirror.FRONT_BACK,mirror==Mirror.LEFT_RIGHT));
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state,level,pos,old,moving);
        if (!level.isClientSide && part == Part.PILLAR) {
            int actual = section(level, pos);
            if (state.getValue(SECTION) != actual) level.setBlock(pos, state.setValue(SECTION, actual), Block.UPDATE_ALL);
        }
        if (!old.is(state.getBlock())) FormationStructure.invalidateNear(level,pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) FormationStructure.invalidateNear(level,pos);
        super.onRemove(state,level,pos,next,moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var found = FormationStructure.findCore(level, pos);
        if (found.isPresent() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(found.get()) instanceof FormationCoreBlockEntity core) {
            NetworkHooks.openScreen(serverPlayer, core, found.get());
        }
        return InteractionResult.CONSUME;
    }
}
