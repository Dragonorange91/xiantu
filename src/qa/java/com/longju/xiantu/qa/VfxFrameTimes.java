package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** QA-only frame interval measurement. No screenshot encoding during sampling. */
@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class VfxFrameTimes {
    private static final List<Double> frames=new ArrayList<>();
    private static String name;
    private static long last,end;
    @SubscribeEvent public static void serverCommands(net.minecraftforge.event.RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantuanatomybench").requires(s->s.hasPermission(2))
            .then(Commands.argument("label",StringArgumentType.word())
            .then(Commands.argument("seconds",IntegerArgumentType.integer(5,60)).executes(c->{
                if(!c.getSource().getServer().getWorldData().getLevelName().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
                String label=StringArgumentType.getString(c,"label");
                if(!label.matches("[A-Za-z0-9_-]{1,48}"))return 0;
                int seconds=IntegerArgumentType.getInteger(c,"seconds");
                Minecraft.getInstance().execute(()->{name=label;last=0;frames.clear();end=System.nanoTime()+seconds*1_000_000_000L;});
                return 1;
            }))));
    }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantuvfxbench")
            .then(Commands.argument("label",StringArgumentType.word())
            .then(Commands.argument("seconds",IntegerArgumentType.integer(5,60)).executes(c->{
                var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
                if(server==null || !Set.of(CreatureShowcase.WORLD,"Xiantu-QA-BeastFlat-20261017").contains(server.getWorldData().getLevelName()))return 0;
                String label=StringArgumentType.getString(c,"label");
                if(!label.matches("[A-Za-z0-9_-]{1,48}"))return 0;
                name=label;last=0;frames.clear();end=System.nanoTime()+IntegerArgumentType.getInteger(c,"seconds")*1_000_000_000L;
                mc.player.displayClientMessage(Component.literal("[VFX frame time] "+name),false);return 1;
            }))));
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(e.phase!=TickEvent.Phase.START || name==null)return;
        long now=System.nanoTime();if(last!=0)frames.add((now-last)/1_000_000.0);last=now;
        if(now<end)return;
        var mc=Minecraft.getInstance();var sorted=frames.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        Map<String,Object> result=new LinkedHashMap<>();result.put("label",name);result.put("frames",frames.size());
        result.put("width",mc.getWindow().getWidth());result.put("height",mc.getWindow().getHeight());
        if(sorted.length>0){
            result.put("mean_ms",frames.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            result.put("p50_ms",sorted[(sorted.length-1)/2]);
            result.put("p95_ms",sorted[(int)((sorted.length-1)*.95)]);
            result.put("p99_ms",sorted[(int)((sorted.length-1)*.99)]);
            result.put("max_ms",sorted[sorted.length-1]);
            result.put("over_50ms",frames.stream().filter(x->x>50).count());
        }
        result.put("intervals_ms",List.copyOf(frames));
        try{
            Path folder=mc.gameDirectory.toPath().resolve("xiantu-qa-vfx-v9");Files.createDirectories(folder);
            Files.writeString(folder.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
            LogUtils.getLogger().info("[Xiantu VFX frames] {} frames={} p99={} max={}",name,sorted.length,result.get("p99_ms"),result.get("max_ms"));
        }catch(Exception ex){LogUtils.getLogger().error("Cannot save frame intervals",ex);}
        name=null;frames.clear();
    }
    private VfxFrameTimes(){}
}
