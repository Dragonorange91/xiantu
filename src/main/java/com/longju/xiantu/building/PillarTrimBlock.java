package com.longju.xiantu.building;

import net.minecraft.world.phys.shapes.VoxelShape;

/** Tapered collars meet the half-block shaft, with a three-quarter-block foot/cap. */
public final class PillarTrimBlock extends BrokenPillarBlock {
    private static final VoxelShape[] BASE = rotatedShapes(new double[][]{
            {2,0,2,14,4,14}, {3,4,3,13,7,13}, {4,7,4,12,16,12}
    });
    private static final VoxelShape[] CAPITAL = rotatedShapes(new double[][]{
            {4,0,4,12,9,12}, {3,9,3,13,12,13}, {2,12,2,14,16,14}
    });

    public PillarTrimBlock(Properties properties, boolean capital) {
        super(properties, capital ? CAPITAL : BASE);
    }
}
