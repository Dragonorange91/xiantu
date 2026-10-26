package com.longju.xiantu.worldgen;

import com.longju.xiantu.terrain.TerrainBiomes;
import com.longju.xiantu.terrain.TerrainBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class MeteorFissureFeature extends Feature<NoneFeatureConfiguration> {
    public MeteorFissureFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int segments = 1 + random.nextInt(2);
        int placed = 0;

        for (int segment = 0; segment < segments; segment++) {
            int x = origin.getX() + (segment == 0 ? 0 : random.nextInt(5) - 2);
            int z = origin.getZ() + (segment == 0 ? 0 : random.nextInt(5) - 2);
            int direction = random.nextInt(8);
            int length = segment == 0 ? 5 + random.nextInt(6) : 3 + random.nextInt(4);
            int drift = 0;

            for (int step = 0; step < length; step++) {
                placed += placeCrackBlock(level, x, z);
                if (random.nextInt(4) == 0) drift = random.nextInt(3) - 1;
                int adjustedDirection = (direction + drift + 8) % 8;
                x += DX[adjustedDirection];
                z += DZ[adjustedDirection];
            }
        }
        return placed > 0;
    }

    private static int placeCrackBlock(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        BlockPos surface = new BlockPos(x, y, z);
        if (!level.getBiome(surface).is(TerrainBiomes.METEOR_WASTELAND)) return 0;
        BlockState ground = level.getBlockState(surface);
        if (!isMeteorGround(ground)) return 0;
        return level.setBlock(surface,
                TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_glow_fissure").get().defaultBlockState(), 2) ? 1 : 0;
    }

    private static boolean isMeteorGround(BlockState state) {
        return state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("star_dust_sand").get())
                || state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_soil").get())
                || state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_rock").get());
    }

    private static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DZ = {0, 1, 1, 1, 0, -1, -1, -1};
}
