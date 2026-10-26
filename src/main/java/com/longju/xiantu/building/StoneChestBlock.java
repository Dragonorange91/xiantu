package com.longju.xiantu.building;

import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Native chest mechanics, with a separate type so it cannot merge with vanilla chests. */
public final class StoneChestBlock extends ChestBlock {
    public StoneChestBlock(Properties properties) {
        super(properties, () -> ModBlockEntities.BLACK_ROCK_CHEST.get());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneChestBlockEntity(pos, state);
    }
}
