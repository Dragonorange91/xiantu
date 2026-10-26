package com.longju.xiantu.talisman;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class CinnabarOreBlock extends com.longju.xiantu.terrain.NaturalResourceOreBlock {
 public CinnabarOreBlock(){super(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3,3).requiresCorrectToolForDrops());}
 @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
  var drops=new ArrayList<ItemStack>(super.getDrops(state,builder));var player=builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
  if(state.getValue(NATURAL_NODE)&&player instanceof net.minecraft.server.level.ServerPlayer&&builder.getLevel().random.nextFloat()<.05f)drops.add(new ItemStack(TalismanItems.SPACE_SAND.get()));return drops;
 }
}
