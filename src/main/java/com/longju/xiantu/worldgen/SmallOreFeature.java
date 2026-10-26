package com.longju.xiantu.worldgen;

import com.longju.xiantu.config.XiantuConfig;
import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Places tiny ore deposits on block coordinates, without sub-block ellipsoids. */
public final class SmallOreFeature extends Feature<OreConfiguration> {
    public SmallOreFeature() {
        super(OreConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        if (context.config().size < 1 || context.config().size > 8) return false;
        double chance = generationMultiplier(context.config());
        if (chance <= 0.0D || (chance < 1.0D && context.random().nextDouble() >= chance)) return false;
        BlockPos origin = context.origin();
        if (!replace(context, origin)) return false;
        int placed = 1;
        int attempts = 0;
        while (placed < context.config().size && attempts++ < context.config().size * 16) {
            // Keep tiny deposits on the sampled Y and grow by a short horizontal walk.
            int direction = context.random().nextInt(4);
            int dx = direction == 0 ? 1 : direction == 1 ? -1 : 0;
            int dz = direction == 2 ? 1 : direction == 3 ? -1 : 0;
            BlockPos neighbor = origin.offset(context.random().nextInt(3) - 1 + dx,
                    0, context.random().nextInt(3) - 1 + dz);
            if (replace(context, neighbor)) placed++;
        }
        return placed > 0;
    }

    private static double generationMultiplier(OreConfiguration config) {
        for (var target : config.targetStates) {
            if (target.state.is(ModBlocks.FAN_LING_SHI_ORE.get())) return XiantuConfig.SPIRIT_ORE_FAN_LING_SHI_CHANCE.get();
            if (target.state.is(ModBlocks.LING_JING_ORE.get())) return XiantuConfig.SPIRIT_ORE_LING_JING_CHANCE.get();
            if (target.state.is(ModBlocks.LING_YU_ORE.get())) return XiantuConfig.SPIRIT_ORE_LING_YU_CHANCE.get();
            if (target.state.is(ModBlocks.XUAN_JING_ORE.get())) return XiantuConfig.SPIRIT_ORE_XUAN_JING_CHANCE.get();
            if (target.state.is(ModBlocks.DI_SUI_ORE.get())) return XiantuConfig.SPIRIT_ORE_DI_SUI_CHANCE.get();
            if (target.state.is(ModBlocks.TIAN_JING_ORE.get())) return XiantuConfig.SPIRIT_ORE_TIAN_JING_CHANCE.get();
            if (target.state.is(ModBlocks.XIAN_YU_ORE.get())) return XiantuConfig.SPIRIT_ORE_XIAN_YU_CHANCE.get();
            if (target.state.is(ModBlocks.DAO_SHI_ORE.get())) return XiantuConfig.SPIRIT_ORE_DAO_SHI_CHANCE.get();
            var metal = com.longju.xiantu.mining.MetalMaterialRegistry.byOre(target.state.getBlock());
            if (metal != null) return XiantuConfig.METAL_ORE_CHANCES.get(metal.id()).get();
        }
        return 1.0D;
    }

    private static boolean replace(FeaturePlaceContext<OreConfiguration> context, BlockPos pos) {
        WorldGenLevel level = context.level();
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false;
        var current = level.getBlockState(pos);
        for (var target : context.config().targetStates) {
            if (target.target.test(current, context.random())) {
                return level.setBlock(pos, target.state, 2);
            }
        }
        return false;
    }
}
