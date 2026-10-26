package com.longju.xiantu.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import java.util.UUID;

/** Loaded-tick growth only; no wall clock, no offline catch-up. */
public final class SpiritWoodBlockEntity extends BlockEntity {
    public UUID treeId;
    public long growthUnits;
    public int boneMealUses;
    public SpiritWoodBlockEntity(BlockPos pos,BlockState state){super(SpiritWoodRegistry.ENTITY.get(),pos,state);}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        if(treeId!=null)tag.putUUID("tree_id",treeId);
        tag.putLong("growth_units",growthUnits);tag.putInt("bone_meal_uses",boneMealUses);
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);treeId=tag.hasUUID("tree_id")?tag.getUUID("tree_id"):null;
        SpiritWoodType t=type();
        growthUnits=Math.max(0,Math.min(tag.getLong("growth_units"),t.growthTicks()*t.energyCost()));
        boneMealUses=Math.max(0,Math.min(5,tag.getInt("bone_meal_uses")));
    }
    public SpiritWoodType type(){return SpiritWoodRegistry.type(getBlockState().getBlock());}
    public long totalUnits(){return type().growthTicks()*type().energyCost();}
    public long remainingUnits(){return Math.max(0,totalUnits()-growthUnits);}
    public void update(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,SpiritWoodBlockEntity be){
        if(level.isClientSide||!(state.getBlock() instanceof SpiritSaplingBlock))return;
        SpiritWoodType t=be.type();
        if(!SpiritTreeGrowth.environment(level,pos))return;
        if(be.growthUnits<be.totalUnits()){be.growthUnits=Math.min(be.totalUnits(),be.growthUnits+t.energyCost());be.setChanged();}
        if(level.getGameTime()%20==0){
            if(be.growthUnits>=be.totalUnits())SpiritTreeGrowth.grow(level,pos,t);
            else be.update();
        }
    }
}
