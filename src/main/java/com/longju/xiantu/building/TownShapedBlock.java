package com.longju.xiantu.building;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;

/** Four cached voxel shapes. North-facing roofs climb south into the building. */
public final class TownShapedBlock extends BrokenPillarBlock {
    private final boolean diagonal;
    public TownShapedBlock(Properties properties, boolean diagonal, double[][] boxes) {
        super(properties, rotatedShapes(boxes));
        this.diagonal = diagonal;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (diagonal && mirror != Mirror.NONE)
            return state.setValue(FACING, mirror.mirror(state.getValue(FACING)).getClockWise());
        return super.mirror(state, mirror);
    }
}
