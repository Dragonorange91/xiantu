package com.longju.xiantu.workstation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Recheck assemblies across chunk borders when a saved section returns, without an idle ticker. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class WorkstationLifecycle {
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var chunk=event.getChunk();
        level.getServer().execute(()->{
            var sections=chunk.getSections();
            for(int section=0;section<sections.length;section++){
                var data=sections[section];
                if(!data.maybeHas(state->state.getBlock() instanceof WorkstationBlock))continue;
                int bottom=chunk.getMinBuildHeight()+section*16;
                for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                    var state=data.getBlockState(x,y,z);
                    if(state.getBlock() instanceof WorkstationBlock block){
                        var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,bottom+y,chunk.getPos().getMinBlockZ()+z);
                        if(level.hasChunkAt(pos))level.scheduleTick(pos,block,1);
                    }
                }
            }
        });
    }
    private WorkstationLifecycle(){}
}
