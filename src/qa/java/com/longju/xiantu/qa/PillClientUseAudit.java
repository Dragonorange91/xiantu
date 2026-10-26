package com.longju.xiantu.qa;
import com.longju.xiantu.alchemy.PillCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;

/** Exercises the native 32-tick client use pipeline, rather than invoking finishUsingItem directly. */
@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class PillClientUseAudit {
    private static int stage,ticks;
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantupilluseaudit").executes(c->{
            var mc=Minecraft.getInstance();
            if(stage!=0||mc.player==null)return 0;
            stage=1;ticks=0;mc.player.connection.sendCommand("xiantupillfixture prepare");return 1;
        }));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||stage==0)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null){mc.options.keyUse.setDown(false);stage=0;return;}
        ticks++;
        if(stage==1){
            if(ticks>100){stage=0;mc.player.connection.sendCommand("xiantupillfixture restore");throw new IllegalStateException("Pill use fixture did not synchronize");}
            var held=mc.player.getMainHandItem();
            if(ticks<10||mc.screen!=null||!held.is(PillCatalog.ITEMS.get("pill_05").get())||held.getCount()!=2)return;
            mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);stage=2;ticks=0;
        } else if(ticks>=40){
            mc.options.keyUse.setDown(false);stage=0;mc.player.connection.sendCommand("xiantupillfixture verify");
        }
    }
}
