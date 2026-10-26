package com.longju.xiantu.formation;

import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** The preview, bill of materials and server placement all use this exact layout. */
public final class FormationPlan {
    public static Map<BlockPos,BlockState> blocks(int tier){
        if(tier<1||tier>3)throw new IllegalArgumentException("formation tier");
        var result=new LinkedHashMap<BlockPos,BlockState>();
        result.put(BlockPos.ZERO,FormationStructure.coreBlock(tier).defaultBlockState());
        result.put(BlockPos.ZERO.above(),ModBlocks.MEDITATION_CUSHION.get().defaultBlockState());
        for(int t=1;t<=tier;t++){
            for(boolean outer:new boolean[]{true,false})for(var tile:FormationTrace.ringStage(t,outer)){
                var state=FormationStructure.partBlock(outer?FormationPartBlock.Part.BASE:FormationPartBlock.Part.RUNE,t).defaultBlockState().setValue(FormationPartBlock.TRACE,tile.trace());
                result.put(new BlockPos(tile.x(),0,tile.z()),state);
            }
            for(var direction:Direction.Plane.HORIZONTAL){
                var foot=BlockPos.ZERO.relative(direction,FormationLayout.outerRadius(t));
                for(int h=1;h<=t;h++)result.put(foot.above(h),FormationStructure.partBlock(FormationPartBlock.Part.PILLAR,t).defaultBlockState().setValue(FormationPartBlock.SECTION,t==1?0:h==1?1:h==t?3:2));
            }
        }
        return Collections.unmodifiableMap(result);
    }
    public static Map<Item,Integer> materials(int tier){
        var result=new LinkedHashMap<Item,Integer>();
        blocks(tier).values().forEach(s->result.merge(s.getBlock().asItem(),1,Integer::sum));
        return result;
    }
    private FormationPlan(){}
}
