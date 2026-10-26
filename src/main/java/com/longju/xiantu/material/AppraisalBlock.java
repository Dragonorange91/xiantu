package com.longju.xiantu.material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.network.NetworkHooks;
public final class AppraisalBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE=Shapes.or(Block.box(1,0,1,15,3,15),Block.box(3,3,3,13,11,13),Block.box(0,11,0,16,15,16),Block.box(1,15,1,15,15.5,15));
    public AppraisalBlock(Properties p){super(p);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new AppraisalBlockEntity(p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!l.isClientSide&&player instanceof net.minecraft.server.level.ServerPlayer sp&&l.getBlockEntity(p) instanceof AppraisalBlockEntity be)NetworkHooks.openScreen(sp,be,p);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving){
        if(!old.is(next.getBlock())&&l.getBlockEntity(p) instanceof AppraisalBlockEntity be){Containers.dropContents(l,p,be);l.updateNeighbourForOutputSignal(p,this);}
        super.onRemove(old,l,p,next,moving);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState s){return true;}
    @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(l.getBlockEntity(p));}
}
