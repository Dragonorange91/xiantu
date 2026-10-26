package com.longju.xiantu.refining;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class BlueprintLearningScreen extends com.longju.xiantu.client.FittedContainerScreen<BlueprintLearningMenu>{
    public BlueprintLearningScreen(BlueprintLearningMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=300;imageHeight=Math.max(125,82+m.details.size()*14);}
    @Override protected void init(){super.init();addRenderableWidget(Button.builder(Component.literal("确认学习"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+42,topPos+imageHeight-29,90,20).build());addRenderableWidget(Button.builder(Component.literal("取消"),b->onClose()).bounds(leftPos+168,topPos+imageHeight-29,90,20).build());}
    @Override protected void renderBg(GuiGraphics g,float dt,int mx,int my){com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,true);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawCenteredString(font,title,150,12,0xff352919);g.drawCenteredString(font,menu.name,150,34,0xff352919);for(int i=0;i<menu.details.size();i++)g.drawString(font,menu.details.get(i),19,54+i*14,0xff51432c,false);}
    @Override protected void renderFitted(GuiGraphics g,int x,int y,float dt){renderBackground(g);super.renderFitted(g,x,y,dt);}
}
