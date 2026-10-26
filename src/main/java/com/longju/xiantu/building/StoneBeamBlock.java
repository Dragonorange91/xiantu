package com.longju.xiantu.building;

import net.minecraft.world.phys.shapes.VoxelShape;

/** Static horizontal masonry: adjacent spans join and brackets meet the underside. */
public final class StoneBeamBlock extends BrokenPillarBlock {
    private static final VoxelShape[] BEAM = rotatedShapes(new double[][]{
            {0,0,4,16,6,12}, {0,6,3,16,8,13}, {0,8,5,16,16,11}
    });
    private static final VoxelShape[] BRACKET = rotatedShapes(new double[][]{
            {4,0,4,12,6,12}, {3,6,3,13,12,13}, {0,12,4,16,16,12}
    });

    public StoneBeamBlock(Properties properties, boolean bracket) {
        super(properties, bracket ? BRACKET : BEAM);
    }
}
