package com.longju.xiantu.smelting;

import com.longju.xiantu.client.WorkstationGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SmeltingScreen extends com.longju.xiantu.client.FittedContainerScreen<SmeltingMenu> {
    private Button start,cancel;
    public SmeltingScreen(SmeltingMenu m,Inventory i,Component t){super(m,i,t);imageWidth=240;imageHeight=234;inventoryLabelX=40;inventoryLabelY=138;titleLabelX=12;titleLabelY=8;}
    @Override protected void init(){super.init();start=addRenderableWidget(Button.builder(Component.literal("开始熔炼"),b->send(0)).bounds(leftPos+92,topPos+110,64,20).build());cancel=addRenderableWidget(Button.builder(Component.literal("取消"),b->send(1)).bounds(leftPos+164,topPos+110,44,20).build());}
    private void send(int action){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    @Override protected void containerTick(){super.containerTick();start.active=menu.canStart();cancel.active=menu.canCancel();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int x=leftPos,y=topPos;com.longju.xiantu.client.WorkstationGui.frame(g,x,y,imageWidth,imageHeight,false);WorkstationGui.panel(g,x+8,y+26,224,108);
        for(var slot:menu.slots)WorkstationGui.slot(g,x,y,slot);
        g.drawString(font,"原矿",x+40,y+33,WorkstationGui.TEXT,false);g.drawString(font,"成品",x+176,y+33,WorkstationGui.TEXT,false);
        g.drawString(font,"完整灵石",x+25,y+81,WorkstationGui.MUTED,false);WorkstationGui.arrow(g,x+76,y+55,85);
        WorkstationGui.progress(g,x+76,y+71,85,menu.elapsed(),menu.total());
        String time=menu.busy()?String.format(java.util.Locale.ROOT,"剩余 %.1f 秒",(menu.total()-menu.elapsed())/20.0):"待机";
        g.drawCenteredString(font,time,x+120,y+82,WorkstationGui.MUTED);
        g.drawString(font,"储能 "+menu.energy()+" / 1000000",x+76,y+97,WorkstationGui.TEXT,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,titleLabelX,titleLabelY,WorkstationGui.TEXT,false);g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,WorkstationGui.MUTED,false);}
    @Override protected void renderFitted(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.renderFitted(g,mx,my,partial);renderTooltip(g,mx,my);}
}
