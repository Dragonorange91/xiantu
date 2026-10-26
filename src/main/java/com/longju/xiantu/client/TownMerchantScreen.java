package com.longju.xiantu.client;

import com.longju.xiantu.trade.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

public final class TownMerchantScreen extends com.longju.xiantu.client.FittedContainerScreen<TownMerchantMenu>{
    private final List<Button> purchases=new ArrayList<>();
    private final List<Button> filters=new ArrayList<>();
    private static final List<String> CATEGORIES=List.of("全部","图籍成品","材料","回收","兑换");
    private String category="全部";
    private Button previous,next;
    private int page;
    public TownMerchantScreen(TownMerchantMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=300;imageHeight=236;inventoryLabelY=140;}
    @Override protected void init(){
        super.init();purchases.clear();filters.clear();
        for(int i=0;i<CATEGORIES.size();i++){String name=CATEGORIES.get(i);filters.add(addRenderableWidget(Button.builder(Component.literal(name),b->{category=name;page=0;update();}).bounds(leftPos+12+i*56,topPos+21,54,15).build()));}
        for(int i=0;i<5;i++){int row=i;purchases.add(addRenderableWidget(Button.builder(Component.literal("购买"),b->{var offers=offers();int at=page*5+row;if(at<offers.size())minecraft.gameMode.handleInventoryButtonClick(menu.containerId,TownTradeService.OFFERS.indexOf(offers.get(at)));}).bounds(leftPos+248,topPos+38+i*16,42,16).build()));}
        previous=addRenderableWidget(Button.builder(Component.literal("上一页"),b->{page--;update();}).bounds(leftPos+12,topPos+119,62,18).build());
        next=addRenderableWidget(Button.builder(Component.literal("下一页"),b->{page++;update();}).bounds(leftPos+228,topPos+119,62,18).build());update();
    }
    private List<TownTradeService.Offer> offers(){return menu.offers().stream().filter(o->category.equals("全部")||o.category().equals(category)).toList();}
    private String quote(TownTradeService.Offer o){return (o.recycling()?"获得":"支付")+o.rank()+"阶完整灵石×"+o.price();}
    private void update(){
        var offers=offers();page=Math.max(0,Math.min(page,Math.max(0,(offers.size()-1)/5)));
        for(int i=0;i<filters.size();i++){var b=filters.get(i);String name=CATEGORIES.get(i);b.visible=!menu.guide();b.active=!name.equals(category)&&menu.offers().stream().anyMatch(o->name.equals("全部")||o.category().equals(name));}
        previous.visible=next.visible=!offers.isEmpty();previous.active=page>0;next.active=(page+1)*5<offers.size();
        for(int row=0;row<5;row++){
            int at=page*5+row;var button=purchases.get(row);button.visible=at<offers.size();if(!button.visible)continue;
            var o=offers.get(at);int index=TownTradeService.OFFERS.indexOf(o);boolean known=menu.known(index),learnOnce=Set.of("book","treatise","talisman_book").contains(o.kind());int remaining=menu.remaining(index);
            button.active=menu.purchases()>=o.purchases()&&(!learnOnce||!known)&&remaining!=0;
            button.setMessage(Component.literal(remaining==0?"售罄":menu.purchases()<o.purchases()?"未开放":known&&learnOnce?"已学":o.recycling()?"回收":o.exchange()?"兑换":"购买"));
            button.setTooltip(Tooltip.create(Component.literal(o.stack().getHoverName().getString()+"×"+o.amount()+"；"+quote(o)+(remaining>=0?"；今日剩余 "+remaining+"/"+TownTradeService.dailyLimit(o)+"（游戏日，跨商人共享）":"")+(o.purchases()>0?"；本店交易 "+menu.purchases()+"/"+o.purchases():"")+(known?"；已掌握":"")+(o.recycling()?"；各品质按同一基础价回收":""))));
        }
    }
    @Override protected void containerTick(){super.containerTick();update();}
    @Override protected void renderBg(GuiGraphics g,float dt,int mx,int my){
        com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,300,236,true);
        var offers=offers();for(int row=0;row<5;row++){int at=page*5+row;if(at>=offers.size())break;var o=offers.get(at);int y=topPos+38+row*16;g.renderItem(o.stack(),leftPos+12,y);g.renderItemDecorations(font,o.stack(),leftPos+12,y);String name=o.stack().getHoverName().getString();if(font.width(name)>143)name=font.plainSubstrByWidth(name,135)+"…";g.drawString(font,name,leftPos+34,y+4,0x352919,false);g.drawString(font,(o.recycling()?"+":"-")+o.rank()+"阶灵石×"+o.price(),leftPos+183,y+4,0x51432c,false);}
        for(var slot:menu.slots)com.longju.xiantu.client.WorkstationGui.slot(g,leftPos,topPos,slot);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,(imageWidth-font.width(title))/2,10,0x352919,false);
        if(menu.guide()){
            g.drawString(font,"仙途始于静心，道友不妨从此入门。",18,45,0x352919,false);
            g.drawString(font,menu.claimed()?"养气诀已赠予你，坐于蒲团持书研习。":"背包已满，请腾出位置后再次交谈。",18,65,0x352919,false);
            g.drawString(font,"学成后打坐吸纳灵气，可修至练气九层。",18,85,0x352919,false);
            g.drawString(font,"届时前往万宝阁，选择五行基础功法。",18,105,0x352919,false);
        }else if(menu.offers().isEmpty()){
            g.drawString(font,"道友有礼，本店尚在筹备。",26,52,0x352919,false);
        }else g.drawCenteredString(font,(page+1)+"/"+Math.max(1,(offers().size()+4)/5)+"页 · 本店交易 "+menu.purchases(),150,124,0xff51432c);
        g.drawString(font,"背包",18,141,0x352919,false);if(!menu.offers().isEmpty())g.drawString(font,category.equals("回收")?"回收后发放灵石":"支付仅收完整灵石",192,141,0x51432c,false);
    }
    @Override protected boolean scrollFitted(double x,double y,double delta){if(!menu.offers().isEmpty()&&y<topPos+138){page+=delta>0?-1:1;update();return true;}return super.scrollFitted(x,y,delta);}
    @Override protected void renderFitted(GuiGraphics g,int x,int y,float dt){renderBackground(g);super.renderFitted(g,x,y,dt);renderTooltip(g,x,y);var offers=offers();for(int row=0;row<5;row++){int at=page*5+row,sy=topPos+38+row*16;if(at<offers.size()&&x>=leftPos+12&&x<leftPos+28&&y>=sy&&y<sy+16)g.renderTooltip(font,offers.get(at).stack(),x,y);}}
}
