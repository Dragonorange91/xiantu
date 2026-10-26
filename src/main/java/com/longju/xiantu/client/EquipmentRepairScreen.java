package com.longju.xiantu.client;
import com.longju.xiantu.refining.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
public final class EquipmentRepairScreen extends com.longju.xiantu.client.FittedContainerScreen<EquipmentRepairMenu>{
 private Button repair;
 public EquipmentRepairScreen(EquipmentRepairMenu m,Inventory i,Component t){super(m,i,t);imageWidth=340;imageHeight=258;inventoryLabelX=89;inventoryLabelY=158;}
 @Override protected void init(){super.init();repair=addRenderableWidget(Button.builder(Component.literal("修复 / 合并"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+104,topPos+112,132,24).build());}
 @Override protected void containerTick(){super.containerTick();repair.active=EquipmentRepair.preview(menu.inputs.getItem(0),menu.inputs.getItem(1)).error().isEmpty();}
 @Override protected void renderBg(GuiGraphics g,float d,int x,int y){WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,false);for(var s:menu.slots)WorkstationGui.slot(g,leftPos,topPos,s);g.fill(leftPos+225,topPos+55,leftPos+247,topPos+77,0xffccad68);g.fill(leftPos+226,topPos+56,leftPos+246,topPos+76,0xff122c2d);var r=EquipmentRepair.preview(menu.inputs.getItem(0),menu.inputs.getItem(1));if(!r.stack().isEmpty())g.renderItem(r.stack(),leftPos+228,topPos+58);}
 @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawCenteredString(font,title,imageWidth/2,12,0xfff0e1bb);g.drawCenteredString(font,Component.literal("装备"),94,39,0xffe2d4b0);g.drawCenteredString(font,Component.literal("同阶主材 / 同款装备"),160,88,0xffe2d4b0);g.drawString(font,"+",122,61,0xffe2d4b0,false);g.drawString(font,"→",196,61,0xffe2d4b0,false);g.drawCenteredString(font,Component.literal("修复预览"),236,39,0xffe2d4b0);var r=EquipmentRepair.preview(menu.inputs.getItem(0),menu.inputs.getItem(1));g.drawCenteredString(font,Component.literal(r.error().isEmpty()?"消耗 "+r.cost()/1000.0+" 灵力":r.error()),170,143,0xffe2d4b0);g.drawString(font,playerInventoryTitle,89,158,0xffe2d4b0,false);}
 @Override protected void renderFitted(GuiGraphics g,int x,int y,float d){renderBackground(g);super.renderFitted(g,x,y,d);renderTooltip(g,x,y);if(x>=leftPos+228&&x<leftPos+244&&y>=topPos+58&&y<topPos+74){var r=EquipmentRepair.preview(menu.inputs.getItem(0),menu.inputs.getItem(1));if(!r.stack().isEmpty())g.renderTooltip(font,r.stack(),x,y);}}
}
