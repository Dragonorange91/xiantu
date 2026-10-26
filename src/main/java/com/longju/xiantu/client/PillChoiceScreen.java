package com.longju.xiantu.client;
import com.longju.xiantu.alchemy.PillChoiceMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
public final class PillChoiceScreen extends com.longju.xiantu.client.FittedContainerScreen<PillChoiceMenu>{
    private int page;
    public PillChoiceScreen(PillChoiceMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=330;imageHeight=240;}
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){super.init();rebuild();}
    private void rebuild(){
        clearWidgets();for(int row=0;row<7;row++){int index=page*7+row;if(index>=menu.labels.size())break;
            addRenderableWidget(Button.builder(Component.literal(menu.labels.get(index)),b->send(index)).bounds(leftPos+12,topPos+54+row*22,306,20).build());}
        addRenderableWidget(Button.builder(Component.literal("上一页"),b->{page=Math.max(0,page-1);rebuild();}).bounds(leftPos+12,topPos+213,60,20).build());
        addRenderableWidget(Button.builder(Component.literal("下一页"),b->{if((page+1)*7<menu.labels.size())page++;rebuild();}).bounds(leftPos+76,topPos+213,60,20).build());
        addRenderableWidget(Button.builder(Component.literal(menu.purpose==0?"清空用丹":"保留原体质"),b->send(100)).bounds(leftPos+144,topPos+213,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("完成"),b->onClose()).bounds(leftPos+242,topPos+213,76,20).build());
    }
    @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,false);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,12,9,0xFFF3DCA7,false);g.drawString(font,font.plainSubstrByWidth(menu.summary,306),12,31,0xFFD9DFCA,false);
        if(menu.labels.isEmpty())g.drawString(font,"背包内没有可选择的丹药",12,64,0xFFB6C0BE,false);}
    @Override protected void renderFitted(GuiGraphics g,int mx,int my,float tick){renderBackground(g);super.renderFitted(g,mx,my,tick);}
}
