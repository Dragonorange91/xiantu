package com.longju.xiantu.wood;
import com.longju.xiantu.material.MaterialQuality;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;
public final class SpiritLogBlock extends RotatedPillarBlock implements EntityBlock {
    public final SpiritWoodType type;public final boolean stripped;
    public SpiritLogBlock(Properties p,SpiritWoodType t,boolean stripped){super(p);type=t;this.stripped=stripped;registerDefaultState(defaultBlockState().setValue(SpiritPlanksBlock.QUALITY,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(SpiritPlanksBlock.QUALITY);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return super.getStateForPlacement(c).setValue(SpiritPlanksBlock.QUALITY,MaterialQuality.get(c.getItemInHand()));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SpiritWoodBlockEntity(p,s);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity e,ItemStack stack){
        if(l.getBlockEntity(p) instanceof SpiritWoodBlockEntity be){be.treeId=null;be.setChanged();}
    }
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder p){
        var entity=p.getOptionalParameter(LootContextParams.THIS_ENTITY);
        ItemStack tool=p.getOptionalParameter(LootContextParams.TOOL);
        if(!(entity instanceof Player player)||tool==null||!SpiritWoodRules.canHarvest(player,type,tool))return List.of();
        List<ItemStack> drops=new ArrayList<>();drops.add(MaterialQuality.set(new ItemStack(this),s.getValue(SpiritPlanksBlock.QUALITY)));
        if(p.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SpiritWoodBlockEntity be && be.treeId!=null)
            drops.add(new ItemStack(SpiritWoodRegistry.block(type,"sapling")));
        return drops;
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        ItemStack axe=player.getItemInHand(hand);
        if(stripped||!axe.canPerformAction(net.minecraftforge.common.ToolActions.AXE_STRIP))return InteractionResult.PASS;
        if(!SpiritWoodRules.canHarvest(player,type,axe)){
            if(!l.isClientSide)SpiritWoodRules.harvestHint(player,type);return InteractionResult.sidedSuccess(l.isClientSide);
        }
        if(!l.isClientSide){
            UUID id=l.getBlockEntity(p) instanceof SpiritWoodBlockEntity be?be.treeId:null;
            l.setBlock(p,SpiritWoodRegistry.block(type,"stripped_log").defaultBlockState().setValue(AXIS,s.getValue(AXIS)).setValue(SpiritPlanksBlock.QUALITY,s.getValue(SpiritPlanksBlock.QUALITY)),3);
            if(l.getBlockEntity(p) instanceof SpiritWoodBlockEntity be){be.treeId=id;be.setChanged();}
            if(!player.getAbilities().instabuild)axe.hurtAndBreak(1,player,e->e.broadcastBreakEvent(hand));
            l.playSound(null,p,net.minecraft.sounds.SoundEvents.AXE_STRIP,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction face){return 5;}
    @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction face){return 5;}
}
