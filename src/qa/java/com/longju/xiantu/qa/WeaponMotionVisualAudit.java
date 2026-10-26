package com.longju.xiantu.qa;

import com.longju.xiantu.client.equipment.WeaponAttackAnimations;
import com.longju.xiantu.client.equipment.WeaponAttackMotion;
import net.minecraft.client.*;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** QA only: queues the actual attack key, never overrides swing progress or render poses. */
@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class WeaponMotionVisualAudit {
    private static int remaining,ticks,interval=24;
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantuweaponmotionqa").executes(c->{
            var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
            if(server==null||!server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||mc.player==null||WeaponAttackAnimations.kind(mc.player.getMainHandItem())==WeaponAttackMotion.Kind.NONE)return 0;
            remaining=3;ticks=0;interval=24;return 1;
        }).then(Commands.literal("rapid").executes(c->{
            var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
            if(server==null||!server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||mc.player==null)return 0;
            remaining=16;ticks=0;interval=4;return 1;
        })));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||remaining==0)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=null||mc.player.isUsingItem()){remaining=0;return;}
        if(++ticks>=12&&(ticks-12)%interval==0){KeyMapping.click(mc.options.keyAttack.getKey());remaining--;}
    }
    private WeaponMotionVisualAudit(){}
}
