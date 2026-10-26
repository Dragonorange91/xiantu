package com.longju.xiantu.client;

import com.google.gson.JsonParser;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.io.InputStreamReader;
import java.util.*;

/** Disk/zip and gzip work runs on the reload worker; native config/texture setup
 * completes on the render thread while the loading overlay is still visible. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class BeastPhotonPreload {
    private record Prepared(Map<ResourceLocation,CompoundTag> effects,List<ResourceLocation> textures) {}
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
        if(!ModList.get().isLoaded("photon"))return;
        event.registerReloadListener(new SimplePreparableReloadListener<Prepared>() {
            @Override protected Prepared prepare(ResourceManager resources,ProfilerFiller profiler) {
                Map<ResourceLocation,CompoundTag> effects=new LinkedHashMap<>();List<ResourceLocation> textures=new ArrayList<>();
                try(var in=resources.open(new ResourceLocation("xiantu","vfx_v10_preload.json"));var reader=new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)) {
                    var manifest=JsonParser.parseReader(reader).getAsJsonObject();
                    for(var item:manifest.getAsJsonArray("effects")) {
                        ResourceLocation id=new ResourceLocation(item.getAsString());
                        try(var stream=resources.open(new ResourceLocation(id.getNamespace(),"fx/"+id.getPath()+".fx"))) {
                            effects.put(id,NbtIo.readCompressed(stream));
                        }
                    }
                    for(var item:manifest.getAsJsonArray("textures"))textures.add(new ResourceLocation(item.getAsString()));
                }catch(Exception ex){throw new IllegalStateException("Cannot preload Xiantu Photon resources",ex);}
                return new Prepared(effects,textures);
            }
            @Override protected void apply(Prepared prepared,ResourceManager resources,ProfilerFiller profiler) {
                FXHelper.clearCache();Map<ResourceLocation,FX> loaded=new HashMap<>();
                prepared.effects.forEach((id,tag)->{
                    if(tag.getInt("_version")!=1 || !tag.contains("fx",10))
                        throw new IllegalStateException("Unsupported native Photon resource "+id);
                    FX fx=new FX();fx.deserializeNBT(tag.getCompound("fx"));fx.setFxLocation(id);
                    if(fx.getMainFX().objects().isEmpty())
                        throw new IllegalStateException("Empty native Photon graph "+id);
                    loaded.put(id,fx);
                });
                for(var texture:prepared.textures)Minecraft.getInstance().getTextureManager().getTexture(texture);
                SkillPresentationClient.clear();BeastPhotonRuntime.install(loaded);BossPhotonTimeline.clear();
                LogUtils.getLogger().info("[Xiantu Photon] Prepared {} shared textures",prepared.textures.size());
            }
        });
    }
    private BeastPhotonPreload() {}
}
