package com.longju.xiantu.material;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class AppraisalScreen extends com.longju.xiantu.client.FittedContainerScreen<AppraisalMenu>{
    private Button identify;
    public AppraisalScreen(AppraisalMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=176;imageHeight=198;inventoryLabelY=104;}
    @Override protected void init(){super.init();identify=addRenderableWidget(Button.builder(Component.literal("鉴定"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+68,topPos+53,40,20).build());}
    @Override public void containerTick(){super.containerTick();identify.active=menu.canIdentify();}
    @Override protected void renderBg(GuiGraphics g,float tick,int x,int y){
        com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,false);
        for(var slot:menu.slots)com.longju.xiantu.client.WorkstationGui.slot(g,leftPos,topPos,slot);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,8,8,0xfff7e9c6,false);g.drawString(font,"材料",46,31,0xfff7e9c6,false);
        g.drawString(font,"凡  下  中  上  极",43,99,0xfff7e9c6,false);g.drawString(font,"背包",8,105,0xfff7e9c6,false);
        if(menu.materials.getItem(0).isEmpty())g.drawString(font,"放入未鉴定材料",8,19,0xffe3e5de,false);
    }
    @Override protected void renderFitted(GuiGraphics g,int x,int y,float tick){renderBackground(g);super.renderFitted(g,x,y,tick);renderTooltip(g,x,y);}
}
