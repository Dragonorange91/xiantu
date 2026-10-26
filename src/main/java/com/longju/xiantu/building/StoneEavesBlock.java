package com.longju.xiantu.building;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low-element tiled eaves; north-facing corner has north and west raised lips. */
public final class StoneEavesBlock extends BrokenPillarBlock {
    private static final VoxelShape[] STRAIGHT = rotatedShapes(new double[][]{
            {0,0,0,16,4,16}, {0,4,0,16,6,8}, {0,6,0,16,8,4}
    });
    private static final VoxelShape[] CORNER = rotatedShapes(new double[][]{
            {0,0,0,16,4,16}, {0,4,0,16,6,8}, {0,4,8,8,6,16},
            {0,6,0,16,8,4}, {0,6,4,4,8,16}
    });
    private final boolean corner;

    public StoneEavesBlock(Properties properties, boolean corner) {
        super(properties, corner ? CORNER : STRAIGHT);
        this.corner = corner;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        // A corner faces diagonally; treating it like a single straight edge swaps corners.
        if (corner && mirror != Mirror.NONE)
            return state.setValue(FACING, mirror.mirror(state.getValue(FACING)).getClockWise());
        return super.mirror(state, mirror);
    }
}
