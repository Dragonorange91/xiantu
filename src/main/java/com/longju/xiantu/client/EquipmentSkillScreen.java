package com.longju.xiantu.client;
import com.longju.xiantu.refining.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class EquipmentSkillScreen extends com.longju.xiantu.client.FittedContainerScreen<EquipmentSkillMenu> {
    private int disguise;private List<String> discovered=List.of();
    public EquipmentSkillScreen(EquipmentSkillMenu m,Inventory i,Component t){super(m,i,t);imageWidth=380;imageHeight=248;}
    private Component disguiseName(){return Component.literal(discovered.isEmpty()?"尚未发现普通妖兽":"变化：").append(discovered.isEmpty()?Component.empty():Component.translatable("entity.xiantu."+discovered.get(disguise)));}
    @Override protected void init(){
        super.init();discovered=QitianSkills.DISGUISES.stream().filter(id->EquipmentSkillClient.state.getCompound("Discovered").getBoolean(id)).toList();disguise=discovered.isEmpty()?0:Math.min(disguise,discovered.size()-1);
        for(int i=0;i<QitianSkills.IDS.length;i++){int selected=i;addRenderableWidget(Button.builder(Component.literal(QitianSkills.NAMES[i]),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,selected)).bounds(leftPos+12,topPos+32+i*38,96,22).build());}
        var choice=addRenderableWidget(Button.builder(disguiseName(),b->{if(!discovered.isEmpty())disguise=(disguise+1)%discovered.size();b.setMessage(disguiseName());}).bounds(leftPos+12,topPos+170,260,22).build());choice.active=!discovered.isEmpty();
        var change=addRenderableWidget(Button.builder(Component.literal("变化"),b->{if(!discovered.isEmpty())minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+QitianSkills.DISGUISES.indexOf(discovered.get(disguise)));}).bounds(leftPos+282,topPos+170,86,22).build());change.active=!discovered.isEmpty();
        addRenderableWidget(Button.builder(Component.literal("完成"),b->onClose()).bounds(leftPos+282,topPos+216,86,22).build());
    }
    @Override protected void renderBg(GuiGraphics g,float dt,int x,int y){WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,false);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,12,10,0xffefd79c,false);var n=EquipmentSkillClient.state;int chosen=Math.max(0,Math.min(QitianSkills.IDS.length-1,n.getInt("Selected")));
        for(int i=0;i<QitianSkills.IDS.length;i++){long ticks=Math.max(0,n.getLong("cooldown_"+QitianSkills.IDS[i])-EquipmentSkillClient.now());String reason=n.getString("Reason"+i);var lines=font.split(Component.literal((chosen==i?"已选择 · ":"")+(ticks>0?(ticks+19)/20+"秒":reason.isEmpty()?"可使用":reason)+" · 灵力 "+n.getInt("Cost"+i)),248);for(int j=0;j<Math.min(3,lines.size());j++)g.drawString(font,lines.get(j),116,34+i*38+j*10,0xffeddfbd,false);}
        g.drawString(font,"施放："+EquipmentSkillClient.USE.getTranslatedKeyMessage().getString()+"；潜行加该键打开选择",12,150,0xffd4dede,false);
        g.drawString(font,discovered.isEmpty()?"接近妖兽可发现变化形态":QitianShapeAbilities.description(discovered.get(disguise))+"；变化持续120秒",12,201,0xffbcd7e5,false);
    }
    @Override protected void renderFitted(GuiGraphics g,int x,int y,float dt){renderBackground(g);super.renderFitted(g,x,y,dt);renderTooltip(g,x,y);}
}
