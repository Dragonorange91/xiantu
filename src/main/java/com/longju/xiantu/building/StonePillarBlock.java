package com.longju.xiantu.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Half-block shaft; native pillar placement still selects the clicked face's axis. */
public final class StonePillarBlock extends RotatedPillarBlock {
    private static final VoxelShape X = box(0,4,4,16,12,12);
    private static final VoxelShape Y = box(4,0,4,12,16,12);
    private static final VoxelShape Z = box(4,4,0,12,12,16);

    public StonePillarBlock(Properties properties) { super(properties); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) { case X -> X; case Y -> Y; case Z -> Z; };
    }

    @Override public int getFlammability(BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos p,net.minecraft.core.Direction f){return getSoundType(s)==net.minecraft.world.level.block.SoundType.WOOD?20:super.getFlammability(s,l,p,f);}
    @Override public int getFireSpreadSpeed(BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos p,net.minecraft.core.Direction f){return getSoundType(s)==net.minecraft.world.level.block.SoundType.WOOD?5:super.getFireSpreadSpeed(s,l,p,f);}
}
