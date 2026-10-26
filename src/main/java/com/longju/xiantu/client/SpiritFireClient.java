package com.longju.xiantu.client;

import com.longju.xiantu.network.*;
import net.minecraft.client.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class SpiritFireClient {
    public static final KeyMapping CONDENSE=new KeyMapping("key.xiantu.condense_fire",GLFW.GLFW_KEY_J,"key.categories.xiantu");
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();boolean pressed=false;
        while(CONDENSE.consumeClick())pressed=true;
        if(pressed&&mc.player!=null&&mc.screen==null)XiantuNetwork.CHANNEL.sendToServer(new CondenseSpiritFirePacket());
    }
    private SpiritFireClient(){}
}
