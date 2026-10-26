package com.longju.xiantu.client;
import com.longju.xiantu.equipment.AccessoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class AccessoryScreen extends com.longju.xiantu.client.FittedContainerScreen<AccessoryMenu> {
    public AccessoryScreen(AccessoryMenu menu,Inventory inv,Component title){super(menu,inv,title);imageHeight=166;inventoryLabelY=72;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int x=leftPos,y=topPos;
        com.longju.xiantu.client.WorkstationGui.frame(g,x,y,imageWidth,imageHeight,true);
        com.longju.xiantu.client.WorkstationGui.panel(g,x+5,y+5,imageWidth-10,63);
        for(var s:menu.slots)com.longju.xiantu.client.WorkstationGui.slot(g,x,y,s);
        for(int i=0;i<3;i++)g.drawCenteredString(font,Component.translatable(i==2?"slot.xiantu.necklace":"slot.xiantu.ring"),x+52+i*36,y+25,0xfff0dfad);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,8,9,0xfff0dfad,false);g.drawString(font,playerInventoryTitle,8,72,0xff343c3b,false);}
    @Override protected void renderFitted(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.renderFitted(g,mx,my,partial);renderTooltip(g,mx,my);}
}
