package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static, single-cell tabletop ornament. No ticker or animated state. */
public final class QunyouFigurineBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape NORTH_SOUTH=box(4,0,1.5,12,7.7,14.5);
    private static final VoxelShape EAST_WEST=box(1.5,0,4,14.5,7.7,12);
    public QunyouFigurineBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(FACING).getAxis()==Direction.Axis.Z?NORTH_SOUTH:EAST_WEST;}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return !level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).isEmpty();}
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){return side==Direction.DOWN&&!state.canSurvive(level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,side,neighbor,level,pos,other);}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new QunyouFigurineEntity(pos,state);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
}
