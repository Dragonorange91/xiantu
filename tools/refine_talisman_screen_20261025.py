from pathlib import Path
p=Path(__file__).resolve().parents[1]/'src/main/java/com/longju/xiantu/talisman/TalismanScreen.java'
s=p.read_text(encoding='utf-8')
s=s.replace('extends AbstractContainerScreen<TalismanMenu>', 'extends com.longju.xiantu.client.FittedContainerScreen<TalismanMenu>')
s=s.replace('private int page;', 'private int page;private net.minecraft.client.gui.components.EditBox search;private int category;private static final String[] CATEGORIES={"全部","成品符箓","材料加工"};')
s=s.replace('button(12,210,48,"上一页",-1);button(70,210,48,"下一页",-2);','button(12,210,48,"上一页",-1);button(70,210,48,"下一页",-2);')
s=s.replace('// Page changes are local; all recipe selections are still validated on the server.', '''search=new EditBox(font,leftPos+12,topPos+25,65,14,Component.literal("搜索"));search.setMaxLength(24);search.setHint(Component.literal("搜索"));search.setResponder(value->page=0);addRenderableWidget(search);
  addRenderableWidget(Button.builder(Component.literal(CATEGORIES[category]),b->{category=(category+1)%3;page=0;b.setMessage(Component.literal(CATEGORIES[category]));}).bounds(leftPos+80,topPos+25,42,14).build());
  // Page changes are local; all recipe selections are still validated on the server.''')
s=s.replace('com.longju.xiantu.client.WorkstationGui.background(g,leftPos,topPos,imageWidth,imageHeight);', '''var skin=com.longju.xiantu.client.WorkstationGui.class;
  com.longju.xiantu.client.WorkstationGui.frame(g,leftPos,topPos,imageWidth,imageHeight,false);
  com.longju.xiantu.client.WorkstationGui.panel(g,leftPos+7,topPos+40,115,163);
  com.longju.xiantu.client.WorkstationGui.frame(g,leftPos+126,topPos+41,136,136,true);
  com.longju.xiantu.client.WorkstationGui.panel(g,leftPos+270,topPos+40,112,163);
  for(var slot:menu.slots)com.longju.xiantu.client.WorkstationGui.slot(g,leftPos,topPos,slot);''')
s=s.replace('var skin=com.longju.xiantu.client.WorkstationGui.class;\n  ','')
s=s.replace('int n=page*24+i;if(n>=TalismanRecipes.ALL.size())break;', 'var list=visible();int item=page*24+i;if(item>=list.size())break;int n=list.get(item);')
s=s.replace('if(r.kind().equals("symbol")&&!menu.knows(r.id()))', 'if(!menu.learned(r))')
s=s.replace('g.drawCenteredString(font,name,195,28,0xffffdf9c);', 'g.drawCenteredString(font,font.plainSubstrByWidth(name,140),195,28,0xffffdf9c);')
s=s.replace('g.drawString(font,"成品",322,166', 'g.drawString(font,"成品",322,157')
s=s.replace('@Override public void render(GuiGraphics g,int mx,int my,float partial)', '@Override protected void renderFitted(GuiGraphics g,int mx,int my,float partial)')
s=s.replace('super.render(g,mx,my,partial);','super.renderFitted(g,mx,my,partial);')
s=s.replace('int n=page*24+i,x=leftPos+12+i%4*26,y=topPos+43+i/4*26;if(n<TalismanRecipes.ALL.size()&&', 'var list=visible();int item=page*24+i,x=leftPos+12+i%4*26,y=topPos+43+i/4*26;if(item<list.size()&&')
s=s.replace('var r=TalismanRecipes.ALL.get(n);var lines=', 'var r=TalismanRecipes.ALL.get(list.get(item));var lines=')
s=s.replace('if(r.kind().equals("symbol"))lines.add', 'if(!menu.learned(r))lines.add(Component.literal("尚未学习对应符谱或工艺"));if(r.kind().equals("symbol"))lines.add')
s=s.replace('@Override public boolean mouseClicked(double x,double y,int button)', '@Override protected boolean clickFitted(double x,double y,int button)')
s=s.replace('(TalismanRecipes.ALL.size()-1)/24','Math.max(0,(visible().size()-1)/24)')
s=s.replace('if(index<TalismanRecipes.ALL.size()&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+index);','var list=visible();if(index<list.size()&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+list.get(index));')
s=s.replace('return super.mouseClicked(x,y,button);','return super.clickFitted(x,y,button);')
s=s.replace('@Override public boolean mouseDragged(double x,double y,int button,double dx,double dy)','@Override protected boolean dragFitted(double x,double y,int button,double dx,double dy)')
s=s.replace('super.mouseDragged(x,y,button,dx,dy)','super.dragFitted(x,y,button,dx,dy)')
s=s.replace('@Override public boolean mouseReleased(double x,double y,int button)','@Override protected boolean releaseFitted(double x,double y,int button)')
s=s.replace('super.mouseReleased(x,y,button)','super.releaseFitted(x,y,button)')
s=s.replace(' private boolean canvas(double x,double y)', ''' private List<Integer> visible(){var out=new ArrayList<Integer>();String query=search==null?"":search.getValue().strip();for(int i=0;i<TalismanRecipes.ALL.size();i++){var r=TalismanRecipes.ALL.get(i);if(category==1&&!r.kind().equals("symbol")||category==2&&r.kind().equals("symbol"))continue;if(!query.isEmpty()&&!TalismanRecipes.name(r).contains(query))continue;out.add(i);}page=Math.max(0,Math.min(page,Math.max(0,(out.size()-1)/24)));return out;}
 @Override protected boolean scrollFitted(double x,double y,double delta){if(x>=leftPos+8&&x<leftPos+124&&y>=topPos+40&&y<topPos+205){page=Math.max(0,Math.min(Math.max(0,(visible().size()-1)/24),page+(delta<0?1:-1)));return true;}return super.scrollFitted(x,y,delta);}
 @Override public boolean keyPressed(int key,int scan,int mods){if(search!=null&&search.isFocused()&&key!=256)return search.keyPressed(key,scan,mods)||key!=256;return super.keyPressed(key,scan,mods);}
 private boolean canvas(double x,double y)''')
# Name reflects actual ingredient count and output ownership; background uses actual menu slots.
p.write_text(s,encoding='utf-8')
print('Talisman screen: fitted, catalog search/filter, actual slot frames')
