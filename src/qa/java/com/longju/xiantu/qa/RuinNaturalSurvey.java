package com.longju.xiantu.qa;
import com.google.gson.*;
import com.longju.xiantu.worldgen.*;
import net.minecraft.commands.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RuinNaturalSurvey {
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e){source=null;ids=null;results=null;index=attempt=0;}
    private static int maximum=128;private static CommandSourceStack source;private static List<String> ids;private static int index,attempt;private static JsonArray results;
    @SubscribeEvent public static void command(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xianturuinsurvey").requires(s->s.hasPermission(2)).executes(RuinNaturalSurvey::start)
            .then(Commands.argument("structure",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(RuinNaturalSurvey::start)));
    }
    private static int start(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c){
        if(source!=null)return 0;var s=c.getSource();
        if(!s.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||s.getLevel().getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource)return 0;
        String selected="";try{selected=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"structure");}catch(IllegalArgumentException ignored){}final String filter=selected;maximum=filter.isEmpty()?128:1024;
        ids=new ArrayList<>();s.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).entrySet().forEach(v->{if(v.getValue() instanceof RuinStructure&&(filter.isEmpty()||v.getKey().location().getPath().equals(filter)))ids.add(v.getKey().location().getPath());});if(ids.isEmpty())return 0;Collections.sort(ids);source=s;index=attempt=0;results=new JsonArray();s.sendSuccess(()->net.minecraft.network.chat.Component.literal("遗迹选址检查开始，每tick一个候选，不放置方块"),false);return 1;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||source==null)return;
        try{
            var l=source.getLevel();var cs=l.getChunkSource();String id=ids.get(index);var structure=l.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("xiantu",id));
            var sets=l.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);RandomSpreadStructurePlacement placement=null;
            for(var set:sets)if(set.structures().stream().anyMatch(v->v.structure().value()==structure)){placement=(RandomSpreadStructurePlacement)set.placement();break;}
            if(placement==null)throw new IllegalStateException("No set "+id);
            int cx=Math.floorDiv((int)source.getPosition().x,16*placement.spacing()),cz=Math.floorDiv((int)source.getPosition().z,16*placement.spacing());
            var candidate=placement.getPotentialStructureChunk(l.getSeed(),(cx+attempt%32-16)*placement.spacing(),(cz+attempt/32-16)*placement.spacing());attempt++;
            boolean eligible=placement.isStructureChunk(cs.getGeneratorState(),candidate.x,candidate.z);
            var start=eligible?structure.generate(l.registryAccess(),cs.getGenerator(),cs.getGenerator().getBiomeSource(),cs.randomState(),l.getStructureManager(),l.getSeed(),candidate,0,l,structure.biomes()::contains):null;
            if(start!=null&&start.isValid()||attempt>=maximum){
                var r=new JsonObject();r.addProperty("id",id);r.addProperty("attempts",attempt);r.addProperty("found",start!=null&&start.isValid());
                if(start!=null&&start.isValid()){r.addProperty("chunkX",candidate.x);r.addProperty("chunkZ",candidate.z);var b=start.getBoundingBox();r.add("bounds",new Gson().toJsonTree(List.of(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ())));}
                results.add(r);index++;attempt=0;
                var path=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/ruin-natural-survey.json");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(results));
                if(index>=ids.size()){source.sendSuccess(()->net.minecraft.network.chat.Component.literal("遗迹选址检查完成："+results.size()+" 类，详见测试报告"),false);source=null;}
            }
        }catch(Exception ex){org.slf4j.LoggerFactory.getLogger(RuinNaturalSurvey.class).error("Survey failed",ex);source.sendFailure(net.minecraft.network.chat.Component.literal(ex.toString()));source=null;}
    }
}
