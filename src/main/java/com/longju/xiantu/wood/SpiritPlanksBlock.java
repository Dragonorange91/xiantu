package com.longju.xiantu.wood;
import com.longju.xiantu.material.MaterialQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;
public final class SpiritPlanksBlock extends Block {
    public static final IntegerProperty QUALITY=IntegerProperty.create("material_quality",0,5);
    public final SpiritWoodType type;
    public SpiritPlanksBlock(Properties p,SpiritWoodType type){super(p);this.type=type;registerDefaultState(defaultBlockState().setValue(QUALITY,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(QUALITY);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(QUALITY,MaterialQuality.get(c.getItemInHand()));}
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder p){return List.of(MaterialQuality.set(new ItemStack(this),s.getValue(QUALITY)));}
    @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,net.minecraft.core.Direction face){return 20;}
    @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,net.minecraft.core.Direction face){return 5;}
}
