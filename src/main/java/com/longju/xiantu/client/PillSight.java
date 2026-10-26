package com.longju.xiantu.client;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class PillSight {
    private static final Set<net.minecraft.world.entity.LivingEntity> REVEALED=new HashSet<>();
    public static boolean active(){return com.longju.xiantu.cultivation.CultivationCapability.clientData().medicine().state().getBoolean("client_true_sight");}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void pre(RenderLivingEvent.Pre<?,?> e){
        var p=Minecraft.getInstance().player;var entity=e.getEntity();
        var id=ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if(p!=null&&active()&&p.distanceToSqr(entity)<=576&&id!=null&&id.getNamespace().equals("xiantu")&&entity.isInvisible()){
            REVEALED.add(entity);entity.setInvisible(false);
        }
    }
    @SubscribeEvent public static void post(RenderLivingEvent.Post<?,?> e){if(REVEALED.remove(e.getEntity()))e.getEntity().setInvisible(true);}
    @SubscribeEvent public static void afterEntities(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        for(var entity:REVEALED)entity.setInvisible(true);
        REVEALED.clear();
    }
}
