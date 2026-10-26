package com.longju.xiantu.mining;

import com.longju.xiantu.item.SpiritStoneTier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class SpiritOreBlock extends Block {
    private final SpiritStoneTier tier;

    public SpiritOreBlock(BlockBehaviour.Properties properties, SpiritStoneTier tier) {
        super(properties.requiresCorrectToolForDrops());
        this.tier = tier;
    }

    public SpiritStoneTier tier() {
        return tier;
    }
}
