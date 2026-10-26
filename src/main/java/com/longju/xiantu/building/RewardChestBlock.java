package com.longju.xiantu.building;

import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Native chest mechanics, with a separate type so it cannot merge with vanilla chests. */
public final class RewardChestBlock extends ChestBlock {
    public RewardChestBlock(Properties properties) {
        super(properties, () -> ModBlockEntities.RUIN_REWARD_CHEST.get());
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite())
            .setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RewardChestBlockEntity(pos, state);
    }
}
