package com.longju.xiantu.client;

import com.longju.xiantu.spell.*;
import com.longju.xiantu.network.*;
import com.longju.xiantu.combat.Element;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

/** Item-icon choices with server-authoritative loadout validation. */
public final class SpellScreen extends com.longju.xiantu.client.FittedContainerScreen<SpellMenu>{
 private final List<ItemChoiceButton> rows=new ArrayList<>();
 private final List<Button> slots=new ArrayList<>(),bindings=new ArrayList<>();
 private final List<String> draft=new ArrayList<>();private final InputConstants.Key[] keys=new InputConstants.Key[4];
 private List<SpellCatalog.Spell> matches=List.of();private int page,revision,binding=-1,type,tier,element;
 private String selected="",query="";private EditBox search;private Button previous,next,hud;
 public SpellScreen(SpellMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=354;imageHeight=236;}
 private Button button(String text,int x,int y,int w,java.util.function.Consumer<Button> fn){return addRenderableWidget(Button.builder(Component.literal(text),fn::accept).bounds(leftPos+x,topPos+y,w,16).build());}
 private void filter(){
  matches=SpellCatalog.ALL.stream().filter(SpellCatalog::enabled).filter(s->SpellClient.DATA.knows(s.id())&&s.name().contains(query.strip()))
   .filter(s->type==0||type==1&&s.active()||type==2&&!s.active()).filter(s->tier==0||s.tier()==tier)
   .filter(s->element==0||s.element()==Element.values()[element-1]).toList();
  page=Math.max(0,Math.min(page,Math.max(0,(matches.size()-1)/20)));
  for(int i=0;i<rows.size();i++){int at=page*20+i;var b=rows.get(i);b.visible=at<matches.size();if(b.visible){var s=matches.get(at);b.choice(SpellIcons.texture(s.id()),Component.literal(s.name()+" · "+s.tier()+"阶"),()->s.id().equals(selected));}}
  if(previous!=null){previous.active=page>0;next.active=(page+1)*20<matches.size();}
 }
 @Override protected void init(){
  super.init();rows.clear();slots.clear();bindings.clear();previous=null;draft.clear();for(int i=0;i<SpellData.TOTAL_SLOTS;i++)draft.add(SpellClient.DATA.slot(i));for(int i=0;i<4;i++)keys[i]=SpellClient.KEYS[i].getKey();revision=SpellClient.DATA.revision();
  search=new EditBox(font,leftPos+8,topPos+24,140,15,Component.literal("搜索法术"));search.setHint(Component.literal("搜索已学法术"));search.setValue(query);search.setResponder(t->{query=t;page=0;filter();});addRenderableWidget(search);
  button(type==0?"全部类型":type==1?"主动":"反应/被动",8,42,68,b->{type=(type+1)%3;b.setMessage(Component.literal(type==0?"全部类型":type==1?"主动":"反应/被动"));filter();});
  button("全部阶位",80,42,68,b->{tier=(tier+1)%7;b.setMessage(Component.literal(tier==0?"全部阶位":tier+"阶"));filter();});
  button("全部属性",8,60,86,b->{element=(element+1)%(Element.values().length+1);b.setMessage(Component.literal(element==0?"全部属性":Element.values()[element-1].displayName()));filter();});
  hud=button("术栏：开",98,60,50,b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,90));
  for(int i=0;i<20;i++){int row=i;rows.add(addRenderableWidget(new ItemChoiceButton(leftPos+10+i%5*28,topPos+79+i/5*28,b->{int at=page*20+row;if(at<matches.size())selected=matches.get(at).id();})));}
  previous=button("前页",8,200,68,b->{page--;filter();});next=button("后页",80,200,68,b->{page++;filter();});
  for(int i=0;i<SpellData.TOTAL_SLOTS;i++){int slot=i;slots.add(button("",164+i%4*46,i<4?39:107,44,b->{var s=SpellCatalog.find(selected);if(s!=null&&s.active()==(slot<4)){for(int j=0;j<SpellData.TOTAL_SLOTS;j++)if(draft.get(j).equals(selected))draft.set(j,"");draft.set(slot,selected);}else if(selected.isEmpty())draft.set(slot,"");}));}
  for(int i=0;i<4;i++){int slot=i;bindings.add(button("",164+i*46,57,44,b->{binding=slot;}));}
  button("选择卸下",164,76,88,b->{selected="";});button("默认按键",258,76,88,b->{for(int i=0;i<4;i++)keys[i]=SpellClient.KEYS[i].getDefaultKey();binding=-1;});
  button("保存",164,214,88,b->{for(int i=0;i<4;i++)SpellClient.KEYS[i].setKey(keys[i]);KeyMapping.resetMapping();minecraft.options.save();XiantuNetwork.CHANNEL.sendToServer(new SpellConfigurePacket(menu.containerId,revision,List.copyOf(draft)));onClose();});
  button("取消",258,214,88,b->onClose());filter();refresh();
 }
 private boolean conflict(int index){if(keys[index].equals(InputConstants.UNKNOWN))return false;for(int i=0;i<4;i++)if(i!=index&&keys[index].equals(keys[i]))return true;for(var key:minecraft.options.keyMappings)if(Arrays.stream(SpellClient.KEYS).noneMatch(k->k==key)&&key.getKey().equals(keys[index]))return true;return false;}
 private void refresh(){for(int i=0;i<SpellData.TOTAL_SLOTS;i++){var s=SpellCatalog.find(draft.get(i));slots.get(i).setMessage(Component.literal(s==null?"空":font.plainSubstrByWidth(s.name(),40)));slots.get(i).setTooltip(Tooltip.create(Component.literal(s==null?"选中法术后点击；选择卸下后点击清空":s.name()+"；"+SpellClient.reason(s.id()))));}for(int i=0;i<4;i++){bindings.get(i).setMessage(Component.literal(binding==i?"按键…":(conflict(i)?"!":"")+keys[i].getDisplayName().getString()));bindings.get(i).setTooltip(Tooltip.create(Component.literal("点击改键；Esc清除。"+(conflict(i)?"存在按键冲突，保存即确认，不会修改其他按键":"无按键冲突"))));}}
 @Override protected void containerTick(){super.containerTick();hud.setMessage(Component.literal(SpellClient.DATA.hudVisible()?"术栏：开":"术栏：关"));refresh();}
 @Override public boolean keyPressed(int key,int scan,int mods){if(binding>=0){keys[binding]=key==256?InputConstants.UNKNOWN:InputConstants.getKey(key,scan);binding=-1;refresh();return true;}if(search.isFocused()&&key!=256&&(search.keyPressed(key,scan,mods)||search.canConsumeInput()))return true;return super.keyPressed(key,scan,mods);}
 @Override protected boolean scrollFitted(double x,double y,double delta){if(x<leftPos+152){page+=delta>0?-1:1;filter();return true;}return super.scrollFitted(x,y,delta);}
 @Override protected void renderBg(GuiGraphics g,float dt,int mx,int my){com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,false);}
 private void text(GuiGraphics g,String text,int x,int y,int color){g.drawString(font,font.plainSubstrByWidth(text,x<150?140:184),x,y,color,false);}
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){int color=0xffe8dab8;text(g,"法术配置",8,8,color);text(g,"主动槽 · 选法术后点击装备",164,25,color);text(g,"反应/被动槽 · 自动触发",164,95,color);text(g,"已学 "+matches.size()+"项 · "+(page+1)+"/"+Math.max(1,(matches.size()+19)/20)+"页",8,220,color);
  var s=SpellCatalog.find(selected);if(s!=null){int level=SpellClient.DATA.mastery(s.id());text(g,s.name()+" · "+s.element().displayName()+" · "+s.tier()+"阶",164,129,color);text(g,"最低"+com.longju.xiantu.cultivation.RealmRegistry.MAJOR_REALM_NAMES.get(s.realm())+" · 冷却 "+s.cooldown()/20.0+"秒",164,143,color);text(g,"熟练 "+level+" · "+SpellClient.DATA.experience(s.id())+"/"+(level==9?"已满":SpellRules.EXPERIENCE[level]),164,157,color);text(g,(s.type().equals("charged")?"灵力 5–20%":"灵力 "+Math.round(s.cost()*100)+"%")+" · 节能 "+level*.5+"%",164,171,color);if(s.type().equals("mount"))text(g,"空格升 · 潜行降 · 双击潜行下剑",164,184,color);else if(s.type().equals("charged"))text(g,"长按蓄力 · 5秒蓄满 · 松开发射",164,184,color);else if(s.id().equals("rejuvenation"))text(g,"恢复 "+Math.round(SupportSpellRules.healRatio(level)*100)+"%生命 · 低于30%自动触发",164,184,color);else text(g,"伤害 "+String.format(java.util.Locale.ROOT,"%.1f",SpellClient.DATA.save().getCompound("Damages").getDouble(s.id()))+" · 熟练加成 "+level*10+"%",164,184,color);}else text(g,"点击槽位可卸下法术",164,129,color);
  text(g,binding>=0?"请按目标键，Esc清除":"! 表示冲突；保存确认本地改键",164,198,color);
 }
 @Override protected void renderFitted(GuiGraphics g,int mx,int my,float dt){renderBackground(g);super.renderFitted(g,mx,my,dt);renderTooltip(g,mx,my);if(!selected.isEmpty()&&!selected.equals("sword_flight")&&!selected.equals("rejuvenation")&&mx>=leftPos+164&&mx<leftPos+348&&my>=topPos+183&&my<topPos+194)g.renderTooltip(font,Component.literal(selected.equals("great_fireball")?"伤害随蓄力增加；满蓄为基础预览的5倍，直径7格、波及8格；受境界、抗性及防御影响":"随修为和熟练等级成长；实际伤害受目标境界、抗性及防御影响"),mx,my);for(var row:rows)row.renderChoiceTooltip(g,font,mx,my);}
}
