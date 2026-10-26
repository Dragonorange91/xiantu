package com.longju.xiantu.instance;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

public final class InstanceCoreBlock extends Block {
    public InstanceCoreBlock(Properties p){super(p);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(p instanceof ServerPlayer sp&&hand==InteractionHand.MAIN_HAND){
            String result=InstanceService.core(sp,pos);if(!result.isEmpty())p.displayClientMessage(Component.literal(result),true);
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
