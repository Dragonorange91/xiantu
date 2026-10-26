package com.longju.xiantu.qa;

import net.minecraft.client.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Integrated-client QA: holds the real use binding; normal client packets start/release it. */
@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class SkillNativeUseAudit {
    private static int remaining,delay;private static boolean sneaking,oldShift;
    public static void click(boolean sneak){Minecraft.getInstance().execute(()->{var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||!server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017")||mc.player==null||mc.screen!=null)return;oldShift=mc.options.keyShift.isDown();sneaking=true;mc.options.keyShift.setDown(sneak);delay=3;});}
    public static void queue(int ticks){Minecraft.getInstance().execute(()->{
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(server==null||!server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017")||mc.player==null||mc.screen!=null)return;
        remaining=ticks;mc.options.keyUse.setDown(true);KeyMapping.click(mc.options.keyUse.getKey());
    });}
    public static void cancel(){Minecraft.getInstance().execute(()->{remaining=0;delay=0;var mc=Minecraft.getInstance();mc.options.keyUse.setDown(false);if(sneaking)mc.options.keyShift.setDown(oldShift);sneaking=false;});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(delay>0){if(--delay==0){remaining=2;mc.options.keyUse.setDown(true);KeyMapping.click(mc.options.keyUse.getKey());}return;}
        if(remaining==0)return;
        if(mc.player==null||mc.screen!=null||--remaining==0){remaining=0;mc.options.keyUse.setDown(false);if(sneaking)mc.options.keyShift.setDown(oldShift);sneaking=false;}
    }
    private SkillNativeUseAudit(){}
}
