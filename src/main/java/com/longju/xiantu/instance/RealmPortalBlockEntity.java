package com.longju.xiantu.instance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public final class RealmPortalBlockEntity extends BlockEntity {
    public long clientOpenedAt=-1;
    public RealmPortalBlockEntity(BlockPos p,BlockState s){super(InstanceRegistry.PORTAL_ENTITY.get(),p,s);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition.offset(-5,-1,-4),worldPosition.offset(6,8,5));}
}
