package com.longju.xiantu.worldgen.town;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class CultivationTownStructure extends Structure {
    public static final Codec<CultivationTownStructure> CODEC=simpleCodec(CultivationTownStructure::new);
    public static final ResourceLocation TEMPLATE=new ResourceLocation("xiantu","town/cultivation_town");
    public CultivationTownStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return TownWorldgen.TOWN.get();}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int baseX=context.chunkPos().getMinBlockX()-TownLayout.ORIGIN_OFFSET,baseZ=context.chunkPos().getMinBlockZ()-TownLayout.ORIGIN_OFFSET;
        var generator=context.chunkGenerator();
        var shared=new TownSurfaceSampler(generator,context.heightAccessor(),context.randomState(),baseX,baseZ);
        int[][] offsets={{0,0},{32,0},{32,32},{0,32},{-32,32},{-32,0},{-32,-32},{0,-32},{32,-32}};
        int order=Math.floorMod(context.chunkPos().x*31+context.chunkPos().z,8);
        // All alternate sites remain inside the native eight-chunk structure-reference radius.
        // Overlapping lattices share cached columns; never generate chunks to search for land.
        for(int attempt=0;attempt<offsets.length;attempt++){
        int[] offset=offsets[attempt==0?0:1+(attempt-1+order)%8];int dx=offset[0],dz=offset[1];
        int ox=baseX+dx,oz=baseZ+dz;
        TownTerrainPlan.Surface surface=new TownTerrainPlan.Surface(){
            public int height(int x,int z){return shared.height(x+dx,z+dz);}
            public int waterDepth(int x,int z,int sea){return shared.waterDepth(x+dx,z+dz,sea);}
        };
        int cx=ox+TownLayout.CENTER_X,cz=oz+TownLayout.CENTER_Z;
        int cy=surface.height(TownLayout.CENTER_X,TownLayout.CENTER_Z);
        if(!biomes().contains(generator.getBiomeSource().getNoiseBiome(cx>>2,cy>>2,cz>>2,context.randomState().sampler())))continue;
        var plan=TownTerrainPlan.create(surface,
            generator.getSeaLevel(),context.heightAccessor().getMaxBuildHeight());
        if(plan.isEmpty())continue;
        var terrain=plan.get();
        return Optional.of(new GenerationStub(new BlockPos(ox+TownLayout.CENTER_X,terrain.height(TownLayout.CENTER_X,TownLayout.CENTER_Z),oz+TownLayout.CENTER_Z),builder->{
            for(int x=-8;x<TownTerrainPlan.WIDTH+8;x+=16)for(int z=-8;z<TownTerrainPlan.DEPTH+8;z+=16)
                builder.addPiece(new TownTerrainPiece(ox,oz,x,z,Math.min(16,TownTerrainPlan.WIDTH+8-x),Math.min(16,TownTerrainPlan.DEPTH+8-z),terrain));
        }));
        }
        return Optional.empty();
    }
}
