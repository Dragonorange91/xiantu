package com.longju.xiantu.worldgen;

import com.longju.xiantu.terrain.TerrainBiomes;
import com.longju.xiantu.terrain.TerrainBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.LinkedHashMap;

/** Small terrain-following silhouettes, not new terrain or resource deposits. */
public final class HabitatLandmarkFeature extends Feature<NoneFeatureConfiguration> {
    public HabitatLandmarkFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level=context.level();var random=context.random();var origin=context.origin();
        var biome=level.getBiome(origin);
        int kind=biome.is(TerrainBiomes.SPIRIT_MEDICINE_PLAIN)?0:
                biome.is(TerrainBiomes.THUNDER_WASTES)?1:biome.is(TerrainBiomes.METEOR_WASTELAND)?2:-1;
        if(kind<0)return false;
        // Keep every silhouette inside its owning chunk. No far writes or chunk loads.
        int x=(origin.getX()&~15)+3+random.nextInt(10),z=(origin.getZ()&~15)+3+random.nextInt(10);
        var center=surface(level,x,z);
        boolean shrub=kind==0&&random.nextInt(3)!=0;
        boolean fallen=kind==1&&random.nextInt(3)==0;
        int radius=1+random.nextInt(2),length=3+random.nextInt(3);
        Direction direction=random.nextBoolean()?Direction.EAST:Direction.SOUTH;
        var planned=new LinkedHashMap<BlockPos,BlockState>();
        if(fallen){
            for(int i=0;i<length;i++){
                var p=center.relative(direction,i-2);
                var ground=surface(level,p.getX(),p.getZ());
                if(ground.getY()!=center.getY()||!validGround(level,ground,kind))return false;
                planned.put(ground,rock("lightning_scarred_log").setValue(RotatedPillarBlock.AXIS,direction.getAxis()));
            }
        }else{
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                int distance=dx*dx+dz*dz;
                if(distance>radius*radius+1)continue;
                var ground=surface(level,x+dx,z+dz);
                if(Math.abs(ground.getY()-center.getY())>1||!validGround(level,ground,kind))return false;
                int height=distance==0?2:(radius==2&&distance<=2?2:1);
                for(int y=0;y<height;y++){
                    BlockState state;
                    if(shrub) state=dx==0&&dz==0&&y==0?Blocks.OAK_LOG.defaultBlockState():
                            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true).setValue(LeavesBlock.DISTANCE,1);
                    else if(kind==0)state=(random.nextInt(4)==0?Blocks.COBBLESTONE:Blocks.MOSSY_COBBLESTONE).defaultBlockState();
                    else if(kind==1)state=random.nextInt(4)==0?Blocks.BASALT.defaultBlockState():rock("thunder_rock");
                    else state=random.nextInt(7)==0?rock("impact_glass_rock"):rock("meteor_rock");
                    planned.put(ground.above(y),state);
                }
            }
        }
        // Atomic preflight: no buried leaves, displaced plants, water, caves or structure blocks.
        for(var p:planned.keySet())if(!level.ensureCanWrite(p)||!level.getBlockState(p).isAir()
                ||!sameBiome(level,p,kind)||level.getBlockEntity(p)!=null)return false;
        planned.forEach((p,s)->level.setBlock(p,s,2));
        return !planned.isEmpty();
    }

    private static BlockPos surface(WorldGenLevel level,int x,int z){
        return new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
    }
    private static boolean sameBiome(WorldGenLevel level,BlockPos p,int kind){
        return level.getBiome(p).is(kind==0?TerrainBiomes.SPIRIT_MEDICINE_PLAIN:
                kind==1?TerrainBiomes.THUNDER_WASTES:TerrainBiomes.METEOR_WASTELAND);
    }
    private static boolean validGround(WorldGenLevel level,BlockPos p,int kind){
        if(!sameBiome(level,p,kind))return false;
        var ground=level.getBlockState(p.below());
        if(!ground.getFluidState().isEmpty()||!ground.isFaceSturdy(level,p.below(),Direction.UP))return false;
        return kind==0?ground.is(Blocks.GRASS_BLOCK):kind==1?ground.is(rock("thunder_rock").getBlock()):
                ground.is(rock("star_dust_sand").getBlock())||ground.is(rock("meteor_soil").getBlock());
    }
    private static BlockState rock(String id){return TerrainBlockRegistry.BLOCKS_BY_ID.get(id).get().defaultBlockState();}
}
