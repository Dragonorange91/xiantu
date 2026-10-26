package com.longju.xiantu.worldgen;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="xiantu")
public final class RuinSpecs {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void reload(net.minecraftforge.event.AddReloadListenerEvent e){clear();}
    private static final Map<ResourceManager,Map<String,JsonObject>> CACHE=new WeakHashMap<>();
    public static synchronized JsonObject load(MinecraftServer server,String id){
        var manager=server.getResourceManager();var map=CACHE.computeIfAbsent(manager,k->new HashMap<>());
        return map.computeIfAbsent(id,key->{try(var r=manager.getResource(new ResourceLocation("xiantu","ruin_blueprints/"+key+".json")).orElseThrow().openAsReader()){return JsonParser.parseReader(r).getAsJsonObject();}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}});
    }
    public static synchronized void clear(){CACHE.clear();RuinSiteExclusions.clear();}
    public static BlockPos offset(BlockPos origin,JsonArray a){return origin.offset(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());}
    public static String waveKey(String root,String wave){return UUID.nameUUIDFromBytes((root+":"+wave).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();}
    private RuinSpecs(){}
}
