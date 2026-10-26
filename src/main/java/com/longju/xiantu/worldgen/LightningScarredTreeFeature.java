package com.longju.xiantu.worldgen;

import com.longju.xiantu.terrain.TerrainBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class LightningScarredTreeFeature extends Feature<NoneFeatureConfiguration> {
    public LightningScarredTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos base = context.origin();
        RandomSource random = context.random();
        BlockState ground = level.getBlockState(base.below());
        if (!ground.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("thunder_rock").get())
                || !level.getBlockState(base).canBeReplaced()) {
            return false;
        }

        int height = 4 + random.nextInt(3);
        for (int y = 0; y < height; y++) {
            BlockPos trunk = base.above(y);
            if (!level.getBlockState(trunk).canBeReplaced()) return false;
        }

        for (int y = 0; y < height; y++) {
            setLog(level, base.above(y), Direction.Axis.Y);
        }

        int branches = 2 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            int startY = 1 + random.nextInt(height - 2);
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int length = 1 + random.nextInt(2);
            BlockPos branch = base.above(startY);
            for (int step = 0; step < length; step++) {
                branch = branch.relative(direction).above(step == 0 && random.nextBoolean() ? 1 : 0);
                if (!level.getBlockState(branch).canBeReplaced()) break;
                setLog(level, branch, direction.getAxis());
            }
        }
        return true;
    }

    private static void setLog(WorldGenLevel level, BlockPos pos, Direction.Axis axis) {
        BlockState state = TerrainBlockRegistry.BLOCKS_BY_ID.get("lightning_scarred_log").get()
                .defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        level.setBlock(pos, state, 2);
    }
}
