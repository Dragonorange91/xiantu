package com.longju.xiantu.wood;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
public class WoodenBlock extends Block {
    public WoodenBlock(Properties p){super(p);}
    @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
    @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
}
