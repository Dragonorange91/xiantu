package com.longju.xiantu.refining;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;
import com.longju.xiantu.client.*;

/** Searchable recipe catalogue. Selecting and starting remain server validated. */
public final class RefiningScreen extends FittedContainerScreen<RefiningMenu> {
    private static final String[] KINDS={"","sword","axe","spear","armor","necklace","ring"};
    private static final String[] LABELS={"全","剑","斧","枪","甲","链","戒"};
    private EditBox search;private Button start,previous,next;
    private final List<ItemChoiceButton> rows=new ArrayList<>();
    private List<RefiningRecipes.Recipe> matches=List.of();
    private int knownFurnace=-1;private int tier=1,category,page;private String query="";private boolean facilities;
    public RefiningScreen(RefiningMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=WorkstationLayout.WIDTH;imageHeight=WorkstationLayout.HEIGHT;}
    private void send(int action){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    private Button button(String text,int x,int y,int width,java.util.function.Consumer<Button> action){return addRenderableWidget(Button.builder(Component.literal(text),action::accept).bounds(leftPos+x,topPos+y,width,18).build());}
    private String fit(String s,int width){return font.width(s)>width?font.plainSubstrByWidth(s,width-8)+"…":s;}
    private void filter(){
        String term=query.strip().toLowerCase(Locale.ROOT);
        matches=(facilities?FacilityRecipes.ALL.stream().map(FacilityRecipes.Recipe::allocationRecipe):RefiningRecipes.ALL.stream()).filter(r->r.tier()==tier&&r.furnace()<=menu.data.get(5)&&(facilities||category==0||r.kind().equals(KINDS[category]))&&(term.isEmpty()||r.name().toLowerCase(Locale.ROOT).contains(term))).toList();
        page=Math.max(0,Math.min(page,Math.max(0,(matches.size()-1)/20)));
        for(int i=0;i<rows.size();i++){
            int index=page*20+i;var b=rows.get(i);b.visible=index<matches.size();b.active=menu.data.get(12)==0;
            if(b.visible){var r=matches.get(index);var icon=r.result();if(facilities)icon.setCount(FacilityRecipes.find(r.id()).count());b.choice(icon,Component.literal(r.name()+" · "+r.tier()+"阶"),()->r.equals(visibleRecipe()));}
        }
        if(previous!=null){previous.active=page>0;next.active=(page+1)*20<matches.size();}
        if(!matches.isEmpty()&&!matches.contains(visibleRecipe())&&menu.data.get(12)==0)select(matches.get(0));
    }
    private void select(RefiningRecipes.Recipe r){send(facilities?2000+FacilityRecipes.ALL.indexOf(FacilityRecipes.find(r.id())):1000+RefiningRecipes.ALL.indexOf(r));}
    @Override protected void init(){
        super.init();rows.clear();search=null;previous=null;next=null;
        if(!menu.forge&&!menu.alchemy){
            button("装备 / 设备",234,3,90,b->{facilities=!facilities;tier=1;page=0;rebuildWidgets();});
            int count=0;for(int t=1;t<=7;t++){final int chosen=t;
                boolean available=facilities?FacilityRecipes.ALL.stream().anyMatch(r->r.tier()==chosen&&r.furnace()<=menu.data.get(5)):RefiningRecipes.ALL.stream().anyMatch(r->r.tier()==chosen&&r.furnace()<=menu.data.get(5));
                if(available)button(t+"阶",8+(count++)*64,23,60,b->{tier=chosen;page=0;filter();});
            }
            search=new EditBox(font,leftPos+10,topPos+47,144,16,Component.literal("搜索装备名称"));search.setMaxLength(40);search.setValue(query);search.setHint(Component.literal("搜索装备名称"));search.setResponder(text->{query=text;page=0;filter();});addRenderableWidget(search);
            for(int i=0;i<KINDS.length;i++){final int chosen=i;button(LABELS[i],8+i*21,68,21,b->{category=chosen;page=0;filter();});}
            for(int i=0;i<20;i++){final int row=i;rows.add(addRenderableWidget(new ItemChoiceButton(leftPos+12+i%5*28,topPos+90+i/5*28,b->{int at=page*20+row;if(at<matches.size())select(matches.get(at));})));}
            previous=button("上一页",8,210,70,b->{page--;filter();});next=button("下一页",84,210,70,b->{page++;filter();});
            filter();
        }
        start=button(menu.forge?"开始锻打":"熔炼器胚",354,86,136,b->send(2));start.visible=!menu.alchemy;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(search!=null&&search.isFocused()&&key!=256){if(search.keyPressed(key,scan,modifiers)||search.canConsumeInput())return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override protected boolean scrollFitted(double x,double y,double delta){
        if(!menu.forge&&!menu.alchemy&&x>=leftPos+8&&x<leftPos+156&&y>=topPos+90&&y<topPos+202){page+=delta>0?-1:1;filter();return true;}
        return super.scrollFitted(x,y,delta);
    }
    @Override public void containerTick(){super.containerTick();if(!menu.forge&&knownFurnace!=menu.data.get(5)){knownFurnace=menu.data.get(5);tier=1;page=0;rebuildWidgets();return;}boolean busy=menu.data.get(12)==1;start.active=!busy&&visibleRecipe()!=null&&(menu.forge||matches.contains(visibleRecipe()))&&(menu.forge||menu.data.get(5)>=visibleRecipe().furnace());start.setMessage(Component.literal(menu.forge?"开始锻打":facilities?"开始装配":"熔炼器胚"));for(var row:rows)row.active=!busy;}
    @Override protected void renderBg(GuiGraphics g,float dt,int x,int y){
        WorkstationGui.background(g,leftPos,topPos,imageWidth,imageHeight);
        if(!menu.forge)WorkstationGui.bagua(g,leftPos+WorkstationLayout.BAGUA_X,topPos+WorkstationLayout.BAGUA_Y);
        for(var slot:menu.slots)if(slot.isActive())WorkstationGui.slot(g,leftPos,topPos,slot);
        if(menu.forge)WorkstationGui.arrow(g,leftPos+270,topPos+121,216);
        else WorkstationGui.arrow(g,leftPos+352,topPos+134,136);
        if(menu.data.get(12)==1)WorkstationGui.progress(g,leftPos+176,topPos+350,366,menu.data.get(9),menu.data.get(10));
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        int c=WorkstationGui.TEXT;g.drawString(font,title,8,8,c,false);
        if(!menu.forge&&!menu.alchemy)g.drawString(font,matches.size()+"项 · "+(page+1)+"/"+Math.max(1,(matches.size()+19)/20)+"页",10,258,c,false);
        if(!menu.forge&&!menu.alchemy&&matches.isEmpty())g.drawString(font,"没有符合条件的装备",10,94,c,false);
        var r=visibleRecipe();
        if(r!=null){
            g.drawCenteredString(font,fit(r.name()+" · "+r.tier()+"阶",300),424,61,c);
            if(!menu.forge&&!menu.alchemy){
                g.drawString(font,"所需材料",184,225,WorkstationGui.MUTED,false);
                for(int i=0;i<r.inputs().size();i++){
                    var item=r.inputs().get(i).example();int ix=WorkstationLayout.REQUIREMENT_X+i*WorkstationLayout.REQUIREMENT_STEP;
                    g.renderItem(item,ix,WorkstationLayout.REQUIREMENT_Y);g.renderItemDecorations(font,item,ix,WorkstationLayout.REQUIREMENT_Y);
                }
            }
            g.drawString(font,menu.facilityRecipe()!=null?"装配必成":"成器率 "+menu.data.get(13)+"%",menu.forge?184:352,menu.forge?162:154,c,false);
        }else g.drawString(font,menu.forge?"请放入器胚":"请选择配方",184,61,c,false);
        if(menu.forge)g.drawString(font,"器胚",238,88,WorkstationGui.MUTED,false);
        g.drawString(font,"产物",506,135,WorkstationGui.MUTED,false);
        if(menu.facilityRecipe()!=null){
            g.drawString(font,"需"+menu.facilityRecipe().furnace()+"阶炉 · 相邻空闲锻打台",352,178,c,false);
        }
        if(menu.forge){
            g.drawString(font,"锻打成型",10,50,c,false);g.drawString(font,"继承器胚材料",10,66,WorkstationGui.MUTED,false);
            g.drawString(font,"及炉具品质",10,78,WorkstationGui.MUTED,false);
            g.drawString(font,menu.data.get(12)==1?"锻打中":"待锻打",184,280,c,false);
        }else{
            g.drawString(font,"灵石供能",184,276,c,false);
            g.drawString(font,"储能",214,291,WorkstationGui.MUTED,false);g.drawString(font,String.valueOf(menu.data.get(7)+menu.data.get(8)*32768),214,306,c,false);
            if(r!=null&&menu.facilityRecipe()==null)g.drawString(font,"需 "+RefiningRules.energy(r.tier()),184,332,c,false);
        }
    }
    private RefiningRecipes.Recipe visibleRecipe(){var facility=menu.facilityRecipe();if(facility!=null)return facility.allocationRecipe();var r=menu.recipe();return menu.alchemy?null:menu.forge||menu.data.get(12)==1||matches.contains(r)?r:null;}
    @Override protected void renderFitted(GuiGraphics g,int x,int y,float dt){
        renderBackground(g);super.renderFitted(g,x,y,dt);renderTooltip(g,x,y);
        for(var row:rows)row.renderChoiceTooltip(g,font,x,y);
        var r=visibleRecipe();if(r!=null&&!menu.forge&&!menu.alchemy)for(int i=0;i<r.inputs().size();i++){int sx=leftPos+WorkstationLayout.REQUIREMENT_X+i*WorkstationLayout.REQUIREMENT_STEP,sy=topPos+WorkstationLayout.REQUIREMENT_Y;if(x>=sx&&x<sx+16&&y>=sy&&y<sy+16)g.renderTooltip(font,r.inputs().get(i).example(),x,y);}
    }
}
