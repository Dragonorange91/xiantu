package com.longju.xiantu.worldgen.town;

import com.longju.xiantu.registry.ModEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.*;

/** Small chunk-clipped pieces serialize their own terrain profile, not a global mutable placement job. */
public final class TownTerrainPiece extends StructurePiece {
    private final int ox,oz,x0,z0,width,depth;
    private final int[] heights;
    public TownTerrainPiece(int ox,int oz,int x,int z,int w,int d,TownTerrainPlan plan){
        super(TownWorldgen.TILE.get(),0,new BoundingBox(ox+x,plan.minimum()-96,oz+z,ox+x+w-1,plan.maximum()+40,oz+z+d-1));
        this.ox=ox;this.oz=oz;x0=x;z0=z;width=w;depth=d;heights=new int[(w+2)*(d+2)];
        for(int a=-1;a<=w;a++)for(int b=-1;b<=d;b++)heights[(b+1)*(w+2)+a+1]=plan.height(Math.max(-8,Math.min(TownLayout.WIDTH+7,x+a)),Math.max(-8,Math.min(TownLayout.DEPTH+7,z+b)));
    }
    public TownTerrainPiece(StructurePieceSerializationContext context,CompoundTag tag){
        super(TownWorldgen.TILE.get(),tag);ox=tag.getInt("OX");oz=tag.getInt("OZ");x0=tag.getInt("X");z0=tag.getInt("Z");width=tag.getInt("W");depth=tag.getInt("D");heights=tag.getIntArray("Heights");
        if(width<1||width>16||depth<1||depth>16||heights.length!=(width+2)*(depth+2))throw new IllegalArgumentException("Invalid town terrain tile");
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){
        tag.putInt("OX",ox);tag.putInt("OZ",oz);tag.putInt("X",x0);tag.putInt("Z",z0);tag.putInt("W",width);tag.putInt("D",depth);tag.putIntArray("Heights",heights);
    }
    private int height(int x,int z){return heights[(z-z0+1)*(width+2)+x-x0+1];}
    public int originX(){return ox;}
    public int originZ(){return oz;}
    public boolean covers(int x,int z){return x>=x0&&x<x0+width&&z>=z0&&z<z0+depth;}
    public int plannedHeight(int x,int z){if(!covers(x,z))throw new IllegalArgumentException("Outside tile");return height(x,z);}
    private static boolean plot(int x,int z){for(var p:TownTerrainPlan.PLOTS)if(p.distance(x,z)==0)return true;return false;}
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox chunkBox,ChunkPos chunk,BlockPos pivot){
        var template=level.getLevel().getStructureManager().get(CultivationTownStructure.TEMPLATE).orElseThrow(()->new IllegalStateException("Missing accepted cultivation town template"));
        TownBlueprint blueprint=TownBlueprint.of(template);
        for(int x=x0;x<x0+width;x++)for(int z=z0;z<z0+depth;z++){
            int h=height(x,z);BlockPos ground=new BlockPos(ox+x,h,oz+z);
            if(!chunkBox.isInside(ground))continue;
            var cells=blueprint.column(x,z);
            var road=TownRoads.at(x,z);
            int actual=level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,ground.getX(),ground.getZ())-1;
            boolean inside=x>=3&&x<=TownLayout.WIDTH-4&&z>=3&&z<=TownLayout.DEPTH-4;
            boolean approach=(Math.abs(x-TownLayout.CENTER_X)<=3)||(Math.abs(z-TownLayout.CENTER_Z)<=3);
            // The 8-block exterior apron blends the gate approaches and foundations into natural ground.
            if(!inside&&cells.isEmpty()&&!approach&&Math.abs(actual-h)<=1)continue;
            BlockState nativeSurface=level.getBlockState(new BlockPos(ground.getX(),actual,ground.getZ()));
            if(nativeSurface.isAir()||!nativeSurface.getFluidState().isEmpty())nativeSurface=Blocks.GRASS_BLOCK.defaultBlockState();
            int clearTop=Math.min(level.getMaxBuildHeight()-1,Math.max(h+40,level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,ground.getX(),ground.getZ())));
            for(int y=h+1;y<=clearTop;y++){
                BlockPos p=new BlockPos(ground.getX(),y,ground.getZ());
                if(!level.getBlockState(p).isAir())level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            }
            boolean building=plot(x,z);
            boolean paved=road!=null||cells.stream().anyMatch(c->c.y()==0&&paving(c.state()));
            // Like village foundations, descend to actual solid ground, including narrow dry fissures.
            // The planner caps exceptional deep support area at 2%; this is never a city-wide raised platform.
            for(int y=h-1;y>=Math.max(level.getMinBuildHeight()+1,h-96);y--){
                BlockPos p=new BlockPos(ground.getX(),y,ground.getZ());var old=level.getBlockState(p);
                // Replace the exposed road bed too: stopping at native solid rock exposes ore on risers.
                if((!paved||h-y>3)&&old.isFaceSturdy(level,p,Direction.UP)&&old.getFluidState().isEmpty())break;
                level.setBlock(p,building||paved?Blocks.STONE_BRICKS.defaultBlockState():h-y>3?Blocks.STONE.defaultBlockState():Blocks.DIRT.defaultBlockState(),2);
            }
            level.setBlock(ground,nativeSurface,2);
            BlockState pavement=null;boolean aboveOccupied=false;
            for(var cell:cells){
                // Natural andesite is an ore-replaceable host; finished paving must not be one.
                BlockState state=cell.y()==0&&cell.state().is(Blocks.ANDESITE)?Blocks.POLISHED_ANDESITE.defaultBlockState():cell.state();
                BlockPos p=ground.above(cell.y());level.setBlock(p,state,2);
                if(cell.y()==0)pavement=state;if(cell.y()==1&&!state.isAir())aboveOccupied=true;
                if(cell.data()!=null){var be=level.getBlockEntity(p);if(be!=null){var data=cell.data().copy();data.putInt("x",p.getX());data.putInt("y",p.getY());data.putInt("z",p.getZ());be.load(data);be.setChanged();}}
                if(!cell.state().getFluidState().isEmpty())level.scheduleTick(p,cell.state().getFluidState().getType(),0);
            }
            if(cells.isEmpty()&&approach){pavement=Blocks.STONE_BRICKS.defaultBlockState();level.setBlock(ground,pavement,2);}
            // A rigid plot can still need a half-step at its outer paving edge. Excluding the whole
            // plot leaves a full-block lip at plaza and shop entrances even when the road grade is valid.
            if(!aboveOccupied&&road!=null&&pavement!=null){
                Direction[] downhill=road.alongX()?new Direction[]{Direction.WEST,Direction.EAST}:new Direction[]{Direction.NORTH,Direction.SOUTH};
                for(Direction direction:downhill){
                    int nx=x+direction.getStepX(),nz=z+direction.getStepZ();
                    if(nx<-8||nx>TownLayout.WIDTH+7||nz<-8||nz>TownLayout.DEPTH+7)continue;
                    if(height(nx,nz)==h-1){
                        var stair=pavement.is(Blocks.STONE_BRICKS)?Blocks.STONE_BRICK_STAIRS:Blocks.POLISHED_ANDESITE_STAIRS;
                        // Replace the HIGH cell's surface, rather than stacking stairs over the low cell.
                        level.setBlock(ground,stair.defaultBlockState().setValue(StairBlock.FACING,direction.getOpposite()),2);break;
                    }
                }
            }
        }
        for(var marker:TownLayout.MERCHANTS)merchant(level,chunkBox,marker.profession(),marker.x(),marker.y(),marker.z(),marker.yaw());
    }
    private static boolean paving(BlockState state){return state.is(Blocks.ANDESITE)||state.is(Blocks.POLISHED_ANDESITE)||state.is(Blocks.SMOOTH_STONE)||state.is(Blocks.STONE_BRICKS);}
    private void merchant(WorldGenLevel level,BoundingBox clip,String profession,int x,int y,int z,float yaw){
        if(x<x0||x>=x0+width||z<z0||z>=z0+depth)return;
        BlockPos p=new BlockPos(ox+x,height(x,z)+y,oz+z);if(!clip.isInside(p))return;
        var npc=ModEntities.MERCHANTS.get(profession).get().create(level.getLevel());if(npc==null)return;
        npc.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,yaw,0);
        npc.getPersistentData().putInt("TownOriginX",ox);npc.getPersistentData().putInt("TownOriginZ",oz);npc.getPersistentData().putBoolean("TownNatural",true);
        level.addFreshEntity(npc);
    }
}
