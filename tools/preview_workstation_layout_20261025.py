"""Pillow reproduction of real nine-slice/slot geometry; not an in-game capture."""
from build_paths import review_font
from pathlib import Path
import re,json
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources/assets/xiantu'
OUT=ROOT/'asset_sources/workstation-layout-20261025';OUT.mkdir(exist_ok=True,parents=True)
source=(ROOT/'src/main/java/com/longju/xiantu/refining/WorkstationLayout.java').read_text()
vals={k:int(v) for k,v in re.findall(r'(\w+)=(-?\d+)',source)}
W,H=vals['WIDTH'],vals['HEIGHT'];font=review_font(11)
def frame(im,x,y,w,h):
 tex=Image.open(RES/'textures/gui/cultivation/panel.png').convert('RGBA');s=48;b=min(12,min(w,h)//3)
 for xx,yy,ww,hh,box in [(x,y,b,b,(0,0,s,s)),(x+w-b,y,b,b,(256-s,0,256,s)),(x,y+h-b,b,b,(0,256-s,s,256)),(x+w-b,y+h-b,b,b,(256-s,256-s,256,256)),(x+b,y,w-2*b,b,(s,0,256-s,s)),(x+b,y+h-b,w-2*b,b,(s,256-s,256-s,256)),(x,y+b,b,h-2*b,(0,s,s,256-s)),(x+w-b,y+b,b,h-2*b,(256-s,s,256,256-s)),(x+b,y+b,w-2*b,h-2*b,(s,s,256-s,256-s))]:
  patch=tex.crop(box).resize((ww,hh),Image.Resampling.NEAREST);im.alpha_composite(patch,(xx,yy))
def main():
 for kind in ['alchemy','refining']:
  im=Image.new('RGBA',(W,H),(18,26,30,255));frame(im,0,0,W,H)
  for box in [(6,44,152,H-50),(168,44,W-174,172),(168,218,W-174,42),(168,264,184,88),(356,264,W-362,88)]:frame(im,*box)
  bagua=Image.open(RES/'textures/gui/cultivation/bagua.png').convert('RGBA').resize((148,148),Image.Resampling.NEAREST);im.alpha_composite(bagua,(182,57))
  draw=ImageDraw.Draw(im);color=(239,223,183)
  def text(x,y,t):draw.text((x,y),t,font=font,fill=color)
  def center(x,y,t):text(round(x-draw.textlength(t,font=font)/2),y,t)
  draw.line((6,20,W-6,20),fill=color)
  text(8,6,'一阶丹炉' if kind=='alchemy' else '一阶炼器炉')
  for i in range(3):center(34+i*56,25,f'{i+1}阶')
  text(12,48,'搜索丹药' if kind=='alchemy' else '搜索装备名称')
  slot=Image.open(RES/'textures/gui/cultivation/slot.png').convert('RGBA').resize((18,18),Image.Resampling.NEAREST)
  def sl(x,y):im.alpha_composite(slot,(x-1,y-1))
  dx=next(re.findall(r'DX=\{([^}]+)\}',source).__iter__());dy=next(re.findall(r'DY=\{([^}]+)\}',source).__iter__())
  for xx,yy in zip(map(int,dx.split(',')),map(int,dy.split(','))):sl(256+xx,128+yy)
  sl(248,120);sl(186,288);sl(508,112)
  if kind=='alchemy':sl(408,112);center(416,86,'辅助妖丹');center(416,139,'最多8枚')
  else:
   frame(im,354,86,136,18);center(422,88,'熔炼器胚');draw.line((352,134,488,134),fill=color)
  for row in range(3):
   for col in range(9):sl(368+col*18,272+row*18)
  for col in range(9):sl(368+col*18,330)
  center(424,59,'凝气丹 · 一阶' if kind=='alchemy' else '青霜剑 · 一阶')
  text(506,135,'产物');text(184,225,'所需材料');text(184,276,'灵石供能');text(214,291,'储能');text(214,306,'2800');text(184,332,'需 120')
  text(352,178 if kind=='alchemy' else 154,'成丹率 90.0%' if kind=='alchemy' else '成器率 90%')
  for i in range(8):
   x=186+i*30;draw.ellipse((x+3,240,x+12,251),fill=(110+i*14,155,170));text(x+10,246,'3')
  for i in range(15 if kind=='alchemy' else 20):
   x=12+i%5*28;y=(68 if kind=='alchemy' else 90)+i//5*28;sl(x,y)
   draw.ellipse((x+4,y+4,x+12,y+12),fill=(90+i*4,179,197))
  center(43,165 if kind=='alchemy' else 210,'上一页');center(119,165 if kind=='alchemy' else 210,'下一页')
  if kind=='alchemy':center(81,187,'开始炼丹')
  im.resize((W*2,H*2),Image.Resampling.NEAREST).convert('RGB').save(OUT/(kind+'-layout.png'))
  im.crop((168,44,338,214)).resize((680,680),Image.Resampling.NEAREST).convert('RGB').save(OUT/(kind+'-bagua-closeup.png'))
 (OUT/'preview-note.json').write_text(json.dumps({'method':'real textures and source shared slot geometry; font/items are stand-ins','not_game_capture':True,'size':[W,H],'bagua_bounds':[182,54,330,202],'panel_inner_margin':[14,10,12]},indent=2))
if __name__=='__main__':main()
