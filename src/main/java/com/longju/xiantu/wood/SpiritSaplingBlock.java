package com.longju.xiantu.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkHooks;
import com.longju.xiantu.item.SpiritStoneItem;

public final class SpiritSaplingBlock extends BushBlock implements EntityBlock,BonemealableBlock {
    public final SpiritWoodType type;
    public SpiritSaplingBlock(Properties p,SpiritWoodType type){super(p);this.type=type;}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return Block.box(2,0,2,14,14,14);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SpiritWoodBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){
        return !l.isClientSide&&t==SpiritWoodRegistry.ENTITY.get()?(level,pos,state,be)->SpiritWoodBlockEntity.tick(level,pos,state,(SpiritWoodBlockEntity)be):null;
    }
    @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s,boolean client){
        return type.rank()<=3&&l.getBlockEntity(p) instanceof SpiritWoodBlockEntity be&&be.boneMealUses<5&&be.remainingUnits()>0;
    }
    @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s){return SpiritTreeGrowth.environment(l,p)&&SpiritTreeGrowth.space(l,p,type);}
    @Override public void performBonemeal(net.minecraft.server.level.ServerLevel l,RandomSource r,BlockPos p,BlockState s){
        if(l.getBlockEntity(p) instanceof SpiritWoodBlockEntity be&&isValidBonemealTarget(l,p,s,false)){
            be.boneMealUses++;be.growthUnits=Math.min(be.totalUnits(),be.growthUnits+be.totalUnits()/10);be.update();
            if(be.remainingUnits()==0)SpiritTreeGrowth.grow(l,p,type);
        }
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND||(!p.isShiftKeyDown()&&!SpiritStoneItem.isSpiritStone(p.getItemInHand(hand))))return InteractionResult.PASS;
        if(!l.isClientSide&&p instanceof ServerPlayer server&&l.getBlockEntity(pos) instanceof SpiritWoodBlockEntity be){
            NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,player)->new SaplingGrowthMenu(id,inv,pos,be,p.isShiftKeyDown()),Component.literal(type.treeName+" · 灵石催熟")),buf->{buf.writeBlockPos(pos);buf.writeBoolean(p.isShiftKeyDown());buf.writeInt(type.ordinal());});
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
