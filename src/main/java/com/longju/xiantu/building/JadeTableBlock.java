package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A two-cell decorative jade desk, with no GUI, inventory or ticking. */
public final class JadeTableBlock extends PlaqueBlock {
    public static final IntegerProperty SECTION = IntegerProperty.create("part", 0, 1);
    private static final double[][] BOXES = {
        {0,13,1,32,14,15},
        {1,14,3,31,16,13},
        {0,14,1,32,16,3},
        {0,14,13,32,16,15},
        {0,14,3,1,16,13},
        {31,14,3,32,16,13},
        {2,11,2,30,13,4},
        {2,11,12,30,13,14},
        {2,11,4,4,13,12},
        {28,11,4,30,13,12},
        {1,0,1,7,2,7},
        {2,2,2,6,11,6},
        {1,0,9,7,2,15},
        {2,2,10,6,11,14},
        {25,0,1,31,2,7},
        {26,2,2,30,11,6},
        {25,0,9,31,2,15},
        {26,2,10,30,11,14}
    };
    private static final VoxelShape[][] SHAPES = createShapes();

    public JadeTableBlock(Properties properties) { super(properties, 2, 1); }
    @Override public IntegerProperty partProperty() { return SECTION; }

    private static VoxelShape[][] createShapes() {
        VoxelShape[][] result = new VoxelShape[2][4];
        for (int part=0;part<2;part++) for (int turn=0;turn<4;turn++) {
            int x=(1-part%2)*16, y=part/2*16;
            VoxelShape shape=Shapes.empty();
            for (double[] b:BOXES) {
                double[] c={Math.max(b[0],x)-x,Math.max(b[1],y)-y,b[2],Math.min(b[3],x+16)-x,Math.min(b[4],y+16)-y,b[5]};
                if(c[0]>=c[3] || c[1]>=c[4]) continue;
                for(int t=0;t<turn;t++) c=new double[]{16-c[5],c[1],c[0],16-c[2],c[4],c[3]};
                shape=Shapes.or(shape,net.minecraft.world.level.block.Block.box(c[0],c[1],c[2],c[3],c[4],c[5]));
            }
            result[part][turn]=shape.optimize();
        }
        return result;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing=context.getHorizontalDirection().getOpposite();
        BlockState state=defaultBlockState().setValue(FACING,facing);
        for(int part=0;part<2;part++) {
            BlockPos p=sectionPosition(context.getClickedPos(),facing,part);
            if(!context.getLevel().isInWorldBounds(p) || !context.getLevel().getBlockState(p).canBeReplaced(context)
                    || !canSurvive(state.setValue(SECTION,part),context.getLevel(),p)) return null;
        }
        return state.setValue(BlockStateProperties.WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).getType()==Fluids.WATER);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int part=state.getValue(SECTION);
        BlockPos anchor=pos.relative(state.getValue(FACING).getClockWise(),part%2).below(part/2);
        for(int col=0;col<2;col++) {
            BlockPos support=sectionPosition(anchor,state.getValue(FACING),col).below();
            if(!level.getBlockState(support).isFaceSturdy(level,support,Direction.UP)) return false;
        }
        return true;
    }

    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,
            net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        if(direction==Direction.DOWN && !state.canSurvive(level,pos))
            return state.getValue(BlockStateProperties.WATERLOGGED) ? net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }

    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        int turn=switch(state.getValue(FACING)) {case NORTH->0;case EAST->1;case SOUTH->2;case WEST->3;default->throw new IllegalStateException();};
        return SHAPES[state.getValue(SECTION)][turn];
    }
}
