"""Apply shared raster skin without changing menu slots, labels or interactions."""
from pathlib import Path
import re, shutil
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'src/main/java/com/longju/xiantu'
OUT=ROOT/'asset_sources/presentation-20261024/before'
W='com.longju.xiantu.client.WorkstationGui'
def edit(name, fn):
 p=next(SRC.rglob(name+'.java'));s=p.read_text(encoding='utf8');t=fn(s)
 if t==s:raise ValueError('No change '+name)
 b=OUT/p.relative_to(ROOT);b.parent.mkdir(parents=True,exist_ok=True)
 if not b.exists():shutil.copy2(p,b)
 p.write_text(t,encoding='utf8')
def frame_simple(s,paper=False):
 return re.sub(r'g\.fill\(leftPos,topPos,leftPos\+imageWidth,topPos\+imageHeight,0x[0-9a-fA-F]+\);',f'{W}.frame(g,leftPos,topPos,imageWidth,imageHeight,{str(paper).lower()});',s)
edit('AccessoryScreen',lambda s:s.replace('g.fill(x,y,x+imageWidth,y+imageHeight,0xff293d3e);\n        g.fill(x+2,y+2,x+imageWidth-2,y+imageHeight-2,0xffd9d3ba);\n        g.fill(x+5,y+5,x+imageWidth-5,y+68,0xff324a4b);',f'{W}.frame(g,x,y,imageWidth,imageHeight,true);\n        {W}.panel(g,x+5,y+5,imageWidth-10,63);').replace('for(var s:menu.slots){g.fill(x+s.x-1,y+s.y-1,x+s.x+17,y+s.y+17,0xff6c685c);g.fill(x+s.x,y+s.y,x+s.x+16,y+s.y+16,0xffb4b3a4);}',f'for(var s:menu.slots){W}.slot(g,x,y,s);'))
edit('EquipmentRepairScreen',lambda s:s.replace('g.fill(leftPos,topPos,leftPos+176,topPos+170,0xff34454b);',f'{W}.frame(g,leftPos,topPos,176,170,false);').replace('for(var s:menu.slots)g.fill(leftPos+s.x-1,topPos+s.y-1,leftPos+s.x+17,topPos+s.y+17,0xff17272c);',f'for(var s:menu.slots){W}.slot(g,leftPos,topPos,s);'))
for name in ('EquipmentSkillScreen','SpellScreen'):
 edit(name,frame_simple)
edit('PillChoiceScreen',lambda s:frame_simple(s).replace('g.fill(leftPos+1,topPos+1,leftPos+imageWidth-1,topPos+26,0xFF354641);',''))
edit('GongfaScreen',lambda s:s.replace('graphics.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff514337);\n        graphics.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xff302820);',f'{W}.frame(graphics,leftPos,topPos,imageWidth,imageHeight,false);'))
edit('InstanceScreen',lambda s:frame_simple(s,True).replace('g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xffe4d9bf);',''))
edit('TownMerchantScreen',lambda s:s.replace('g.fill(leftPos,topPos,leftPos+300,topPos+236,0xff26332f);g.fill(leftPos+3,topPos+3,leftPos+297,topPos+233,0xffe3d6ba);g.fill(leftPos+8,topPos+24,leftPos+292,topPos+138,0xffcdc2a7);',f'{W}.frame(g,leftPos,topPos,300,236,true);').replace('for(var slot:menu.slots){int x=leftPos+slot.x-1,y=topPos+slot.y-1;g.fill(x,y,x+18,y+18,0xff847d6c);g.fill(x+1,y+1,x+17,y+17,0xffb6ac94);}',f'for(var slot:menu.slots){W}.slot(g,leftPos,topPos,slot);'))
edit('AppraisalScreen',lambda s:s.replace('g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff263638);g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xff52686a);',f'{W}.frame(g,leftPos,topPos,imageWidth,imageHeight,false);').replace('for(var slot:menu.slots){int sx=leftPos+slot.x,sy=topPos+slot.y;g.fill(sx-1,sy-1,sx+17,sy+17,0xff1b292a);g.fill(sx,sy,sx+16,sy+16,0xff829193);}',f'for(var slot:menu.slots){W}.slot(g,leftPos,topPos,slot);'))
edit('FormationScreen',lambda s:s.replace('graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF25302E);\n        graphics.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFF4C5146);',f'{W}.frame(graphics,x,y,imageWidth,imageHeight,false);').replace('graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF9D9B7D);\n            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF262923);',f'{W}.slotAt(graphics,sx,sy);'))
edit('SmeltingScreen',lambda s:s.replace('g.fill(x,y,x+imageWidth,y+imageHeight,0xff17272b);',f'{W}.frame(g,x,y,imageWidth,imageHeight,false);'))
edit('SaplingGrowthScreen',lambda s:frame_simple(s).replace('g.fill(leftPos+5,topPos+5,leftPos+imageWidth-5,topPos+imageHeight-5,0xff414f40);',''))
edit('BlueprintLearningScreen',lambda s:s.replace('g.fill(leftPos,topPos,leftPos+280,topPos+125,0xff26332f);g.fill(leftPos+3,topPos+3,leftPos+277,topPos+122,0xffe3d6ba);',f'{W}.frame(g,leftPos,topPos,280,125,true);'))
print('13 screens skinned; Alchemy/Refining share WorkstationGui: total 15')
