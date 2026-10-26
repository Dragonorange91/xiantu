package com.longju.xiantu.instance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.*;

/** The thin, non-colliding portal sheet is entered by walking; no click interaction. */
public final class RealmPortalBlock extends Block implements EntityBlock {
    private final boolean anchor;
    public RealmPortalBlock(boolean anchor){super(Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable().lightLevel(s->s.getValue(BlockStateProperties.POWERED)?14:4));this.anchor=anchor;registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.POWERED);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return anchor?new RealmPortalBlockEntity(p,s):null;}
    @Override public void entityInside(BlockState s,Level level,BlockPos pos,Entity entity){
        if(s.getValue(BlockStateProperties.POWERED)&&entity instanceof ServerPlayer p&&Math.abs(p.getZ()-(pos.getZ()+.5))<.5)RealmPortal.touch(p,pos);
    }
}
