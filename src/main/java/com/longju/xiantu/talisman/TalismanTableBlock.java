package com.longju.xiantu.talisman;
import com.longju.xiantu.workstation.WorkstationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class TalismanTableBlock extends WorkstationBlock.Table implements EntityBlock {
 public TalismanTableBlock(Properties p,double[][] boxes){super(p,boxes);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(partProperty())==Part.P000?new TalismanTableEntity(p,s):null;}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){if(l.isClientSide||t!=com.longju.xiantu.registry.ModBlockEntities.TALISMAN.get()||s.getValue(partProperty())!=Part.P000)return null;return (level,p,state,be)->TalismanTableEntity.tick(level,p,state,(TalismanTableEntity)be);}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){if(p instanceof net.minecraft.server.level.ServerPlayer sp&&l.getBlockEntity(anchor(pos,s)) instanceof TalismanTableEntity be){if(p.isShiftKeyDown()){be.bind(sp);var held=p.getItemInHand(h);if(held.is(TalismanItems.SYMBOLS.get("long_return").get())){String error=TalismanReturn.bind(sp,held,be);p.displayClientMessage(net.minecraft.network.chat.Component.literal(error.isEmpty()?"千里符已绑定此制符台":error),true);}}else net.minecraftforge.network.NetworkHooks.openScreen(sp,be,b->b.writeBlockPos(be.getBlockPos()));}return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!l.isClientSide&&!s.is(next.getBlock())&&s.getValue(partProperty())==Part.P000&&l.getBlockEntity(p) instanceof TalismanTableEntity be){be.discard();Containers.dropContents(l,p,be);}super.onRemove(s,l,p,next,moving);}
}
