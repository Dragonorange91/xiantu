package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.util.*;

/** Read-only, bounded per-tick audit of actual block light on the preview town. */
@Mod.EventBusSubscriber(modid="xiantu_qa", bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class TownLightingAudit {
    private static Job job;
    private static final class Job {
        final CommandSourceStack source;
        final ServerLevel level;
        final BlockPos origin;
        int[] terrain;
        int column, surfaces, minimum=15, unloaded;
        final int[] histogram=new int[16];
        final List<List<Integer>> dark=new ArrayList<>(), dim=new ArrayList<>();
        Job(CommandSourceStack s, BlockPos p) {source=s;level=s.getLevel();origin=p;}
    }
    static boolean startNatural(CommandSourceStack source,int ox,int oz,int[] terrain){
        if(job!=null)return false;
        job=new Job(source,new BlockPos(ox,0,oz));job.terrain=terrain.clone();return true;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiantutownlight").requires(s->s.hasPermission(2))
            .then(Commands.argument("x",IntegerArgumentType.integer())
            .then(Commands.argument("y",IntegerArgumentType.integer())
            .then(Commands.argument("z",IntegerArgumentType.integer()).executes(c->{
                if(job!=null){c.getSource().sendFailure(Component.literal("Town light audit already running"));return 0;}
                job=new Job(c.getSource(),new BlockPos(IntegerArgumentType.getInteger(c,"x"),IntegerArgumentType.getInteger(c,"y"),IntegerArgumentType.getInteger(c,"z")));
                c.getSource().sendSuccess(()->Component.literal("Town light audit started (read-only)"),false);return 1;
            })))));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||job==null)return;
        Job j=job;
        if(j.source.getServer().isStopped()){job=null;return;}
        // 256 columns / tick; do not force-load chunks or modify the world.
        for(int budget=0;budget<256 && j.column<(com.longju.xiantu.worldgen.town.TownLayout.WIDTH-8)*(com.longju.xiantu.worldgen.town.TownLayout.DEPTH-8);budget++,j.column++) {
            int x=4+j.column%(com.longju.xiantu.worldgen.town.TownLayout.WIDTH-8),z=4+j.column/(com.longju.xiantu.worldgen.town.TownLayout.WIDTH-8);
            int terrainY=j.terrain==null?0:j.terrain[z*com.longju.xiantu.worldgen.town.TownLayout.WIDTH+x];
            BlockPos base=j.origin.offset(x,terrainY,z);
            if(!j.level.hasChunkAt(base)){j.unloaded++;continue;}
            for(int y=0;y<38;y++) {
                BlockPos floor=base.above(y),feet=floor.above();
                var state=j.level.getBlockState(floor);
                if(state.isAir()||!state.isValidSpawn(j.level,floor,EntityType.ZOMBIE))continue;
                if(!j.level.getBlockState(feet).isAir()||!j.level.getBlockState(feet.above()).isAir())continue;
                int light=j.level.getBrightness(LightLayer.BLOCK,feet);
                j.surfaces++;j.histogram[light]++;j.minimum=Math.min(j.minimum,light);
                if(light==0)j.dark.add(List.of(x,terrainY+y+1,z));
                if(light<4)j.dim.add(List.of(x,terrainY+y+1,z,light));
            }
        }
        if(j.column<(com.longju.xiantu.worldgen.town.TownLayout.WIDTH-8)*(com.longju.xiantu.worldgen.town.TownLayout.DEPTH-8))return;
        job=null;
        try {
            Map<String,Object> report=new LinkedHashMap<>();
            report.put("origin",List.of(j.origin.getX(),j.origin.getY(),j.origin.getZ()));
            report.put("dimension",j.level.dimension().location().toString());
            report.put("gameTime",j.level.getGameTime());report.put("surfaces",j.surfaces);
            report.put("terrainFollowing",j.terrain!=null);
            report.put("unloadedColumns",j.unloaded);report.put("minimumBlockLight",j.minimum);
            report.put("histogram",j.histogram);report.put("dark",j.dark);report.put("belowFour",j.dim);
            var file=FMLPaths.GAMEDIR.get().resolve("xiantu-qa/town-lighting.json");Files.createDirectories(file.getParent());
            Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report));
            j.source.sendSuccess(()->Component.literal("Town light audit: "+j.surfaces+" surfaces, "+j.dark.size()+" dark, "+j.dim.size()+" below 4, "+j.unloaded+" unloaded columns"),false);
        } catch(Exception e){j.source.sendFailure(Component.literal("Town light audit failed: "+e.getMessage()));}
    }
}
