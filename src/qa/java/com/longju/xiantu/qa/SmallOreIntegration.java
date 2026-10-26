package com.longju.xiantu.qa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.storage.LevelResource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Tests registered tiny-ore placement against actual server world blocks. */
public final class SmallOreIntegration {
    private SmallOreIntegration() {}

    public static int run(ServerPlayer player) {
        if (!player.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level = player.serverLevel();
        // A temporary, restored five-block fixture, away from existing test structures.
        BlockPos origin = player.blockPosition().offset(64, 3, 64);
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-2, 0, -2), origin.offset(2, 0, 2))) {
            saved.put(p.immutable(), level.getBlockState(p));
        }
        int assertions = 0;
        try {
            for (String id : new String[]{"tian_jing_ore", "xian_yu_ore", "dao_shi_ore"}) {
                ConfiguredFeature<?, ?> configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .get(new ResourceLocation("xiantu", id));
                if (configured == null || !(configured.config() instanceof OreConfiguration config)) {
                    throw new IllegalStateException("Missing ore configuration: " + id);
                }
                @SuppressWarnings("unchecked") Feature<OreConfiguration> feature = (Feature<OreConfiguration>) configured.feature();
                for (BlockState host : new BlockState[]{Blocks.STONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(), Blocks.AIR.defaultBlockState()}) {
                    for (int seed = 0; seed < 8; seed++) {
                        for (BlockPos p : saved.keySet()) level.setBlock(p, host, 2);
                        boolean placed = feature.place(new FeaturePlaceContext<>(Optional.empty(), level,
                                level.getChunkSource().getGenerator(), RandomSource.create(seed), origin, config));
                        long count = saved.keySet().stream().filter(p -> config.targetStates.stream()
                                .anyMatch(target -> level.getBlockState(p).equals(target.state))).count();
                        boolean expected = !host.isAir();
                        if (placed != expected || count != (expected ? config.size : 0)) {
                            throw new IllegalStateException(id + " host=" + host + " seed=" + seed + " placed=" + placed + " count=" + count);
                        }
                        assertions++;
                    }
                }
            }
            player.sendSystemMessage(Component.literal("[Small ore QA] All " + assertions + " placement assertions passed (stone/deepslate/air)"));
            return 1;
        } finally {
            saved.forEach((pos, state) -> level.setBlock(pos, state, 2));
        }
    }
}
