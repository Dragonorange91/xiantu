package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.*;

/** Neighbor-driven decorative channel. All fluid behavior is native waterlogging. */
public final class StoneWaterChannelBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty NORTH=BlockStateProperties.NORTH, EAST=BlockStateProperties.EAST,
            SOUTH=BlockStateProperties.SOUTH, WEST=BlockStateProperties.WEST,
            WATERLOGGED=BlockStateProperties.WATERLOGGED;
    private static final VoxelShape[] SHAPES=makeShapes();
    public StoneWaterChannelBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(NORTH,false).setValue(EAST,false)
                .setValue(SOUTH,false).setValue(WEST,false).setValue(WATERLOGGED,false));
    }
    public static int mask(BlockState s) {
        return (s.getValue(NORTH)?1:0)|(s.getValue(EAST)?2:0)|(s.getValue(SOUTH)?4:0)|(s.getValue(WEST)?8:0);
    }
    private static BooleanProperty property(Direction d) {
        return switch(d) {case NORTH->NORTH;case EAST->EAST;case SOUTH->SOUTH;case WEST->WEST;default->throw new IllegalArgumentException();};
    }
    private static VoxelShape[] makeShapes() {
        VoxelShape[] result=new VoxelShape[16];
        for(int m=0;m<16;m++) {
            VoxelShape s=box(0,0,0,16,8,16);
            // Corner posts remain in every state, preserving edge-to-edge rims.
            for(int x:new int[]{0,14}) for(int z:new int[]{0,14}) s=Shapes.or(s,box(x,8,z,x+2,16,z+2));
            if((m&1)==0)s=Shapes.or(s,box(2,8,0,14,16,2));
            if((m&2)==0)s=Shapes.or(s,box(14,8,2,16,16,14));
            if((m&4)==0)s=Shapes.or(s,box(2,8,14,14,16,16));
            if((m&8)==0)s=Shapes.or(s,box(0,8,2,2,16,14));
            result[m]=s.optimize();
        }
        return result;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState s=defaultBlockState().setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).getType()==Fluids.WATER);
        for(Direction d:Direction.Plane.HORIZONTAL)s=s.setValue(property(d),context.getLevel().getBlockState(context.getClickedPos().relative(d)).is(this));
        return s;
    }
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other) {
        if(s.getValue(WATERLOGGED))level.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));
        return d.getAxis().isHorizontal()?s.setValue(property(d),neighbor.is(this)):s;
    }
    @Override public FluidState getFluidState(BlockState s) {return s.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override public VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c) {return SHAPES[mask(s)];}
    @Override public BlockState rotate(BlockState s,Rotation r) {
        BlockState out=s;
        for(Direction d:Direction.Plane.HORIZONTAL)out=out.setValue(property(r.rotate(d)),s.getValue(property(d)));
        return out;
    }
    @Override public BlockState mirror(BlockState s,Mirror mirror) {
        BlockState out=s;
        for(Direction d:Direction.Plane.HORIZONTAL)out=out.setValue(property(mirror.mirror(d)),s.getValue(property(d)));
        return out;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(NORTH,EAST,SOUTH,WEST,WATERLOGGED);}
}
