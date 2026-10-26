package com.longju.xiantu.building;

import net.minecraft.world.phys.shapes.VoxelShape;

/** Low broken masonry using the same rotation and water behavior as pillar fragments. */
public final class RuinRubbleBlock extends BrokenPillarBlock {
    private static final VoxelShape[] SHAPES = rotatedShapes(new double[][]{
            {1, 0, 2, 8, 3, 9}, {9, 0, 1, 14, 2, 6},
            {7, 0, 9, 15, 4, 14}, {3, 3, 4, 7, 6, 8}, {8, 4, 10, 12, 7, 13}
    });

    public RuinRubbleBlock(Properties properties) {
        super(properties, SHAPES);
    }
}
