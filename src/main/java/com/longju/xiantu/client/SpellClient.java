package com.longju.xiantu.client;
import com.longju.xiantu.spell.*;
import com.longju.xiantu.network.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class SpellClient {
 public static final SpellData DATA=new SpellData();public static long serverTime,receivedAt;public static String casting="";
 public static final KeyMapping OPEN=new KeyMapping("key.xiantu.spells",GLFW.GLFW_KEY_K,"key.categories.xiantu");
 public static final KeyMapping[] KEYS={key(0,GLFW.GLFW_KEY_Z),key(1,GLFW.GLFW_KEY_X),key(2,GLFW.GLFW_KEY_C),key(3,GLFW.GLFW_KEY_V)};
 private static final boolean[] DOWN=new boolean[4];
 private static final java.util.UUID[] GESTURES=new java.util.UUID[4];
 private static int inputTicks;
 private static KeyMapping key(int slot,int code){return new KeyMapping("key.xiantu.spell_"+slot,code,"key.categories.xiantu");}
 public static void update(SpellStatePacket p){DATA.load(p.data()==null?new net.minecraft.nbt.CompoundTag():p.data());serverTime=p.time();receivedAt=System.nanoTime();casting=p.casting();}
 public static long now(){return serverTime+Math.max(0,(System.nanoTime()-receivedAt)/50000000);}
 public static String reason(String id){var state=DATA.save();String why=state.getCompound("Eligibility").getString(id);if(!why.isEmpty())return why;return state.getDouble("PowerMilli")+1e-6<state.getCompound("Costs").getLong(id)?"灵力不足":"当前可用";}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;inputTicks++;
  if(mc.screen!=null){while(OPEN.consumeClick()){}for(int i=0;i<4;i++){while(KEYS[i].consumeClick()){}if(GESTURES[i]!=null)XiantuNetwork.CHANNEL.sendToServer(new GreatFireballInputPacket(i,GESTURES[i],3));GESTURES[i]=null;DOWN[i]=KEYS[i].isDown();}return;}
  if(OPEN.consumeClick())XiantuNetwork.CHANNEL.sendToServer(new SpellIntentPacket(-1));
  for(int i=0;i<4;i++){
   boolean down=KEYS[i].isDown(),clicked=KEYS[i].consumeClick(),charged=DATA.slot(i).equals("great_fireball");
   if(charged){if((down&&!DOWN[i]||clicked)&&GESTURES[i]==null){GESTURES[i]=java.util.UUID.randomUUID();XiantuNetwork.CHANNEL.sendToServer(new GreatFireballInputPacket(i,GESTURES[i],0));}
    if(GESTURES[i]!=null){if(!down){XiantuNetwork.CHANNEL.sendToServer(new GreatFireballInputPacket(i,GESTURES[i],2));GESTURES[i]=null;}else if(inputTicks%5==0)XiantuNetwork.CHANNEL.sendToServer(new GreatFireballInputPacket(i,GESTURES[i],1));}
   }else{if(GESTURES[i]!=null){XiantuNetwork.CHANNEL.sendToServer(new GreatFireballInputPacket(i,GESTURES[i],3));GESTURES[i]=null;}if(clicked)XiantuNetwork.CHANNEL.sendToServer(new SpellIntentPacket(i));}
   DOWN[i]=down;
  }
 }
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){DATA.load(new net.minecraft.nbt.CompoundTag());casting="";serverTime=0;java.util.Arrays.fill(DOWN,false);java.util.Arrays.fill(GESTURES,null);GreatFireballRenderer.clear();}
 @SubscribeEvent public static void inventory(ScreenEvent.Init.Post e){if(e.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen s)e.addListener(new Button(s.getGuiLeft()+79,s.getGuiTop()+48,16,18,Component.literal("术"),b->XiantuNetwork.CHANNEL.sendToServer(new SpellIntentPacket(-1)),supplier->supplier.get()){@Override public void renderWidget(net.minecraft.client.gui.GuiGraphics g,int mx,int my,float dt){setX(s.getGuiLeft()+79);setY(s.getGuiTop()+48);super.renderWidget(g,mx,my,dt);}});}
 @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
  var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.options.hideGui||mc.player.isSpectator()||!DATA.hudVisible())return;
  var g=e.getGuiGraphics();float scale=Math.min(HudLayout.scale(mc.getWindow()),(mc.getWindow().getGuiScaledWidth()-8)/180F);
  // Frame bottoms follow the hotbar item bottoms; icons identify spells without labels.
  g.pose().pushPose();g.pose().translate(mc.getWindow().getGuiScaledWidth()-2-172*scale,mc.getWindow().getGuiScaledHeight()-2-40*scale,0);g.pose().scale(scale,scale,1);
  var state=DATA.save();
  for(int i=0;i<4;i++){int x=i*44;var s=SpellCatalog.find(DATA.slot(i));var tex=new net.minecraft.resources.ResourceLocation("xiantu","textures/gui/hud/spell_slot_"+i+".png");
   g.blit(tex,x,0,40,40,0,0,64,64,64,64);
   if(s!=null)SpellIcons.draw(g,s.id(),x+8,4,24);
   long cd=s==null?0:DATA.cooldown(s.id(),now());boolean low=s!=null&&state.getDouble("PowerMilli")+1e-6<state.getCompound("Costs").getLong(s.id());
   if(cd>0){int h=(int)(24*Math.min(1,cd/(double)Math.max(1,s.cooldown())));g.fill(x+6,5,x+34,5+h,0xa0101820);}
   String status=cd>0?(cd+19)/20+"s":low?"缺灵力":KEYS[i].getTranslatedKeyMessage().getString();
   g.drawCenteredString(mc.font,status,x+20,29,low?0xffff8877:0xfff2e3b9);
  }
  StringBuilder passive=new StringBuilder();for(int i=4;i<SpellData.TOTAL_SLOTS;i++){var s=SpellCatalog.find(DATA.slot(i));if(s!=null){if(passive.length()>0)passive.append(" · ");passive.append(s.name());}}
  if(passive.length()>0)g.drawString(mc.font,mc.font.plainSubstrByWidth("被动："+passive,172),0,-24,0xffc5dbe0,false);
  if(!casting.isEmpty()){var s=SpellCatalog.find(casting);if(s!=null){
   if(casting.equals("great_fireball")){double charge=GreatFireballRenderer.chargeProgress();g.fill(0,-39,172,-35,0x99332618);g.fill(0,-39,(int)(172*charge),-35,0xffffa32b);g.drawString(mc.font,"蓄力 "+(int)(charge*100)+"% · 松开发射",0,-51,0xffffdb80,false);}
   else g.drawString(mc.font,"凝神："+s.name(),0,-36,0xffffdb80,false);
  }}
  g.pose().popPose();
 }
 private SpellClient(){}
}
