package com.longju.xiantu.talisman;
import com.longju.xiantu.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;
/** Catalog icons, exact ingredients and a normalized stroke canvas. */
public final class TalismanScreen extends com.longju.xiantu.client.FittedContainerScreen<TalismanMenu> {
 private int page;private net.minecraft.client.gui.components.EditBox search;private int category;private static final String[] CATEGORIES={"全部","成品符箓","材料加工"};private boolean dragging;private final List<TalismanDrawing.Point> pending=new ArrayList<>();private long sent;private int observedStroke=-1;
 public TalismanScreen(TalismanMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=390;imageHeight=336;inventoryLabelY=237;inventoryLabelX=108;}
 private void button(int x,int y,int w,String text,int action){addRenderableWidget(Button.builder(Component.literal(text),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}).bounds(leftPos+x,topPos+y,w,18).build());}
 @Override protected void init(){super.init();button(12,210,48,"上一页",-1);button(70,210,48,"下一页",-2);button(138,210,58,"自动制符",0);button(200,210,58,"亲手绘制",1);button(264,210,46,"取消",2);button(314,210,58,"收笔",3);button(12,232,90,"切换供能",4);button(282,232,90,"登记归途",5);
  search=new EditBox(font,leftPos+12,topPos+25,65,14,Component.literal("搜索"));search.setMaxLength(24);search.setHint(Component.literal("搜索"));search.setResponder(value->page=0);addRenderableWidget(search);
  addRenderableWidget(Button.builder(Component.literal(CATEGORIES[category]),b->{category=(category+1)%3;page=0;b.setMessage(Component.literal(CATEGORIES[category]));}).bounds(leftPos+80,topPos+25,42,14).build());
  // Page changes are local; all recipe selections are still validated on the server.
  var widgets=children();for(var widget:widgets)if(widget instanceof Button b){if(b.getMessage().getString().equals("上一页"))b.active=true;}
 }
 @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
  com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,false);
  com.longju.xiantu.client.WorkstationGui.panel(g,leftPos+7,topPos+40,115,163);
  com.longju.xiantu.client.WorkstationGui.frame(g,leftPos+126,topPos+41,136,136,true);
  com.longju.xiantu.client.WorkstationGui.panel(g,leftPos+270,topPos+40,112,163);
  for(var slot:menu.slots)com.longju.xiantu.client.WorkstationGui.slot(g,leftPos,topPos,slot);
  g.fill(leftPos+128,topPos+43,leftPos+260,topPos+175,0xffded0a2);g.fill(leftPos+130,topPos+45,leftPos+258,topPos+173,0xfff1e4ba);
  for(int i=0;i<24;i++){var list=visible();int item=page*24+i;if(item>=list.size())break;int n=list.get(item);var r=TalismanRecipes.ALL.get(n);int x=leftPos+12+i%4*26,y=topPos+43+i/4*26;g.fill(x-1,y-1,x+19,y+19,n==menu.selected()?0xffbd9345:0xff263e45);g.renderItem(r.output(1),x,y);if(!menu.learned(r))g.fill(x,y,x+16,y+16,0x99402020);}
  for(var s:menu.slots){if(s.container==menu.materials)g.fill(leftPos+s.x-1,topPos+s.y-1,leftPos+s.x+17,topPos+s.y+17,0xffa18962);}
  var r=menu.recipe();if(r.kind().equals("symbol")){var d=TalismanCatalog.find(r.id());for(int i=0;i<d.strokes();i++){var points=TalismanDrawing.template(r.id(),i);int color=i<menu.stroke()?0xffbc2118:i==menu.stroke()?0xffdc3925:0xffbaac83;for(int k=1;k<points.size();k++)line(g,points.get(k-1),points.get(k),color);}if(observedStroke!=menu.stroke()){observedStroke=menu.stroke();pending.clear();dragging=false;}}
  int bar=menu.total()==0?0:128*menu.elapsed()/menu.total();g.fill(leftPos+130,topPos+182,leftPos+258,topPos+187,0xff26353c);g.fill(leftPos+130,topPos+182,leftPos+130+bar,topPos+187,0xffe9ba65);
 }
 private void line(GuiGraphics g,TalismanDrawing.Point a,TalismanDrawing.Point b,int color){int n=Math.max(1,(int)(Math.hypot(a.x()-b.x(),a.y()-b.y())*180));for(int i=0;i<=n;i++){int x=leftPos+130+(int)((a.x()+(b.x()-a.x())*i/n)*128),y=topPos+45+(int)((a.y()+(b.y()-a.y())*i/n)*128);g.fill(x,y,x+1,y+1,color);}}
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,12,10,0xfff0deb0,false);var r=menu.recipe();String name=TalismanRecipes.name(r);g.drawCenteredString(font,font.plainSubstrByWidth(name,140),195,28,0xffffdf9c);g.drawString(font,"符笔",278,55,0xffe6d5b2,false);g.drawString(font,"灵石",342,55,0xffe6d5b2,false);g.drawString(font,"材料",280,98,0xffe6d5b2,false);g.drawString(font,"成品",322,157,0xffe6d5b2,false);g.drawString(font,"灵力 "+menu.energy()+" / 300000",130,193,0xffe6d5b2,false);g.drawString(font,new String[]{"灵力池","个人灵力","混合供能"}[menu.mode()],12,253,0xfff0deb0,false);if(menu.busy()&&menu.manual())g.drawString(font,menu.elapsed()<menu.total()*15/100?"铺纸蘸砂":menu.elapsed()<menu.total()*85/100?"第 "+(menu.stroke()+1)+" 笔 · 失误 "+menu.mistakes():menu.sealed()?"封符中":"点击收笔",130,233,0xfff0deb0,false);}
 @Override protected void renderFitted(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.renderFitted(g,mx,my,partial);renderTooltip(g,mx,my);for(int i=0;i<24;i++){var list=visible();int item=page*24+i,x=leftPos+12+i%4*26,y=topPos+43+i/4*26;if(item<list.size()&&mx>=x&&mx<x+18&&my>=y&&my<y+18){var r=TalismanRecipes.ALL.get(list.get(item));var lines=new ArrayList<Component>();lines.add(Component.literal(TalismanRecipes.name(r)));lines.add(Component.literal("耗能 "+r.energy()+"；用时 "+r.ticks()/20+"秒"));if(!menu.learned(r))lines.add(Component.literal("尚未学习"+TalismanBlueprints.name(r.knowledge())));if(r.kind().equals("symbol"))lines.add(Component.literal("同阶符纸 ×1"));for(var in:r.ingredients())lines.add(Component.literal(in.example().getHoverName().getString()+" ×"+in.count()));g.renderComponentTooltip(font,lines,mx,my);break;}}}
 @Override protected boolean clickFitted(double x,double y,int button){if(button==0){int localX=(int)x-leftPos,localY=(int)y-topPos;if(localY>=210&&localY<228&&localX>=12&&localX<118){page=Math.max(0,Math.min(Math.max(0,(visible().size()-1)/24),page+(localX<62?-1:1)));return true;}if(!menu.busy()&&localX>=12&&localX<116&&localY>=43&&localY<199){int index=page*24+(localY-43)/26*4+(localX-12)/26;var list=visible();if(index<list.size()&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+list.get(index));return true;}if(canvas(x,y)&&menu.busy()&&menu.manual()&&menu.owns()){dragging=true;point(x,y,false);return true;}}return super.clickFitted(x,y,button);}
 private List<Integer> visible(){var out=new ArrayList<Integer>();String query=search==null?"":search.getValue().strip();for(int i=0;i<TalismanRecipes.ALL.size();i++){var r=TalismanRecipes.ALL.get(i);if(category==1&&!r.kind().equals("symbol")||category==2&&r.kind().equals("symbol"))continue;if(!query.isEmpty()&&!TalismanRecipes.name(r).contains(query))continue;out.add(i);}page=Math.max(0,Math.min(page,Math.max(0,(out.size()-1)/24)));return out;}
 @Override protected boolean scrollFitted(double x,double y,double delta){if(x>=leftPos+8&&x<leftPos+124&&y>=topPos+40&&y<topPos+205){page=Math.max(0,Math.min(Math.max(0,(visible().size()-1)/24),page+(delta<0?1:-1)));return true;}return super.scrollFitted(x,y,delta);}
 @Override public boolean keyPressed(int key,int scan,int mods){if(search!=null&&search.isFocused()&&key!=256)return search.keyPressed(key,scan,mods)||key!=256;return super.keyPressed(key,scan,mods);}
 private boolean canvas(double x,double y){return x>=leftPos+130&&x<=leftPos+258&&y>=topPos+45&&y<=topPos+173;}
 private void point(double x,double y,boolean end){pending.add(new TalismanDrawing.Point(Math.max(0,Math.min(1,(x-leftPos-130)/128)),Math.max(0,Math.min(1,(y-topPos-45)/128))));long now=net.minecraft.Util.getMillis();if(end||pending.size()>=24||now-sent>=80){XiantuNetwork.CHANNEL.sendToServer(new TalismanStrokePacket(menu.containerId,menu.nonce(),menu.stroke(),List.copyOf(pending),end));pending.clear();sent=now;}}
 @Override protected boolean dragFitted(double x,double y,int button,double dx,double dy){if(dragging&&button==0){point(x,y,false);return true;}return super.dragFitted(x,y,button,dx,dy);}
 @Override protected boolean releaseFitted(double x,double y,int button){if(dragging&&button==0){point(x,y,true);dragging=false;return true;}return super.releaseFitted(x,y,button);}
}
