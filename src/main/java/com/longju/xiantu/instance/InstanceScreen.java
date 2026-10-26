package com.longju.xiantu.instance;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public final class InstanceScreen extends com.longju.xiantu.client.FittedContainerScreen<InstanceMenu>{
    private static final String[] SECTIONS={"入门流程", "境界成长", "灵根体质", "功法", "战斗法术", "采集资源", "妖兽材料", "炼丹", "炼器", "城镇交易", "遗迹秘境", "洞府", "聚灵阵"};
    private static final String[][] PAGES={
        {"初次进入后寻找修仙城镇，与广场引路人交谈领取玄元养气决。", "右键使用本手册可浏览各玩法说明；手册不会消耗。", "到达城镇完成“初入仙途”成就，随身引路石会被收回。"},
        {"学习玄元养气决开启修仙资格；死亡后资格保留。", "修为小阶段满额后自动提升，大境界突破需在探灵玉鉴确认。", "练气最高九层；换用合适主功法后才可继续突破筑基。", "击败敌对妖兽也能获得修为；和平妖兽不提供修为。"},
        {"灵根数量影响修炼速度与灵力属性加成；探灵玉鉴可查看实际倍率。", "开局体质随机，凡体概率最高；特殊体质提供各自效果。", "功法、修为与装备会继续影响角色战斗能力。"},
        {"持功法书右键确认，再坐在蒲团上持续按住使用键研习10秒。", "研习完成后功法书保留；常用功法可跨多个境界继续修炼。", "更换已装备主功法会按规则损失一个大境界；先确认再研习。"},
        {"主手武器左键攻击；剑、斧、长枪、棍棒使用各自攻击属性。", "法术界面配置主动与被动槽并查看绑定按键；当前开放火球术、雷引术及御剑术。", "法术伤害随法术等级和修为提升；护甲与饰品也会提供战斗效果。"},
        {"主世界自然生成灵石矿、因果矿、灵木与多种药材群系。", "使用达到要求的镐挖掘灵石矿；因果矿按普通矿石开采。", "草药种子种在对应灵耕土上，作物会经历生长阶段。", "蒲团修炼消耗所在16×16区块灵气；灵气耗尽后换区块或使用聚灵阵。"},
        {"敌对妖兽会自然生成，并按自身境界掉落妖兽材料与对应阶位妖丹。", "妖兽有不同的近战、技能和行为；首领战注意攻击范围与招式阶段。", "和平妖兽不会主动攻击，也不会提供击杀修为。"},
        {"学习丹道真解后解锁一至三阶丹方；更高阶丹药需学习对应丹方。", "选择丹药后按配方放入八卦外围材料槽，中央放灵火，辅助槽可放妖丹。", "灵石放入供能槽后开始自动计时炼制；可查看进度并取消。", "丹炉品阶、炼丹熟练、材料品质及火种会影响成丹与品质。"},
        {"学习器道真解后解锁一至三阶普通装备；四阶起需学习器方。", "先在炼器炉熔炼材料获得器胚，再放入相邻锻打台完成锻打。", "八个外围槽放材料，中央槽放灵火，灵石放供能槽自动储能。", "材料品质不可混用；设备阶位、炼器等级和图纸共同限制可制装备。"},
        {"城镇广场有万宝阁、炼器铺、丹阁、符箓坊和引路人。", "万宝阁出售功法、基础矿物与杂物；其他商铺提供各自商品。", "与NPC交谈或交易时，先核对界面列出的物品、数量和灵石价格。", "到达城镇完成成就并消耗引路石；原版村庄仍会正常生成。"},
        {"主世界会自然生成不同主题的遗迹，结构、守护者和奖励各不相同。", "探索分支通道、地下区域和侧室；部分宝箱不在主路线显眼处。", "天道秘境按现实时间轮流开启；入场修为范围与当次战利品规则一致。", "结束后秘境会遣返并归档；测试指令仅用于测试存档。"},
        {"洞府令牌可开辟个人洞府，主令牌用于进出与管理授权。", "修仙手册的洞府页面查看状态、补发主令牌或管理子令牌。", "签发与补发都会消耗材料；操作前检查页面所列费用和令牌状态。"},
        {"聚灵阵可聚拢天地灵气，配合蒲团加快打坐修炼。", "选择阵阶后手持手册瞄准平地，右键固定投影，再次右键消耗背包材料搭建。", "潜行右键取消；阵法不会覆盖建筑、水体或受保护区域。"}
    };
    private boolean guide;private int page,planTier=1;
    public InstanceScreen(InstanceMenu m,Inventory i,Component t){super(m,i,t);imageWidth=400;imageHeight=316;guide=m.view.getBoolean("Guide");inventoryLabelY=1000;}
    private void action(String text,int x,int y,int width,int action,boolean active){var b=Button.builder(Component.literal(text),v->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action)).bounds(leftPos+x,topPos+y,width,20).build();b.active=active;addRenderableWidget(b);}
    private void tab(String text,int x,int y,int width,Runnable click,boolean active){var b=Button.builder(Component.literal(text),v->click.run()).bounds(leftPos+x,topPos+y,width,18).build();b.active=active;addRenderableWidget(b);}
    @Override protected void init(){
        super.init();
        if(guide){
            if(page==12){for(int tier=1;tier<=3;tier++){final int rank=tier;tab(tier+"阶",112+(tier-1)*90,142,86,()->{planTier=rank;rebuildWidgets();},planTier!=tier);}action("开始投影搭建",112,280,266,2000+planTier,true);}
            for(int i=0;i<SECTIONS.length;i++){int chosen=i;tab(SECTIONS[i],8,32+i*18,96,()->{page=chosen;rebuildWidgets();},page!=i);}
            tab("洞府管理",242,7,75,()->{guide=false;rebuildWidgets();},true);
        }else{
            tab("修仙手册",156,7,75,()->{guide=true;page=0;rebuildWidgets();},true);
            if(menu.view.getBoolean("Owned")){
                action("补发主令牌 · 三阶灵石×16",15,79,300,0,true);
                action("签发子令牌",15,101,145,1,menu.view.getBoolean("Inside"));
                var grants=menu.view.getList("Grants",10);for(int n=0;n<grants.size();n++)action("撤销子令牌 "+(menu.view.getInt("Page")*8+n+1)+(grants.getCompound(n).getBoolean("Active")?"":"（已撤销）"),15+(n%2)*155,124+(n/2)*20,145,10+n,menu.view.getBoolean("Inside")&&grants.getCompound(n).getBoolean("Active"));
                action("上一页",15,205,90,1001,menu.view.getInt("Page")>0);action("下一页",225,205,90,1000,menu.view.getInt("Page")<menu.view.getInt("LastPage"));
            }
        }
    }
    @Override protected void renderBg(GuiGraphics g,float dt,int mx,int my){com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,true);}
    private void paragraph(GuiGraphics g,String text,int x,int y,int maxWidth,int color){
        String rest=text;while(!rest.isEmpty()){String line=font.plainSubstrByWidth(rest,maxWidth);if(line.isEmpty())break;g.drawString(font,line,x,y,color,false);y+=12;rest=rest.substring(line.length());}
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,guide?Component.literal("仙途修真手册"):title,12,12,0xff2b3731,false);
        if(guide){
            g.drawString(font,SECTIONS[page],112,38,0xff2b3731,false);int yy=61;
            for(String line:PAGES[page]){paragraph(g,line,112,yy,272,0xff3d4337);yy+=Math.max(16,((font.width(line)+271)/272)*12+4);}
            if(page==12){String text="所需材料：";for(var need:com.longju.xiantu.formation.FormationPlan.materials(planTier).entrySet())text+=need.getKey().getDescription().getString()+"×"+need.getValue()+"  ";paragraph(g,text,112,170,272,0xff3d4337);}
        }else{
            String[] lines=menu.view.getBoolean("Owned")?new String[]{"永久洞府：12×12区块，只有基础平台。","主手令牌右键进出，附近有敌人时不可使用。","签发子令牌：云玉×4、三阶灵石×1、妖骨×1。","须在洞府内主手持主令牌管理授权。"}:new String[]{"你还没有开辟永久洞府。","万宝阁出售令牌：32枚六阶完整灵石。","天道秘境核心有5%概率出现令牌。","首次开辟必须在主世界，每人仅一座。","秘境结束前5分钟遣返，死亡惩罚仍生效。"};int yy=32;for(String line:lines){g.drawString(font,line,15,yy,0xff3d4337,false);yy+=16;}
        }
    }
    @Override protected void renderFitted(GuiGraphics g,int x,int y,float dt){renderBackground(g);super.renderFitted(g,x,y,dt);}
}
