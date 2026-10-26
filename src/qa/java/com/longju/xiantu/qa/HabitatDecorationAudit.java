package com.longju.xiantu.qa;

import com.longju.xiantu.terrain.*;
import com.longju.xiantu.worldgen.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.RegisterCommandsEvent;
import java.lang.reflect.*;
import java.util.*;

/** In-memory terrain fixtures exercise actual registered features without changing the save. */
public final class HabitatDecorationAudit {
    private static int checks;
    public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantuhabitataudit").requires(s->s.hasPermission(2)).executes(c->{
            checks=0;var counts=new TreeMap<String,Integer>();
            try{
                var level=c.getSource().getLevel();
                for(int kind=0;kind<3;kind++)for(int scenario=0;scenario<4;scenario++)for(int seed=0;seed<64;seed++){
                    var fixture=new Fixture(level,kind,scenario);var view=fixture.view();
                    Feature<NoneFeatureConfiguration> feature=new HabitatLandmarkFeature();
                    boolean success=feature.place(NoneFeatureConfiguration.INSTANCE,view,level.getChunkSource().getGenerator(),RandomSource.create(seed),new BlockPos(8,fixture.height(8),8));
                    check(success==!fixture.writes.isEmpty(),"return status agrees with atomic placement");
                    for(var entry:fixture.writes.entrySet()){
                        var p=entry.getKey();check(p.getX()>=0&&p.getX()<16&&p.getZ()>=0&&p.getZ()<16,"bounded landmark chunk");
                        check(p.getY()>=fixture.height(p.getX()),"never replaces ground");
                        check(scenario!=2,"no underwater landmarks");
                        check(scenario!=3||p.getX()<8,"no neighboring biome writes");
                    }
                    counts.merge("landmark_"+kind+"_scenario_"+scenario,success?1:0,Integer::sum);
                }
                for(int scenario=0;scenario<4;scenario++)for(int seed=0;seed<64;seed++){
                    var fixture=new Fixture(level,1,scenario);var view=fixture.view();
                    Feature<NoneFeatureConfiguration> bamboo=new StormBambooPatchFeature();
                    bamboo.place(NoneFeatureConfiguration.INSTANCE,view,level.getChunkSource().getGenerator(),RandomSource.create(seed),new BlockPos(8,fixture.height(8),8));
                    check(fixture.writes.size()%2==0,"complete bamboo pairs");
                    for(var entry:fixture.writes.entrySet()){
                        var p=entry.getKey();var state=entry.getValue();
                        check(scenario!=2,"no underwater bamboo");
                        check(scenario!=3||p.getX()<8,"bamboo stays in thunder biome");
                        check(state.canSurvive(view,p),"bamboo supported on slopes and has partner");
                    }
                    counts.merge("bamboo_scenario_"+scenario,fixture.writes.size()/2,Integer::sum);
                }
                for(int kind=0;kind<3;kind++)check(counts.get("landmark_"+kind+"_scenario_0")>40,"landmarks actually generate on valid terrain");
                check(counts.get("bamboo_scenario_1")>100,"bamboo actually decorates sloping terrain");
                var report=new LinkedHashMap<String,Object>();report.put("passed",true);report.put("checks",checks);report.put("placements",counts);
                report.put("scope","Registered feature algorithms, synthetic flat/slope/water/biome-edge fixtures; no save edits");
                java.nio.file.Files.writeString(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("habitat-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
                c.getSource().sendSuccess(()->Component.literal("Habitat audit PASS "+checks),false);return checks;
            }catch(Throwable ex){c.getSource().sendFailure(Component.literal("Habitat audit FAILED "+ex));ex.printStackTrace();return 0;}
        }));
    }
    private static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    private static final class Fixture {
        final ServerLevel level;final int kind,scenario;final Map<BlockPos,BlockState> writes=new LinkedHashMap<>();
        Fixture(ServerLevel l,int k,int s){level=l;kind=k;scenario=s;}
        int height(int x){return 80+(scenario==1?Math.floorDiv(x,4):0);}
        BlockState state(BlockPos p){
            if(writes.containsKey(p))return writes.get(p);
            if(p.getY()>=height(p.getX()))return p.getY()==height(p.getX())&&scenario==2?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
            return kind==0?Blocks.GRASS_BLOCK.defaultBlockState():TerrainBlockRegistry.BLOCKS_BY_ID.get(kind==1?"thunder_rock":"star_dust_sand").get().defaultBlockState();
        }
        WorldGenLevel view(){return (WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class[]{WorldGenLevel.class},(proxy,m,a)->{
            if(m.getReturnType()==BlockState.class&&a!=null&&a[0] instanceof BlockPos p)return state(p);
            if(m.getReturnType()==FluidState.class&&a!=null&&a[0] instanceof BlockPos p)return state(p).getFluidState();
            if(m.getReturnType()==Holder.class&&a!=null&&a[0] instanceof BlockPos p)return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                scenario==3&&p.getX()>=8?Biomes.PLAINS:kind==0?TerrainBiomes.SPIRIT_MEDICINE_PLAIN:kind==1?TerrainBiomes.THUNDER_WASTES:TerrainBiomes.METEOR_WASTELAND);
            if(a!=null&&a.length>=2&&a[0] instanceof BlockPos p&&a[1] instanceof BlockState s){check(state(p).isAir(),"never replaces existing solids or water");writes.put(p.immutable(),s);return true;}
            if(m.getReturnType()==int.class&&a!=null&&a.length==3&&a[0] instanceof Heightmap.Types)return height((int)a[1])+(scenario==2?1:0);
            if(m.getReturnType()==net.minecraft.world.level.block.entity.BlockEntity.class)return null;
            if(m.getReturnType()==boolean.class&&a!=null&&a.length==1&&a[0] instanceof BlockPos)return true;
            try{return m.invoke(level,a);}catch(InvocationTargetException ex){throw ex.getCause();}
        });}
    }
}
