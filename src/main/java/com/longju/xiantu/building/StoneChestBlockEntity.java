package com.longju.xiantu.building;

import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla 27 slots, persistence, lid events, hopper capability and loot behavior. */
public final class StoneChestBlockEntity extends ChestBlockEntity {
    public StoneChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLACK_ROCK_CHEST.get(), pos, state);
    }
    @Override protected Component getDefaultName() {
        return Component.translatable("container.xiantu.black_rock_chest");
    }
}
