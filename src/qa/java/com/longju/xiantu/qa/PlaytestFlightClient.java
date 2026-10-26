package com.longju.xiantu.qa;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Nonblocking QA key-state driver; production input packets and flight prediction stay active. */
@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class PlaytestFlightClient {
 private static boolean active,up,sprint;private static float yaw,pitch;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();if(mc.player==null){active=false;return;}
  if(PlaytestAudit.steer&&mc.getSingleplayerServer()!=null){if(!active){up=mc.options.keyUp.isDown();sprint=mc.options.keySprint.isDown();yaw=mc.player.getYRot();pitch=mc.player.getXRot();active=true;}mc.options.keyUp.setDown(true);mc.options.keySprint.setDown(PlaytestAudit.boost);mc.player.setYRot(PlaytestAudit.heading);mc.player.setXRot(-3);}
  else if(active){mc.options.keyUp.setDown(up);mc.options.keySprint.setDown(sprint);mc.player.setYRot(yaw);mc.player.setXRot(pitch);active=false;}
 }
 private PlaytestFlightClient(){}
}
