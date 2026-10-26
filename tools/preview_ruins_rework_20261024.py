"""Offline actual block-model geometry, texture mean colours. Not game captures."""
from build_paths import review_font
from pathlib import Path
import json,math,sys
from PIL import Image,ImageDraw,ImageFont
from preview_realm_architecture import shapes,rotate
P=Path(__file__).resolve().parents[1];O=P/'asset_sources/ruins-rework-20261024';V=O/'previews';V.mkdir(exist_ok=True)
def render(b,cut=None):
 W,H=1600,1100;w,h,d=b['size'];cy=(cut if cut else h)/2
 cells={tuple(c[:3]):c[3]for c in b['blocks']if c[3]!='minecraft:air'and (cut is None or c[1]<cut)}
 # Orthographic southeast camera, true scale in all three axes.
 span=(w+d)*.72;vertical=(w+d)*.32+(cut or h)
 scale=min((W-100)/span,(H-180)/vertical)
 def pr(x,y,z):return W/2+(x-w/2-z+d/2)*.72*scale,H/2+60+((x-w/2+z-d/2)*.32-(y-cy))*scale
 polys=[]
 for (x,y,z),state in cells.items():
  ss=shapes('minecraft:water' if state.startswith('minecraft:water[') else state)
  for lo,hi,co,rot in ss:
   X,Y,Z=lo;A,B,C=hi;cs=[(X,Y,Z),(A,Y,Z),(A,B,Z),(X,B,Z),(X,Y,C),(A,Y,C),(A,B,C),(X,B,C)]
   for i,p in enumerate(cs):
    if rot.get('element'):
     r=rot['element'];p=rotate(p,r['axis'],r['angle'],tuple(t/16 for t in r['origin']))
    p=rotate(p,'x',-rot.get('x',0));p=rotate(p,'y',-rot.get('y',0));cs[i]=(x+p[0],y+p[1],z+p[2])
   for inds,light,step in [([3,7,6,2],1.12,(0,1,0)),([1,2,6,5],.76,(1,0,0)),([4,5,6,7],.94,(0,0,1))]:
    ns=cells.get((x+step[0],y+step[1],z+step[2]));n=shapes(ns)if ns else []
    if n and len(n)==1 and n[0][0]==(0,0,0)and n[0][1]==(1,1,1):continue
    pts=[cs[i]for i in inds];depth=sum(a+c+v*.3 for a,v,c in pts)/4
    polys.append((depth,[pr(*v)for v in pts],tuple(min(255,int(v*light))for v in co)))
 im=Image.new('RGB',(W,H),'#192b32');dr=ImageDraw.Draw(im)
 for _,pts,co in sorted(polys,key=lambda p:p[0]):dr.polygon(pts,fill=co)
 dr.text((30,18),b.get('name',b['id'])+'  '+str(b['size'])+('  剖切'if cut else ''),font=review_font(30),fill='#ecd9af')
 dr.text((30,H-40),'离线方块模型几何预览 · 材质均色 · 不是 Minecraft 截图',font=review_font(20),fill='#bcced2')
 path=V/(b['id']+(f'-cut{cut}'if cut else '')+'.png');im.save(path);print(path.name,flush=True)
def plan(b,y):
 w,h,d=b['size'];im=Image.new('RGB',(w,d),'#112029');px=im.load()
 for x,Y,z,s in b['blocks']:
  if Y!=y:continue
  px[x,z]=(52,77,88)if s=='minecraft:air'else(225,177,83)if'reward_chest'in s else(206,75,68)if'encounter_seal'in s else(122,131,137)
 im.resize((w*6,d*6),Image.Resampling.NEAREST).save(V/(b['id']+f'-plan{y}.png'))
if __name__=='__main__':
 for p in sorted((O/'structures').glob('*.json')):
  if len(sys.argv)>1 and p.stem not in sys.argv[1:]:continue
  b=json.loads(p.read_text(encoding='utf8'));render(b)
  cuts={'earthvein_mine_palace':21,'taixu_tomb_passage':13,'water_curtain_caverns_grand':25,'water_palace':10,'xuantian_ruined_city':8,'skyship_wreck':8,'cinnabar_seal_tower':12,'ancient_cultivator_cave':7}
  if p.stem in cuts:render(b,cuts[p.stem]);plan(b,cuts[p.stem]-1)
