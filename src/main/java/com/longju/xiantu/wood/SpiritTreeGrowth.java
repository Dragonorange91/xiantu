package com.longju.xiantu.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Preflight complete geometry before touching terrain, so growth never overwrites a build. */
public final class SpiritTreeGrowth {
    private SpiritTreeGrowth(){}
    public static boolean environment(net.minecraft.world.level.Level l,BlockPos p){
        return l.canSeeSky(p)&&l.getMaxLocalRawBrightness(p)>=9&&l.getBlockState(p.below()).is(BlockTags.DIRT);
    }
    public static Map<BlockPos,Boolean> layout(BlockPos base,SpiritWoodType t){
        Map<BlockPos,Boolean> blocks=new LinkedHashMap<>();
        long seed=SpiritTreeGeometry.positionSeed(base.getX(),base.getY(),base.getZ());
        SpiritTreeGeometry.layout(t.rank(),seed).forEach((p,wood)->blocks.put(base.offset(p.x(),p.y(),p.z()),wood));
        return blocks;
    }
    public static boolean space(LevelAccessor l,BlockPos base,SpiritWoodType t){
        return space(l,base,layout(base,t));
    }
    private static boolean space(LevelAccessor l,BlockPos base,Map<BlockPos,Boolean> geometry){
        for(var entry:geometry.entrySet()){
            BlockPos p=entry.getKey();
            if(p.getY()<l.getMinBuildHeight()||p.getY()>=l.getMaxBuildHeight()||!l.hasChunkAt(p))return false;
            BlockState state=l.getBlockState(p);
            if(p.equals(base)&&state.getBlock() instanceof SpiritSaplingBlock)continue;
            if(!replaceable(state,!entry.getValue()))return false;
        }
        return true;
    }
    static boolean replaceable(BlockState state,boolean foliage){
        boolean plant=state.is(Blocks.GRASS)||state.is(Blocks.TALL_GRASS)||state.is(Blocks.FERN)||state.is(Blocks.LARGE_FERN);
        return canReplace(state.isAir()||plant,state.is(BlockTags.LEAVES),foliage);
    }
    static boolean canReplace(boolean airOrPlant,boolean leaf,boolean foliage){return airOrPlant||foliage&&leaf;}
    public static boolean grow(LevelAccessor l,BlockPos base,SpiritWoodType t){
        var geometry=layout(base,t);
        if(!space(l,base,geometry))return false;
        BlockState log=SpiritWoodRegistry.block(t,"log").defaultBlockState();
        BlockState leaf=SpiritWoodRegistry.block(t,"leaves").defaultBlockState();
        for(var entry:geometry.entrySet())if(entry.getValue()){
            var p=entry.getKey();var axis=net.minecraft.core.Direction.Axis.Y;
            if(!p.equals(base)&&!Boolean.TRUE.equals(geometry.get(p.above()))&&!Boolean.TRUE.equals(geometry.get(p.below()))){
                if(Boolean.TRUE.equals(geometry.get(p.east()))||Boolean.TRUE.equals(geometry.get(p.west())))axis=net.minecraft.core.Direction.Axis.X;
                else if(Boolean.TRUE.equals(geometry.get(p.north()))||Boolean.TRUE.equals(geometry.get(p.south())))axis=net.minecraft.core.Direction.Axis.Z;
            }
            l.setBlock(p,log.setValue(RotatedPillarBlock.AXIS,axis),3);
        }
        // Follow real leaf adjacency. All natural leaves remain decayable after felling.
        Map<SpiritTreeGeometry.Cell,Boolean> cells=new LinkedHashMap<>();
        geometry.forEach((p,wood)->cells.put(new SpiritTreeGeometry.Cell(p.getX(),p.getY(),p.getZ()),wood));
        var distances=SpiritTreeGeometry.leafDistances(cells);
        for(var entry:geometry.entrySet())if(!entry.getValue()){
            var p=entry.getKey();int distance=distances.get(new SpiritTreeGeometry.Cell(p.getX(),p.getY(),p.getZ()));
            l.setBlock(p,leaf.setValue(LeavesBlock.DISTANCE,distance),3);
        }
        if(l.getBlockEntity(base) instanceof SpiritWoodBlockEntity be){be.treeId=UUID.randomUUID();be.update();}
        return true;
    }
}
