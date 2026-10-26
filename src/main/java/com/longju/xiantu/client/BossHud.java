package com.longju.xiantu.client;
import com.longju.xiantu.network.BossHudPacket;
import com.longju.xiantu.cultivation.RealmRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class BossHud {
 private static BossHudPacket state;private static long received;
 private static final Map<String,Integer> COLORS=Map.of("spirit_ape_king",0xffe6b75d,"meteor_general",0xff91d0ff,"five_element_warden",0xff7bd5a6,"ender_dragon",0xffc47ff0,"wither",0xffd0c7da,"warden",0xff58ddd6,"elder_guardian",0xff78e2c9);
 public static void receive(BossHudPacket p){state=p;received=net.minecraft.Util.getMillis();SMOOTH.keySet().removeIf(id->p.entries().stream().noneMatch(x->x.event().equals(id)));}
 private static boolean valid(){var mc=Minecraft.getInstance();return state!=null&&mc.level!=null&&mc.player!=null&&mc.level.dimension().location().equals(state.dimension())&&net.minecraft.Util.getMillis()-received<2500;}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){state=null;SMOOTH.clear();}
 @SubscribeEvent public static void nativeBar(CustomizeGuiOverlayEvent.BossEventProgress e){
  if(!valid()||com.longju.xiantu.config.XiantuClientConfig.BOSS_STYLE.get()!=com.longju.xiantu.config.XiantuClientConfig.BossStyle.ORNATE)return;
  var b=state.entries().stream().filter(x->x.event().equals(e.getBossEvent().getId())).findFirst().orElse(null);if(b==null||!COLORS.containsKey(b.skin())||!Float.isFinite(b.maximum())||b.maximum()<=0)return;
  var mc=Minecraft.getInstance();var tex=new ResourceLocation("xiantu","textures/gui/boss/"+b.skin()+".png");if(mc.getResourceManager().getResource(tex).isEmpty())return;
  e.setCanceled(true);var g=e.getGuiGraphics();var window=mc.getWindow();
  double guiScale=window.getGuiScale();
  // Draw in physical pixels so auto GUI scale changes cannot inflate this composition.
  int panelRight=(int)Math.ceil(4*guiScale+210*HudLayout.scale(window)*guiScale);
  int width=HudLayout.bossWidth(window.getWidth(),window.getHeight(),panelRight),x=(window.getWidth()-width)/2;
  float ui=Math.max(.85F,width/560F);
  int y=Math.max(6,8+(int)((e.getY()-12)*guiScale)),h=Math.max(44,width/8);
  g.pose().pushPose();g.pose().scale((float)(1/guiScale),(float)(1/guiScale),1);
  float ratio=Math.max(0,Math.min(1,b.health()/b.maximum())),old=SMOOTH.getOrDefault(b.event(),ratio);float shown=com.longju.xiantu.config.XiantuClientConfig.BOSS_ANIMATION.get()?old+(ratio-old)*.22f:ratio;SMOOTH.put(b.event(),shown);
  g.blit(tex,x,y,width,h,0,0,512,64,512,64);
  int[] cavity=CAVITIES.get(b.skin());int left=x+width*145/512,right=x+width*367/512;
  int minimum=(int)Math.ceil(12*ui);
  int top=y+h*cavity[0]/64,bottom=Math.min(y+h-1,Math.max(top+minimum,y+h*cavity[1]/64));
  top=Math.min(top,bottom-minimum);int filled=(int)((right-left)*shown);
  g.fill(left,top,right,bottom,0xff111a29);g.fill(left,top,left+filled,bottom,COLORS.get(b.skin()));g.fill(left,top,left+filled,top+1,0xfff8edd1);
  int phase=left+(right-left)*3/10;g.fill(phase,top-1,phase+1,bottom+1,0xfff8d39b);
  if(ratio<=.3){int glow=com.longju.xiantu.config.XiantuClientConfig.BOSS_ANIMATION.get()?110+(int)(35*Math.sin(net.minecraft.Util.getMillis()/300.)):135;g.fill(left-2,top-2,right+2,top-1,(glow<<24)|0xffca64);}
  String realm=com.longju.xiantu.entity.BeastRealmRules.valid(b.realm(),b.stage())?RealmRegistry.get(b.realm(),b.stage()).majorName()+RealmRegistry.get(b.realm(),b.stage()).minorName():"";
  var label=b.name().copy().append(" · "+realm);
  float textScale=Math.min(1.1F*ui,(right-left-8*ui)/(float)Math.max(1,mc.font.width(label)));
  g.pose().pushPose();g.pose().translate(x+width/2.,top+(bottom-top-mc.font.lineHeight*textScale)/2.,0);g.pose().scale(textScale,textScale,1);g.drawCenteredString(mc.font,label,0,0,0xfffff4db);g.pose().popPose();
  if(com.longju.xiantu.config.XiantuClientConfig.BOSS_NUMBERS.get()){String hp=String.format(java.util.Locale.ROOT,"%.0f / %.0f",Math.max(0,b.health()),b.maximum());g.pose().pushPose();g.pose().translate(x+width/2.,bottom+2*ui,0);g.pose().scale(.9f*ui,.9f*ui,1);g.drawCenteredString(mc.font,hp,0,0,0xfff4e7c9);g.pose().popPose();}
  g.pose().popPose();
  e.setIncrement((int)Math.ceil((h+18*ui)/guiScale));
 }
 private static final Map<UUID,Float> SMOOTH=new HashMap<>();
 private static final Map<String,int[]> CAVITIES=Map.of("spirit_ape_king",new int[]{46,55},"meteor_general",new int[]{35,46},"five_element_warden",new int[]{32,41},"ender_dragon",new int[]{31,42},"wither",new int[]{35,41},"warden",new int[]{34,44},"elder_guardian",new int[]{27,38});
 private BossHud(){}
}
