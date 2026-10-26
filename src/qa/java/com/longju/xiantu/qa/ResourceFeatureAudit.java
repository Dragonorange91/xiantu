package com.longju.xiantu.qa;

import com.longju.xiantu.config.XiantuConfig;
import com.longju.xiantu.mining.SpecialMineralRegistry;
import com.longju.xiantu.worldgen.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.lang.reflect.*;
import java.util.*;

/** Real feature execution against restored block fixtures; biome inputs are explicit test inputs. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class ResourceFeatureAudit {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantufeatureaudit").requires(s->s.hasPermission(2)).executes(c->{
            var level=c.getSource().getLevel();
            if(!level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            var before=new LinkedHashMap<BlockPos,BlockState>();var result=new LinkedHashMap<String,Object>();
            int x=((int)c.getSource().getPosition().x+32)&~15,z=((int)c.getSource().getPosition().z+32)&~15;
            try {
                for(var m:SpecialMineralRegistry.ALL){
                    int y=(m.minY()+m.maxY())/2;var center=new BlockPos(x+8,y,z+8);
                    var host=switch(m.habitat()){case "deep"->Blocks.DEEPSLATE;case "nether"->Blocks.NETHERRACK;case "end"->Blocks.END_STONE;default->Blocks.STONE;};
                    ResourceKey<Biome> key=switch(m.habitat()){
                        case "mountain"->Biomes.JAGGED_PEAKS;case "cold"->Biomes.SNOWY_PLAINS;
                        case "thunder"->com.longju.xiantu.terrain.TerrainBiomes.THUNDER_WASTES;
                        case "nether"->Biomes.NETHER_WASTES;case "end"->Biomes.END_HIGHLANDS;default->Biomes.PLAINS;
                    };
                    // End's radial restriction is tested with a fixture beyond the central island.
                    if(m.habitat().equals("end"))center=center.offset(2048,0,0);
                    for(var p:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4)))saveSet(level,before,p,host.defaultBlockState());
                    if(m.habitat().equals("lava"))saveSet(level,before,center.offset(0,0,4),Blocks.LAVA.defaultBlockState());
                    var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
                    var view=view(level,biome,null);
                    try {
                        var seeded=net.minecraft.util.RandomSource.create(20);
                        var zeroRoll=(net.minecraft.util.RandomSource)Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{
                            if(method.getReturnType()==double.class&&method.getParameterCount()==0)return 0D;
                            try{return method.invoke(seeded,args);}catch(InvocationTargetException ex){throw ex.getCause();}
                        });
                        net.minecraft.world.level.levelgen.feature.Feature<NoneFeatureConfiguration> feature=new SpecialMineralFeature(m);
                        require(feature.place(NoneFeatureConfiguration.INSTANCE,view,level.getChunkSource().getGenerator(),zeroRoll,center),"place "+m.id());
                        Set<BlockPos> ore=new HashSet<>();
                        for(var p:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4)))if(level.getBlockState(p).is(SpecialMineralRegistry.ORES.get(m.id()).get()))ore.add(p.immutable());
                        require(ore.size()>=m.minVein()&&ore.size()<=m.maxVein(),"vein size "+m.id());
                        var connected=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(center);connected.add(center);
                        while(!queue.isEmpty()){
                            var current=queue.remove();
                            for(var direction:Direction.values()){var p=current.relative(direction);if(ore.contains(p)&&connected.add(p))queue.add(p);}
                        }
                        require(connected.equals(ore),"connected vein "+m.id());
                        for(Block block:List.of(Blocks.IRON_ORE,Blocks.STONE_BRICKS,Blocks.AIR)){
                            level.setBlock(center,block.defaultBlockState(),2);
                            require(!feature.place(NoneFeatureConfiguration.INSTANCE,view,level.getChunkSource().getGenerator(),zeroRoll,center),"reject host "+block+" "+m.id());
                            require(level.getBlockState(center).is(block),"host preserved "+m.id());
                        }
                    } finally { /* No live config mutation: controlled random roll only. */ }
                    result.put(m.id(),"feature placement and bounded vein passed; explicit biome and probability-roll fixture");
                }
                for(String biomeId:List.of("thunder_wastes","meteor_wasteland","spirit_medicine_plain")){
                    var key=ResourceKey.create(Registries.BIOME,new net.minecraft.resources.ResourceLocation("xiantu",biomeId));
                    var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
                    int top=180;
                    for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++)for(int depth=0;depth<32;depth++)
                        saveSet(level,before,new BlockPos(x+dx,top-depth,z+dz),depth==0?Blocks.GRASS_BLOCK.defaultBlockState():depth==8||depth==9?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState());
                    net.minecraft.world.level.levelgen.feature.Feature<NoneFeatureConfiguration> rockFeature=new TerrainRockLayerFeature();
                    require(rockFeature.place(NoneFeatureConfiguration.INSTANCE,view(level,biome,top+1),level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(20),new BlockPos(x,top,z)),"geology fixture "+biomeId);
                    for(int depth=1;depth<8;depth++)require(!level.getBlockState(new BlockPos(x,top-depth,z)).is(Blocks.STONE),"upper geology depth "+depth);
                    require(level.getBlockState(new BlockPos(x,top,z)).is(Blocks.GRASS_BLOCK),"grass preserved");
                    require(level.getBlockState(new BlockPos(x,top-10,z)).is(Blocks.STONE),"cave floor not repainted");
                    require(level.getBlockState(new BlockPos(x,top-25,z)).is(Blocks.STONE),"deep layer retained");
                    // With no cavity, the replacement ends at exactly 18 blocks from the first surface.
                    for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++)for(int depth=1;depth<32;depth++)level.setBlock(new BlockPos(x+dx,top-depth,z+dz),Blocks.STONE.defaultBlockState(),2);
                    rockFeature.place(NoneFeatureConfiguration.INSTANCE,view(level,biome,top+1),level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(20),new BlockPos(x,top,z));
                    require(!level.getBlockState(new BlockPos(x,top-17,z)).is(Blocks.STONE)&&level.getBlockState(new BlockPos(x,top-18,z)).is(Blocks.STONE),"18-block depth bound");
                    result.put(biomeId,"surface geology, 18-block bound, cave floor and grass preservation passed; explicit biome and probability-roll fixture");
                }
                c.getSource().sendSuccess(()->Component.literal("Feature audit PASS "+result.keySet()),false);
                java.nio.file.Files.writeString(FMLPaths.GAMEDIR.get().resolve("xiantu-feature-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
                return result.size();
            }catch(Exception|AssertionError|LinkageError ex){c.getSource().sendFailure(Component.literal("Feature audit FAIL: "+ex));ex.printStackTrace();return 0;}
            finally{before.forEach((p,s)->level.setBlock(p,s,2));}
        }));
    }
    private static WorldGenLevel view(net.minecraft.server.level.ServerLevel level,Holder<Biome> biome,Integer top){
        return (WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class[]{WorldGenLevel.class},(proxy,method,args)->{
            if(method.getReturnType()==Holder.class&&args!=null&&args.length==1&&args[0] instanceof BlockPos)return biome;
            if(top!=null&&method.getReturnType()==int.class&&args!=null&&args.length==3&&args[0] instanceof net.minecraft.world.level.levelgen.Heightmap.Types)return top;
            try{return method.invoke(level,args);}catch(InvocationTargetException ex){throw ex.getCause();}
        });
    }
    private static void saveSet(net.minecraft.server.level.ServerLevel level,Map<BlockPos,BlockState> before,BlockPos p,BlockState s){
        if(level.getBlockEntity(p)!=null)throw new IllegalStateException("Fixture contains block entity");
        before.putIfAbsent(p.immutable(),level.getBlockState(p));level.setBlock(p,s,2);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
