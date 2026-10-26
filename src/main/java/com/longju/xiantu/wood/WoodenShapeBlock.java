package com.longju.xiantu.wood;
import com.longju.xiantu.building.BrokenPillarBlock;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
public final class WoodenShapeBlock extends BrokenPillarBlock {
    public WoodenShapeBlock(Properties p,double[][] boxes){super(p,rotatedShapes(boxes));}
    @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
    @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
}
