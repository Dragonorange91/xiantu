package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Neighbor-driven masonry joins with cached static models/shapes, like native connecting blocks. */
public final class ConnectedStoneBeamBlock extends BrokenPillarBlock {
    public enum Join implements StringRepresentable {
        STRAIGHT_X(10), STRAIGHT_Z(5), CORNER_NE(3), CORNER_SE(6), CORNER_SW(12), CORNER_NW(9),
        TEE_NES(7), TEE_ESW(14), TEE_SWN(13), TEE_WNE(11), CROSS(15);
        final int mask;
        Join(int mask) { this.mask = mask; }
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
        static Join forMask(int mask) {
            for (Join join : values()) if (join.mask==mask) return join;
            throw new IllegalArgumentException("Invalid beam join: "+mask);
        }
    }
    public static final EnumProperty<Join> JOIN = EnumProperty.create("join", Join.class);
    private static final VoxelShape[] SHAPES = makeShapes();

    public ConnectedStoneBeamBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(JOIN,Join.STRAIGHT_X));
    }

    private static int bit(Direction direction) {
        return switch (direction) { case NORTH -> 1; case EAST -> 2; case SOUTH -> 4; case WEST -> 8; default -> 0; };
    }
    private static VoxelShape[] makeShapes() {
        VoxelShape[] shapes = new VoxelShape[Join.values().length];
        for (Join join : Join.values()) {
            VoxelShape shape = Shapes.empty();
            for (int[] layer : new int[][]{{4,12,0,6},{3,13,6,8},{5,11,8,16}}) {
                int lo=layer[0], hi=layer[1], y=layer[2], top=layer[3];
                shape=Shapes.or(shape,Block.box(lo,y,lo,hi,top,hi));
                if ((join.mask&1)!=0) shape=Shapes.or(shape,Block.box(lo,y,0,hi,top,lo));
                if ((join.mask&2)!=0) shape=Shapes.or(shape,Block.box(hi,y,lo,16,top,hi));
                if ((join.mask&4)!=0) shape=Shapes.or(shape,Block.box(lo,y,hi,hi,top,16));
                if ((join.mask&8)!=0) shape=Shapes.or(shape,Block.box(0,y,lo,lo,top,hi));
            }
            shapes[join.ordinal()]=shape.optimize();
        }
        return shapes;
    }
    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        int mask=0;
        for (Direction side : Direction.Plane.HORIZONTAL)
            if (level.getBlockState(pos.relative(side)).getBlock() instanceof ConnectedStoneBeamBlock) mask|=bit(side);
        // Isolated/end beams retain a full straight span, aligned with any sole neighbor.
        if (mask==0) mask=state.getValue(FACING).getAxis()==Direction.Axis.Z ? 10 : 5;
        else if (Integer.bitCount(mask)==1) mask=(mask&5)!=0 ? 5 : 10;
        return state.setValue(JOIN,Join.forMask(mask));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(super.getStateForPlacement(context),context.getLevel(),context.getClickedPos());
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connect(super.updateShape(state,side,neighbor,level,pos,neighborPos),level,pos);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(JOIN).ordinal()];
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        int mask=0;
        for (Direction side:Direction.Plane.HORIZONTAL)
            if ((state.getValue(JOIN).mask&bit(side))!=0) mask|=bit(rotation.rotate(side));
        return state.setValue(FACING,rotation.rotate(state.getValue(FACING))).setValue(JOIN,Join.forMask(mask));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        int mask=0;
        for (Direction side:Direction.Plane.HORIZONTAL)
            if ((state.getValue(JOIN).mask&bit(side))!=0) mask|=bit(mirror.mirror(side));
        return state.setValue(FACING,mirror.mirror(state.getValue(FACING))).setValue(JOIN,Join.forMask(mask));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(JOIN);
    }
}
