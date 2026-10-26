package com.longju.xiantu.alchemy;
import com.longju.xiantu.refining.RefiningRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;
import com.longju.xiantu.client.*;
import com.longju.xiantu.refining.WorkstationLayout;

public final class AlchemyScreen extends FittedContainerScreen<AlchemyMenu>{
    private static final String[] STAGES={"温炉","投入主药","投入辅药","投入调和组","合药控火","凝丹收炉"};
    private final List<ItemChoiceButton> rows=new ArrayList<>();
    private final List<Button> rankButtons=new ArrayList<>();
    private List<AlchemyRecipes.Recipe> matches=List.of();
    private Button previous,next,start,cancel;
    private EditBox search;
    private int knownFurnace=-1;private int rank=1,page;
    private String query="";
    public AlchemyScreen(AlchemyMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=WorkstationLayout.WIDTH;imageHeight=WorkstationLayout.HEIGHT;}
    private void send(int action){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    private Button button(String label,int x,int y,int width,java.util.function.Consumer<Button> callback){return addRenderableWidget(Button.builder(Component.literal(label),callback::accept).bounds(leftPos+x,topPos+y,width,18).build());}
    private void filter(){
        matches=AlchemyRecipes.ALL.stream().filter(r->r.rank()==rank&&AlchemyRules.furnace(r.rank())<=menu.data.get(5)&&r.name().contains(query.strip())).toList();page=Math.max(0,Math.min(page,Math.max(0,(matches.size()-1)/15)));
        for(int i=0;i<rows.size();i++){int at=page*15+i;var b=rows.get(i);b.visible=at<matches.size();if(b.visible){var r=matches.get(at);b.choice(r.preview(),Component.literal(r.name()+" · "+r.rank()+"阶"),()->r.equals(menu.recipe()));}}
        if(previous!=null){previous.active=page>0;next.active=(page+1)*15<matches.size();}
        if(menu.data.get(1)==0&&!matches.contains(menu.recipe()))send(matches.isEmpty()?999:1000+AlchemyRecipes.ALL.indexOf(matches.get(0)));
    }
    @Override protected void init(){
        super.init();rows.clear();rankButtons.clear();previous=null;next=null;
        int count=0;for(int i=1;i<=8;i++){int chosen=i;if(AlchemyRules.furnace(i)<=menu.data.get(5))rankButtons.add(button(i+"阶",8+(count++)*56,23,52,b->{rank=chosen;page=0;filter();}));}
        search=new EditBox(font,leftPos+10,topPos+47,144,16,Component.literal("搜索丹药"));search.setValue(query);search.setHint(Component.literal("搜索丹药"));search.setResponder(value->{query=value;page=0;filter();});addRenderableWidget(search);
        for(int i=0;i<15;i++){int row=i;rows.add(addRenderableWidget(new ItemChoiceButton(leftPos+12+i%5*28,topPos+68+i/5*28,b->{int at=page*15+row;if(at<matches.size())send(1000+AlchemyRecipes.ALL.indexOf(matches.get(at)));})));}
        previous=button("上一页",8,165,70,b->{page--;filter();});next=button("下一页",84,165,70,b->{page++;filter();});
        start=button("开始炼丹",8,187,146,b->send(1));
        cancel=button("取消本炉",8,187,146,b->send(2));filter();update();
    }
    private void update(){
        boolean busy=menu.data.get(1)==1,available=menu.recipe()!=null&&matches.contains(menu.recipe());
        start.visible=!busy;start.active=available;cancel.visible=busy;
        for(int i=0;i<rows.size();i++){rows.get(i).visible=!busy&&page*15+i<matches.size();rows.get(i).active=!busy;}
        for(var rankButton:rankButtons)rankButton.active=!busy;search.setVisible(!busy);previous.visible=next.visible=!busy;
    }
    @Override protected void containerTick(){super.containerTick();if(knownFurnace!=menu.data.get(5)){knownFurnace=menu.data.get(5);rank=1;page=0;rebuildWidgets();return;}update();}
    @Override public boolean keyPressed(int key,int scan,int mods){if(search.isFocused()&&key!=256&&(search.keyPressed(key,scan,mods)||search.canConsumeInput()))return true;return super.keyPressed(key,scan,mods);}
    @Override protected boolean scrollFitted(double x,double y,double delta){if(x>=leftPos+8&&x<leftPos+156&&y>=topPos+68&&y<topPos+164){page+=delta>0?-1:1;filter();return true;}return super.scrollFitted(x,y,delta);}
    @Override protected void renderBg(GuiGraphics g,float dt,int x,int y){
        WorkstationGui.background(g,leftPos,topPos,imageWidth,imageHeight);
        WorkstationGui.bagua(g,leftPos+WorkstationLayout.BAGUA_X,topPos+WorkstationLayout.BAGUA_Y);
        for(var slot:menu.slots)if(slot.isActive())WorkstationGui.slot(g,leftPos,topPos,slot);
        if(menu.data.get(1)==1)WorkstationGui.progress(g,leftPos+176,topPos+350,366,menu.data.get(9),menu.data.get(10));
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        int color=WorkstationGui.TEXT;g.drawString(font,title,8,8,color,false);
        var r=menu.recipe();
        if(r!=null){
            g.drawCenteredString(font,r.name()+" · "+r.rank()+"阶",424,61,color);
            g.drawString(font,"所需材料",184,225,WorkstationGui.MUTED,false);
            for(int i=0;i<r.inputs().size();i++){
                var item=r.inputs().get(i).example();int ix=WorkstationLayout.REQUIREMENT_X+i*WorkstationLayout.REQUIREMENT_STEP;
                g.renderItem(item,ix,WorkstationLayout.REQUIREMENT_Y);g.renderItemDecorations(font,item,ix,WorkstationLayout.REQUIREMENT_Y);
            }
            g.drawString(font,"成丹率 "+String.format(Locale.ROOT,"%.1f%%",menu.data.get(12)/100.0),352,178,color,false);
            g.drawString(font,"需 "+AlchemyRules.energy(r.rank()),184,332,color,false);
        }else g.drawString(font,"当前条件下暂无丹方",352,61,color,false);
        g.drawCenteredString(font,"辅助妖丹",416,88,WorkstationGui.MUTED);g.drawCenteredString(font,"最多8枚",416,139,WorkstationGui.MUTED);g.drawString(font,"产物",506,135,WorkstationGui.MUTED,false);
        g.drawString(font,"灵石供能",184,276,color,false);
        g.drawString(font,"储能",214,291,WorkstationGui.MUTED,false);g.drawString(font,String.valueOf(menu.data.get(7)+menu.data.get(8)*32768),214,306,color,false);
        if(menu.data.get(1)==1){
            g.drawString(font,(menu.data.get(25)>0?"已暂停 · ":"")+STAGES[Math.max(0,Math.min(5,menu.data.get(13)))],12,72,color,false);
            g.drawString(font,"炉温 "+menu.data.get(27)+" · "+(menu.data.get(28)==0?"文火":"武火"),12,99,color,false);
            g.drawString(font,"目标 "+(menu.data.get(29)==0?"文火 25~45":"武火 65~85"),12,119,WorkstationGui.MUTED,false);
        }
    }
    @Override protected void renderFitted(GuiGraphics g,int x,int y,float dt){
        renderBackground(g);super.renderFitted(g,x,y,dt);renderTooltip(g,x,y);var r=menu.recipe();
        for(var row:rows)row.renderChoiceTooltip(g,font,x,y);
        if(r!=null)for(int i=0;i<r.inputs().size();i++)if(x>=leftPos+WorkstationLayout.REQUIREMENT_X+i*WorkstationLayout.REQUIREMENT_STEP&&x<leftPos+WorkstationLayout.REQUIREMENT_X+i*WorkstationLayout.REQUIREMENT_STEP+16&&y>=topPos+WorkstationLayout.REQUIREMENT_Y&&y<topPos+WorkstationLayout.REQUIREMENT_Y+16){String group=switch(r.groups().get(i)){case "main"->"主药";case "auxiliary"->"辅药";default->"调和组";};g.renderComponentTooltip(font,List.of(r.inputs().get(i).example().getHoverName(),Component.literal(group+" · 数量 "+r.inputs().get(i).count())),x,y);}
    }
}
