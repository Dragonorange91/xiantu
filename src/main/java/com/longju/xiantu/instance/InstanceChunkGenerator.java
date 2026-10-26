package com.longju.xiantu.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import java.util.*;
import java.util.concurrent.*;

/** Stable dimension codec, with an immutable session-specific vanilla Overworld generation delegate. */
public final class InstanceChunkGenerator extends ChunkGenerator {
    public static final Codec<InstanceChunkGenerator> CODEC=RecordCodecBuilder.create(i->i.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(g->g.biomeSource),
        Codec.BOOL.fieldOf("cave").forGetter(g->g.cave),Codec.INT.fieldOf("salt").forGetter(g->g.salt)
    ).apply(i,InstanceChunkGenerator::new));
    private final boolean cave;private final int salt;
    private record Session(InstanceTerrain terrain,RealmLayout layout,NoiseBasedChunkGenerator generator,RandomState random){}
    private volatile Session session;
    public InstanceChunkGenerator(BiomeSource source,boolean cave,int salt){super(source);this.cave=cave;this.salt=salt;}
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    public synchronized void configure(InstanceTerrain t,RealmLayout layout,RegistryAccess registries){
        if(cave)throw new IllegalStateException("Void caves have no terrain session");
        if(session!=null){if(!session.terrain.equals(t))throw new IllegalStateException("Live terrain cannot be replaced");return;}
        var settings=registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var random=RandomState.create(settings.value(),registries.lookupOrThrow(Registries.NOISE),t.seed());
        session=new Session(t,layout,new NoiseBasedChunkGenerator(new RealmBiomeSource(registries,t),settings),random);
    }
    @Override public CompletableFuture<ChunkAccess> createBiomes(Executor executor,RandomState r,Blender b,StructureManager m,ChunkAccess c){var s=session;return s==null?super.createBiomes(executor,r,b,m,c):s.generator.createBiomes(executor,s.random,b,m,c);}
    @Override public BiomeSource getBiomeSource(){var s=session;return s==null?super.getBiomeSource():s.generator.getBiomeSource();}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState r,StructureManager m,ChunkAccess c){
        var s=session;if(cave||s==null){Heightmap.primeHeightmaps(c,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG,Heightmap.Types.WORLD_SURFACE_WG));return CompletableFuture.completedFuture(c);}
        if(!inside(c,s.terrain))return CompletableFuture.completedFuture(c);
        return s.generator.fillFromNoise(executor,blender,s.random,m,c);
    }
    private static boolean inside(ChunkAccess c,InstanceTerrain t){var p=c.getPos();return t.generates(p.getMiddleBlockX(),p.getMiddleBlockZ());}
    @Override public void applyCarvers(WorldGenRegion l,long seed,RandomState r,BiomeManager b,StructureManager m,ChunkAccess c,GenerationStep.Carving step){var s=session;if(s!=null&&inside(c,s.terrain))s.generator.applyCarvers(l,s.terrain.seed(),s.random,b,m,c,step);}
    @Override public void buildSurface(WorldGenRegion l,StructureManager m,RandomState r,ChunkAccess c){
        var s=session;if(s==null||!inside(c,s.terrain))return;
        s.generator.buildSurface(l,m,s.random,c);
        // Adapt only around actual foundations and routes, preserving deep native caves/aquifers.
        var cp=c.getPos();var p=new BlockPos.MutableBlockPos();
        for(int x=cp.getMinBlockX();x<=cp.getMaxBlockX();x++)for(int z=cp.getMinBlockZ();z<=cp.getMaxBlockZ();z++){
            int original=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x&15,z&15),target=surface(s,x,z,original);
            if(target==original&&!s.terrain.sanctuary(x,z))continue;
            int top=Math.max(c.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x&15,z&15),target);
            for(int y=Math.max(-63,Math.min(original,target)-4);y<=top;y++)c.setBlockState(p.set(x,y,z),y>target?Blocks.AIR.defaultBlockState():y==target?Blocks.GRASS_BLOCK.defaultBlockState():y>target-4?Blocks.DIRT.defaultBlockState():Blocks.STONE.defaultBlockState(),false);
        }
        Heightmap.primeHeightmaps(c,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG,Heightmap.Types.WORLD_SURFACE_WG));
    }
    private int surface(Session s,int x,int z,int original){
        if(!s.terrain.natural())return s.layout.surface(x,z,original);
        double weight=1-Math.max(0,Math.min(1,(Math.hypot(x,z)-17)/20));return (int)Math.round(original+(64-original)*weight*weight*(3-2*weight));
    }
    @Override public void applyBiomeDecoration(WorldGenLevel l,ChunkAccess c,StructureManager m){
        var s=session;if(s==null||!inside(c,s.terrain))return;
        // Reserve occasional spirit trees before vanilla forests fill their crown
        // space. Full geometry preflight excludes sanctuary/blueprint columns.
        RealmEcology.spiritWood(l,c,s.terrain,s.layout);
        s.generator.applyBiomeDecoration(l,c,m); // Native placed trees, flowers, grasses, caves and ores.
        var cp=c.getPos();var p=new BlockPos.MutableBlockPos();
        for(int x=cp.getMinBlockX();x<=cp.getMaxBlockX();x++)for(int z=cp.getMinBlockZ();z<=cp.getMaxBlockZ();z++)if(s.terrain.sanctuary(x,z)||!s.terrain.natural()&&s.layout.protectedColumn(x,z)){
            int ground=surface(s,x,z,s.generator.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,c,s.random)-1);
            for(int y=ground+1;y<=Math.min(319,ground+48);y++){
                var at=p.set(x,y,z);var state=l.getBlockState(at);
                if(state.is(net.minecraft.tags.BlockTags.LOGS)||state.is(net.minecraft.tags.BlockTags.LEAVES)||state.getBlock() instanceof net.minecraft.world.level.block.BushBlock||state.is(Blocks.VINE))l.setBlock(at,Blocks.AIR.defaultBlockState(),2);
            }
        }
        RealmEcology.decorate(l,c,s.terrain,s.layout);
    }
    @Override public void spawnOriginalMobs(WorldGenRegion l){} // Realm beasts use bounded authoritative ecology.
    @Override public int getGenDepth(){return 384;}
    @Override public int getSeaLevel(){return 63;}
    @Override public int getMinY(){return -64;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor l,RandomState r){var s=session;if(s==null||!s.terrain.generates(x,z))return -64;int h=s.generator.getBaseHeight(x,z,type,l,s.random)-1;return surface(s,x,z,h)+1;}
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor l,RandomState r){var s=session;if(s!=null&&s.terrain.generates(x,z)){
        var column=s.generator.getBaseColumn(x,z,l,s.random);int original=s.generator.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,l,s.random)-1,target=surface(s,x,z,original);
        if(target!=original||s.terrain.sanctuary(x,z))for(int y=Math.max(-63,Math.min(original,target)-4);y<=Math.max(original,target);y++)column.setBlock(y,y>target?Blocks.AIR.defaultBlockState():y==target?Blocks.GRASS_BLOCK.defaultBlockState():y>target-4?Blocks.DIRT.defaultBlockState():Blocks.STONE.defaultBlockState());
        return column;
    }var states=new BlockState[384];Arrays.fill(states,Blocks.AIR.defaultBlockState());return new NoiseColumn(-64,states);}
    @Override public void addDebugScreenInfo(List<String> lines,RandomState r,BlockPos p){var s=session;lines.add(cave?"Xiantu permanent cave":"Xiantu realm: vanilla noise/caves/features");if(s!=null)s.generator.addDebugScreenInfo(lines,s.random,p);}
}
