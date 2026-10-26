package com.longju.xiantu.worldgen;

import com.longju.xiantu.terrain.NaturalResourceOreBlock;
import com.longju.xiantu.terrain.TerrainBiomes;
import com.longju.xiantu.terrain.TerrainBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class MeteorCraterFeature extends Feature<NoneFeatureConfiguration> {
    public MeteorCraterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos center = context.origin();
        RandomSource random = context.random();
        int radius = 6 + random.nextInt(4);
        int maxDepth = 2 + random.nextInt(3);
        int placedColumns = 0;
        BlockPos craterFloor = null;
        double craterFloorDistance = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                double edgeVariation = random.nextDouble() * 0.8D - 0.4D;
                if (distance > radius + edgeVariation) continue;

                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos surface = new BlockPos(x, surfaceY, z);
                if (!level.getBiome(surface).is(TerrainBiomes.METEOR_WASTELAND)) continue;
                if (!isMeteorGround(level.getBlockState(surface))) continue;

                if (distance >= radius - 1.25D + edgeVariation) {
                    BlockState rim = random.nextInt(4) == 0
                            ? TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_rock").get().defaultBlockState()
                            : TerrainBlockRegistry.BLOCKS_BY_ID.get("impact_glass_rock").get().defaultBlockState();
                    level.setBlock(surface, rim, 2);
                    placedColumns++;
                    continue;
                }

                double basin = Math.max(0.0D, 1.0D - distance / radius);
                int depth = Math.max(1, (int) Math.round(maxDepth * basin));
                boolean dug = true;
                for (int y = 0; y < depth; y++) {
                    if (!isMeteorGround(level.getBlockState(surface.below(y)))) {
                        dug = false;
                        break;
                    }
                }
                if (!dug) continue;

                for (int y = 0; y < depth; y++) {
                    level.setBlock(surface.below(y), Blocks.AIR.defaultBlockState(), 2);
                }
                BlockPos floor = surface.below(depth);
                BlockState floorState = random.nextInt(4) == 0
                        ? TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_rock").get().defaultBlockState()
                        : TerrainBlockRegistry.BLOCKS_BY_ID.get("impact_glass_rock").get().defaultBlockState();
                level.setBlock(floor, floorState, 2);
                if (distance < craterFloorDistance) {
                    craterFloor = floor;
                    craterFloorDistance = distance;
                }
                placedColumns++;
            }
        }
        if (placedColumns >= 8 && craterFloor != null) {
            placeMeteorite(level, craterFloor, random);
        }
        return placedColumns >= 8;
    }

    private static void placeMeteorite(WorldGenLevel level, BlockPos floor, RandomSource random) {
        MeteoriteShape shape = MeteoriteShape.fromRoll(random.nextInt(10));
        BlockPos anchor = floor.above();
        BlockState meteorite = TerrainBlockRegistry.BLOCKS_BY_ID.get("meteorite_stone").get().defaultBlockState();
        BlockState starDustOre = TerrainBlockRegistry.BLOCKS_BY_ID.get("ore_xingchen_sand").get().defaultBlockState()
                .setValue(NaturalResourceOreBlock.NATURAL_NODE, true);
        BlockState starOre = TerrainBlockRegistry.BLOCKS_BY_ID.get("ore_xingyun_sand").get().defaultBlockState()
                .setValue(NaturalResourceOreBlock.NATURAL_NODE, true);

        for (MeteoriteShape.Cell cell : shape.cells()) {
            BlockPos target = anchor.offset(cell.x(), cell.y(), cell.z());
            if (!level.getBiome(target).is(TerrainBiomes.METEOR_WASTELAND)
                    || !canReplaceForMeteorite(level.getBlockState(target))) {
                return;
            }
        }

        for (MeteoriteShape.Cell cell : shape.cells()) {
            level.setBlock(anchor.offset(cell.x(), cell.y(), cell.z()), meteorite, 2);
        }

        List<MeteoriteShape.Cell> interior = new ArrayList<>(shape.interiorCells());
        placeInternalOre(level, anchor, interior, starDustOre, shape.starDustCount(), random);
        placeInternalOre(level, anchor, interior, starOre, shape.starOreCount(), random);
    }

    private static void placeInternalOre(WorldGenLevel level, BlockPos anchor, List<MeteoriteShape.Cell> interior,
                                         BlockState ore, int count, RandomSource random) {
        for (int i = 0; i < count; i++) {
            MeteoriteShape.Cell cell = interior.remove(random.nextInt(interior.size()));
            level.setBlock(anchor.offset(cell.x(), cell.y(), cell.z()), ore, 2);
        }
    }

    private static boolean canReplaceForMeteorite(BlockState state) {
        return state.isAir() || isMeteorGround(state)
                || state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("impact_glass_rock").get());
    }

    private static boolean isMeteorGround(BlockState state) {
        return state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("star_dust_sand").get())
                || state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_soil").get())
                || state.is(TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_rock").get());
    }
}
