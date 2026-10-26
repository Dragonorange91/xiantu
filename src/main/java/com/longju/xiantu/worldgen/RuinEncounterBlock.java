package com.longju.xiantu.worldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.world.level.material.PushReaction;

/** Invisible non-colliding encounter marker. No item, GUI or drops; removed after its single wave. */
public final class RuinEncounterBlock extends BaseEntityBlock {
    public RuinEncounterBlock(){super(Properties.copy(Blocks.CHISELED_STONE_BRICKS).strength(-1,3600000).noLootTable().noCollission().noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.phys.shapes.Shapes.empty();}
    @Override public PushReaction getPistonPushReaction(BlockState s){return PushReaction.BLOCK;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new RuinEncounterBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ModBlockEntities.RUIN_ENCOUNTER.get(),RuinEncounterBlockEntity::tick);}
}
