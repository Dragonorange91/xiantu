package com.longju.xiantu.client;

import com.longju.xiantu.trade.TownMerchantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lytharalab.gfbs.gltf.api.client.*;
import java.util.*;

/** One fixed-pose instance per visible merchant; immutable GLB assets are shared by GFBS. */
public final class TownMerchantRenderer extends EntityRenderer<TownMerchantEntity> {
    private static final Map<UUID,Entry> INSTANCES=new HashMap<>();
    private static long generation;
    private final ResourceLocation model;
    public TownMerchantRenderer(EntityRendererProvider.Context context,String name) {super(context);model=new ResourceLocation("xiantu","models/entity/merchants/"+name+".glb");shadowRadius=.35F;}
    @Override public ResourceLocation getTextureLocation(TownMerchantEntity merchant){return model;}
    @Override public void render(TownMerchantEntity entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        Entry entry=INSTANCES.computeIfAbsent(entity.getUUID(),id->new Entry());
        if(entry.instance==null&&!entry.requested){
            entry.requested=true;long requestedGeneration=generation;
            ClientGltfApi.models().load(model).whenComplete((asset,error)->Minecraft.getInstance().execute(()->{
                if(generation!=requestedGeneration||INSTANCES.get(entity.getUUID())!=entry||entity.isRemoved())return;
                if(error!=null){com.mojang.logging.LogUtils.getLogger().error("Merchant model load failed: {}",model,error);return;}
                entry.instance=new GltfInstance(asset);entry.instance.renderOptions().frustumCulling(false);
            }));
        }
        if(entry.instance!=null){poses.pushPose();try{
            // Authored face is -Z; Minecraft yaw 0 faces +Z.
            poses.mulPose(Axis.YP.rotationDegrees(180-yaw));
            GltfRenderer.render(entry.instance,poses,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        }finally{poses.popPose();}}
        super.render(entity,yaw,partial,poses,buffers,light);
    }
    private static void close(UUID id){var entry=INSTANCES.remove(id);if(entry!=null&&entry.instance!=null)entry.instance.close();}
    private static void clear(){generation++;for(var e:INSTANCES.values())if(e.instance!=null)e.instance.close();INSTANCES.clear();}
    public static SimplePreparableReloadListener<Void> reloadListener(){return new SimplePreparableReloadListener<>(){
        @Override protected Void prepare(ResourceManager r,ProfilerFiller p){return null;}
        @Override protected void apply(Void v,ResourceManager r,ProfilerFiller p){clear();}
    };}
    @Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
    public static final class Lifecycle {
        @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){if(event.getLevel().isClientSide()&&event.getEntity() instanceof TownMerchantEntity)Minecraft.getInstance().execute(()->close(event.getEntity().getUUID()));}
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    }
    private static final class Entry{GltfInstance instance;boolean requested;}
}
