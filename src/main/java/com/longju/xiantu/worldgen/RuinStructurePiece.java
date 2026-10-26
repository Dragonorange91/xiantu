package com.longju.xiantu.worldgen;
import com.longju.xiantu.worldgen.town.TownWorldgen;
import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.*;

public final class RuinStructurePiece extends StructurePiece {
    private final String blueprint;private final BlockPos origin;private final int x0,z0,w,d,ground;
    public RuinStructurePiece(String name,BlockPos origin,int x,int z,int w,int d,int minY,int maxY,int ground){super(TownWorldgen.RUIN_TILE.get(),0,new BoundingBox(origin.getX()+x,minY,origin.getZ()+z,origin.getX()+x+w-1,maxY,origin.getZ()+z+d-1));blueprint=name;this.origin=origin;x0=x;z0=z;this.w=w;this.d=d;this.ground=ground;}
    public RuinStructurePiece(StructurePieceSerializationContext context,CompoundTag n){super(TownWorldgen.RUIN_TILE.get(),n);blueprint=n.getString("Blueprint");origin=BlockPos.of(n.getLong("Origin"));x0=n.getInt("X");z0=n.getInt("Z");w=n.getInt("W");d=n.getInt("D");ground=n.getInt("Ground");if(w<1||w>16||d<1||d>16)throw new IllegalArgumentException("Invalid ruin tile");}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag n){n.putString("Blueprint",blueprint);n.putLong("Origin",origin.asLong());n.putInt("X",x0);n.putInt("Z",z0);n.putInt("W",w);n.putInt("D",d);n.putInt("Ground",ground);}
    @Override public void postProcess(WorldGenLevel l,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var server=l.getLevel();var spec=RuinSpecs.load(server.getServer(),blueprint);String root=RuinEncounterService.key(server,blueprint,origin);String mode=spec.getAsJsonObject("placement").get("mode").getAsString();
        var template=server.getStructureManager().get(new ResourceLocation("xiantu","ruins/"+blueprint)).orElseThrow();var index=RuinTemplateIndex.of(template);var entry=RuinSpecs.offset(origin,spec.getAsJsonArray("entry"));
        for(int x=x0;x<x0+w;x++)for(int z=z0;z<z0+d;z++){
            int X=origin.getX()+x,Z=origin.getZ()+z;if(X<clip.minX()||X>clip.maxX()||Z<clip.minZ()||Z>clip.maxZ())continue;
            var cells=index.column(x,z);int actual=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,X,Z)-1;
            var solid=cells.stream().filter(c->!c.state().isAir()&&c.state().getFluidState().isEmpty()).findFirst();
            if(!spec.has("layoutGraph")&&solid.isEmpty())cells=java.util.List.of();
            if(!mode.equals("floating")&&!mode.equals("underground")&&solid.isPresent()&&solid.get().y()<spec.getAsJsonArray("entry").get(1).getAsInt()){
                int base=origin.getY()+solid.get().y();
                for(int y=base-1;y>=Math.max(l.getMinBuildHeight()+1,base-16);y--){var p=new BlockPos(X,y,Z);if(l.getBlockState(p).isFaceSturdy(l,p,Direction.UP)&&l.getFluidState(p).isEmpty())break;l.setBlock(p,y<base-3?Blocks.STONE.defaultBlockState():Blocks.MOSSY_COBBLESTONE.defaultBlockState(),2);}
            }
            for(var cell:cells){
                var at=new BlockPos(X,origin.getY()+cell.y(),Z);if(!clip.isInside(at))continue;
                var state=cell.state();var info=new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(at,state,cell.data());
                // The existing herb processor gives independently seeded tier variants on proper grass.
                var processed=RuinHerbProcessor.INSTANCE.processBlock(l,origin,origin,info,info,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings());
                if(processed!=null)state=processed.state();l.setBlock(at,state,2);
                if(cell.data()!=null&&l.getBlockEntity(at)!=null){var n=cell.data().copy();n.putInt("x",X);n.putInt("y",at.getY());n.putInt("z",Z);l.getBlockEntity(at).load(n);}
                if(!state.getFluidState().isEmpty())l.scheduleTick(at,state.getFluidState().getType(),1);
            }
            // Blend the exterior apron by at most one block per horizontal block.
            if(!mode.equals("floating")&&!mode.equals("underground")&&!mode.equals("underwater")&&Math.abs(X-entry.getX())<=3&&Z>=entry.getZ()&&Z<=entry.getZ()+10){
                int delta=Z-entry.getZ();int exit=generator.getBaseHeight(entry.getX(),entry.getZ()+10,Heightmap.Types.OCEAN_FLOOR_WG,l,server.getChunkSource().randomState())-1;int target=entry.getY()-1+(int)Math.round((exit-entry.getY()+1)*delta/10.0);
                for(int y=Math.min(actual,target);y<=target;y++)l.setBlock(new BlockPos(X,y,Z),y==target?Blocks.MOSSY_STONE_BRICKS.defaultBlockState():Blocks.STONE.defaultBlockState(),2);
                for(int y=target+1;y<=Math.max(actual+2,target+3);y++)l.setBlock(new BlockPos(X,y,Z),Blocks.AIR.defaultBlockState(),2);
            }
        }
        if(spec.has("resourceNodes"))for(var v:spec.getAsJsonArray("resourceNodes")){var m=v.getAsJsonObject();var at=RuinSpecs.offset(origin,m.getAsJsonArray("position"));if(owns(at,clip)){
            var ore=m.has("resource")&&m.get("resource").getAsString().equals("spirit_gold_essence")?com.longju.xiantu.refining.QitianMaterials.ORE.get():com.longju.xiantu.mining.AdvancedMinerals.ORES.get("causality_stone").get();
            l.setBlock(at,ore.defaultBlockState(),2);
        }}
        for(var v:spec.getAsJsonArray("encounters")){var e=v.getAsJsonObject();var at=RuinSpecs.offset(origin,e.getAsJsonArray("position"));if(owns(at,clip)){l.setBlock(at,ModBlocks.RUIN_ENCOUNTER.get().defaultBlockState(),2);if(l.getBlockEntity(at)!=null)l.getBlockEntity(at).load(RuinAdventureService.sealData(root,blueprint,origin,e));}}
        for(var v:spec.getAsJsonArray("markers")){var m=v.getAsJsonObject();if(!m.get("type").getAsString().equals("reward"))continue;var at=RuinSpecs.offset(origin,m.getAsJsonArray("position"));if(owns(at,clip)&&l.getBlockEntity(at)!=null)l.getBlockEntity(at).load(RuinAdventureService.chestData(root,blueprint,origin,m));}
    }
    private boolean owns(BlockPos p,BoundingBox clip){return clip.isInside(p)&&p.getX()>=origin.getX()+x0&&p.getX()<origin.getX()+x0+w&&p.getZ()>=origin.getZ()+z0&&p.getZ()<origin.getZ()+z0+d;}
}
