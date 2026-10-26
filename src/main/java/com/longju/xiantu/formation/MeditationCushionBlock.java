package com.longju.xiantu.formation;

import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class MeditationCushionBlock extends Block {
    public MeditationCushionBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return FormationVisualShapes.CUSHION;
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos.below()) instanceof FormationCoreBlockEntity core) core.invalidateStructure();
        super.onRemove(state,level,pos,next,moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player.getVehicle() instanceof FormationSeatEntity
                && player.getItemInHand(hand).getItem() instanceof com.longju.xiantu.gongfa.GongfaItem) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos corePos = pos.below();
        if (level.getBlockEntity(corePos) instanceof FormationCoreBlockEntity core
                && player.getVehicle() instanceof FormationSeatEntity seat && seat.cushionPos().equals(pos)) {
            if (player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer, core, corePos);
            return InteractionResult.CONSUME;
        }
        FormationCoreBlockEntity core = level.getBlockEntity(corePos) instanceof FormationCoreBlockEntity c ? c : null;
        if (core != null && core.canMeditate() && core.unsafe(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.xiantu.formation_unavailable"), true);
            if (player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer, core, corePos);
            return InteractionResult.CONSUME;
        }
        if (!level.getEntitiesOfClass(FormationSeatEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(0.6D)).isEmpty()) return InteractionResult.CONSUME;
        if (level.getBlockState(pos.above()).isCollisionShapeFullBlock(level, pos.above())) return InteractionResult.CONSUME;
        FormationSeatEntity seat = ModEntities.FORMATION_SEAT.get().create(level);
        if (seat == null) return InteractionResult.FAIL;
        seat.setPos(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D);
        seat.setCushionPos(pos);
        level.addFreshEntity(seat);
        player.startRiding(seat, true);
        if (core != null) core.beginMeditation(player);
        return InteractionResult.CONSUME;
    }
}
