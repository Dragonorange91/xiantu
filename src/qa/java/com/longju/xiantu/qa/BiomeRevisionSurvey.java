package com.longju.xiantu.qa;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/** Bounded noise-biome sampling, no chunks edited or loaded by the survey. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class BiomeRevisionSurvey {
    private static CommandSourceStack source;private static int row;private static int originX,originZ;
    private static final List<List<String>> rows=new ArrayList<>();
    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("xiantubiomesurvey").requires(s->s.hasPermission(2)).executes(c->{
            if(source!=null)return 0;
            if(!c.getSource().getLevel().getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            source=c.getSource();row=0;rows.clear();originX=(int)source.getPosition().x-4096;originZ=(int)source.getPosition().z-4096;
            source.sendSuccess(()->Component.literal("Biome survey started: 8192 square, 32-block grid, Y=72, no chunk generation"),false);return 1;
        }));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||source==null)return;
        try {
            var level=source.getLevel();var generator=level.getChunkSource().getGenerator();var sampler=level.getChunkSource().randomState().sampler();
            var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
            for(int batch=0;batch<4&&row<256;batch++,row++){
                List<String> line=new ArrayList<>();
                for(int col=0;col<256;col++){
                    var biome=generator.getBiomeSource().getNoiseBiome((originX+col*32)>>2,18,(originZ+row*32)>>2,sampler);
                    line.add(registry.getKey(biome.value()).toString());
                }
                rows.add(line);
            }
            if(row>=256){
                var counts=new TreeMap<String,Integer>();rows.forEach(r->r.forEach(b->counts.merge(b,1,Integer::sum)));
                var report=Map.of("seed",level.getSeed(),"origin",List.of(originX,72,originZ),"step",32,"rows",rows,"counts",counts);
                java.nio.file.Files.writeString(FMLPaths.GAMEDIR.get().resolve("xiantu-biome-survey.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
                source.sendSuccess(()->Component.literal("Biome survey complete: "+counts.entrySet().stream().filter(v->v.getKey().startsWith("xiantu:")).toList()),false);source=null;
            }
        } catch(Exception ex){if(source!=null)source.sendFailure(Component.literal(ex.toString()));source=null;}
    }
    private BiomeRevisionSurvey() {}
}
