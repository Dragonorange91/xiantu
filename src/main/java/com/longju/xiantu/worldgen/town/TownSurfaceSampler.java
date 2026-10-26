package com.longju.xiantu.worldgen.town;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import java.util.HashMap;
import java.util.Map;

/** Base-noise columns only: no neighbor chunk loads or feature/structure recursion. */
public final class TownSurfaceSampler implements TownTerrainPlan.Surface {
    public record Column(int ground,int top,int waterDepth) {}
    private final ChunkGenerator generator;
    private final LevelHeightAccessor access;
    private final RandomState random;
    private final int ox,oz;
    private final Map<Long,Column> columns=new HashMap<>();
    public TownSurfaceSampler(ChunkGenerator generator,LevelHeightAccessor access,RandomState random,int ox,int oz){
        this.generator=generator;this.access=access;this.random=random;this.ox=ox;this.oz=oz;
    }
    public Column column(int x,int z){return columns.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),key->{
        var noise=generator.getBaseColumn(ox+x,oz+z,access,random);
        int top=access.getMinBuildHeight(),ground=top;boolean found=false,water=false;
        for(int y=access.getMaxBuildHeight()-1;y>=access.getMinBuildHeight();y--){
            var state=noise.getBlock(y);
            if(!found&&!state.isAir()){top=y;found=true;water=!state.getFluidState().isEmpty();}
            if(!state.isAir()&&state.getFluidState().isEmpty()){ground=y;break;}
        }
        return new Column(ground,top,water?Math.max(0,top-ground):0);
    });}
    @Override public int height(int x,int z){return column(x,z).ground();}
    @Override public int waterDepth(int x,int z,int seaLevel){return column(x,z).waterDepth();}
}
