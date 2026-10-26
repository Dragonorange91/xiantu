package com.longju.xiantu.medicine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class MedicineSoilBlock extends Block {
    private final int tier;

    public MedicineSoilBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(player.getItemInHand(hand).getItem() instanceof HoeItem) || !level.getBlockState(pos.above()).isAir()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, MedicineRegistry.FARMLAND_BLOCKS.get(tier).get().defaultBlockState(), 3);
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
