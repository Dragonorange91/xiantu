package com.longju.xiantu.wood;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class SaplingGrowthScreen extends com.longju.xiantu.client.FittedContainerScreen<SaplingGrowthMenu>{
    private Button single,batch,preview,overflow;
    public SaplingGrowthScreen(SaplingGrowthMenu m,Inventory i,Component title){super(m,i,title);imageWidth=284;imageHeight=196;}
    private Button button(String label,int y,int action){return addRenderableWidget(Button.builder(Component.literal(label),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action)).bounds(leftPos+18,topPos+y,248,20).build());}
    @Override protected void init(){super.init();single=button("确认消耗一枚",88,1);batch=button("批量：只投入不溢出的枚数",112,2);preview=button("预览最后溢出的一枚",136,3);overflow=button("确认一枚，舍弃多余灵力",160,4);}
    @Override public void containerTick(){super.containerTick();single.visible=!menu.batch;single.active=menu.allowed();batch.visible=preview.visible=overflow.visible=menu.batch;batch.active=menu.allowed()&&menu.perStone()>0&&menu.remaining()>=menu.perStone();preview.active=menu.allowed();overflow.active=menu.allowed()&&menu.validPreview()&&menu.wastedPower()>0;single.setMessage(Component.literal(menu.wastedPower()>0?"确认一枚，舍弃"+menu.wastedPower()+"灵力":"确认消耗一枚"));}
    @Override protected void renderBg(GuiGraphics g,float tick,int x,int y){com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,false);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,12,10,0xffeedec0,false);
        double progress=100.0*(menu.total()-menu.remaining())/menu.total();
        g.drawString(font,String.format(java.util.Locale.ROOT,"成长进度：%.2f%%",progress),12,30,0xffbde7af,false);
        g.drawString(font,"从零催熟需"+menu.type.stones+"枚同阶灵石；现有"+menu.count()+"枚",12,46,0xffeedec0,false);
        g.drawString(font,"每枚推进"+String.format(java.util.Locale.ROOT,"%.2f%%",100.0*menu.perStone()/menu.total()),12,62,0xffeedec0,false);
        g.drawString(font,menu.reasonText(),12,76,menu.allowed()?0xffbde7af:0xffffb8a4,false);
    }
    @Override protected void renderFitted(GuiGraphics g,int x,int y,float tick){renderBackground(g);super.renderFitted(g,x,y,tick);renderTooltip(g,x,y);}
}
