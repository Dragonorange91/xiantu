package com.longju.xiantu.refining;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Passive occupancy marker. Only the anchor has inventory, ownership and a ticker. */
public final class WorkstationPartBlockEntity extends BlockEntity {
    public WorkstationPartBlockEntity(BlockPos p,BlockState s){super(com.longju.xiantu.registry.ModBlockEntities.REFINING_PART.get(),p,s);}
}
