package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import com.longju.xiantu.worldgen.town.*;
import com.longju.xiantu.trade.TownMerchantEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.util.*;

/** Verifies a stored native StructureStart; a pasted template cannot pass this audit. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class TownNativeAudit {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantutowninspect").requires(s->s.hasPermission(2))
            .then(Commands.argument("chunk_x",IntegerArgumentType.integer())
            .then(Commands.argument("chunk_z",IntegerArgumentType.integer()).executes(c->{
                var level=c.getSource().getLevel();
                if(!level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
                int cx=IntegerArgumentType.getInteger(c,"chunk_x"),cz=IntegerArgumentType.getInteger(c,"chunk_z");
                var chunk=level.getChunkSource().getChunkNow(cx,cz);
                if(chunk==null){c.getSource().sendFailure(Component.literal("Candidate chunk not loaded"));return 0;}
                var type=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("xiantu","cultivation_town"));
                var start=chunk.getStartForStructure(type);
                if(start==null||!start.isValid()){c.getSource().sendFailure(Component.literal("No valid native town StructureStart"));return 0;}
                var tiles=start.getPieces().stream().filter(p->p instanceof TownTerrainPiece).map(p->(TownTerrainPiece)p).toList();
                int ox=tiles.get(0).originX(),oz=tiles.get(0).originZ();int[] heights=new int[TownLayout.WIDTH*TownLayout.DEPTH];
                int unloaded=0,missing=0,grade=0;var missingCells=new ArrayList<Object>();
                for(int x=0;x<TownLayout.WIDTH;x++)for(int z=0;z<TownLayout.DEPTH;z++){
                    int px=x,pz=z;var tile=tiles.stream().filter(t->t.covers(px,pz)).findFirst().orElseThrow();int h=tile.plannedHeight(x,z);heights[z*TownLayout.WIDTH+x]=h;
                    BlockPos p=new BlockPos(ox+x,h,oz+z);
                    if(!level.hasChunkAt(p)){unloaded++;continue;}
                    if(x>=3&&x<TownLayout.WIDTH-3&&z>=3&&z<TownLayout.DEPTH-3&&level.getBlockState(p).isAir()){missing++;if(missingCells.size()<40)missingCells.add(List.of(x,h,z));}
                }
                for(int x=0;x<TownLayout.WIDTH-1;x++)for(int z=0;z<TownLayout.DEPTH-1;z++){
                    int i=z*TownLayout.WIDTH+x;if(Math.abs(heights[i]-heights[i+1])>1||Math.abs(heights[i]-heights[i+TownLayout.WIDTH])>1)grade++;
                }
                var merchants=level.getEntitiesOfClass(TownMerchantEntity.class,new AABB(ox,level.getMinBuildHeight(),oz,ox+TownLayout.WIDTH,level.getMaxBuildHeight(),oz+TownLayout.DEPTH));
                int roadCells=0,oreHosts=0,roadWidthErrors=0,stairErrors=0;var roadErrors=new ArrayList<Object>();
                for(var road:TownRoads.ALL)for(int t=Math.max(0,road.start());t<=road.end();t++){
                    var section=road.section(t);Integer crossHeight=null;
                    for(int x=section.x0();x<=section.x1();x++)for(int z=section.z0();z<=section.z1();z++){
                        if(x<0||z<0||x>=TownLayout.WIDTH||z>=TownLayout.DEPTH)continue;
                        int h=heights[z*TownLayout.WIDTH+x];if(crossHeight!=null&&crossHeight!=h)roadWidthErrors++;crossHeight=h;
                        int px=x,pz=z;if(TownTerrainPlan.PLOTS.stream().anyMatch(p->p.distance(px,pz)==0))continue;
                        var pos=new BlockPos(ox+x,h,oz+z);var state=level.getBlockState(pos);roadCells++;
                        if(state.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES)||state.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES)||net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).getPath().contains("ore"))oreHosts++;
                        if(!level.getBlockState(pos.above()).isAir())continue;
                        var directions=road.alongX()?new net.minecraft.core.Direction[]{net.minecraft.core.Direction.WEST,net.minecraft.core.Direction.EAST}:new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH};
                        for(var d:directions){int nx=x+d.getStepX(),nz=z+d.getStepZ();if(nx<0||nz<0||nx>=TownLayout.WIDTH||nz>=TownLayout.DEPTH||!road.contains(nx,nz))continue;
                            if(heights[nz*TownLayout.WIDTH+nx]==h-1&&(!(state.getBlock() instanceof net.minecraft.world.level.block.StairBlock)||state.getValue(net.minecraft.world.level.block.StairBlock.FACING)!=d.getOpposite())){
                                stairErrors++;if(roadErrors.size()<30)roadErrors.add(List.of(x,h,z,d.toString(),state.toString()));
                            }
                        }
                    }
                }
                int collisionSteps=0,collisionErrors=0;var collisionExamples=new ArrayList<Object>();
                for(var road:TownRoads.ALL)for(int lane=road.low();lane<=road.high();lane++){
                    Double previous=null;
                    for(int t=Math.max(0,road.start());t<=road.end();t++){
                        int x=road.alongX()?t:lane,z=road.alongX()?lane:t;
                        if(x<0||z<0||x>=TownLayout.WIDTH||z>=TownLayout.DEPTH){previous=null;continue;}
                        int h=heights[z*TownLayout.WIDTH+x];var pos=new BlockPos(ox+x,h,oz+z);
                        // Read actual collision boxes, including authored gate stairs above the terrain datum.
                        if(!level.getBlockState(pos.above(3)).isAir()){previous=null;continue;}
                        for(double along:new double[]{.25,.75}){
                            double fx=road.alongX()?along:.5,fz=road.alongX()?.5:along,top=Double.NEGATIVE_INFINITY;
                            for(int y=0;y<=2;y++){var at=pos.above(y);for(var box:level.getBlockState(at).getCollisionShape(level,at).toAabbs())
                                if(fx>box.minX&&fx<box.maxX&&fz>box.minZ&&fz<box.maxZ)top=Math.max(top,h+y+box.maxY);
                            }
                            if(!Double.isFinite(top)){previous=null;continue;}
                            if(previous!=null){collisionSteps++;if(Math.abs(top-previous)>.60001){collisionErrors++;if(collisionExamples.size()<30)collisionExamples.add(List.of(x,z,along,previous,top));}}
                            previous=top;
                        }
                    }
                }
                var npcs=new ArrayList<Object>();for(var merchant:merchants){net.minecraft.world.entity.Entity e=merchant;npcs.add(Map.of("type",net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).toString(),"uuid",e.getUUID().toString(),"position",List.of(e.getX(),e.getY(),e.getZ())));}
                var result=new LinkedHashMap<String,Object>();result.put("nativeStart",true);result.put("chunk",List.of(cx,cz));result.put("origin",List.of(ox,oz));result.put("pieces",tiles.size());result.put("unloadedColumns",unloaded);result.put("missingGround",missing);result.put("missingCells",missingCells);result.put("gradeViolations",grade);result.put("heights",heights);result.put("merchants",npcs);
                result.put("roadCells",roadCells);result.put("roadOreHosts",oreHosts);result.put("roadWidthErrors",roadWidthErrors);result.put("roadStairErrors",stairErrors);result.put("roadErrors",roadErrors);
                result.put("collisionSteps",collisionSteps);result.put("collisionErrors",collisionErrors);result.put("collisionExamples",collisionExamples);
                try{var path=FMLPaths.GAMEDIR.get().resolve("xiantu-qa/town-native.json");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception e){throw new RuntimeException(e);}
                TownLightingAudit.startNatural(c.getSource(),ox,oz,heights);
                int absent=missing,load=unloaded;c.getSource().sendSuccess(()->Component.literal("Native town: "+tiles.size()+" pieces, "+merchants.size()+" merchants, "+absent+" missing ground, "+load+" unloaded; terrain light audit started"),false);return 1;
            }))));
    }
}
