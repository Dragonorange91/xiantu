package com.longju.xiantu.building;

import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class QunyouFigurineEntity extends BlockEntity {
    public QunyouFigurineEntity(BlockPos pos,BlockState state){super(ModBlockEntities.QUNYOU_FIGURINE.get(),pos,state);}
}
