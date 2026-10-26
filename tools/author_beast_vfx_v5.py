"""Native Photon graph upgrades with individually authored pixel silhouettes."""
import json,math,shutil,time,argparse
from pathlib import Path
from PIL import Image,ImageDraw
from design_beast_vfx_v5 import BEASTS
from beast_photon_helper import ROOT,call,guard
from author_beast_photon import snbt,number,vector,color
OUT=ROOT/'asset_sources/boss-art-v5-20261018/photon'
RES=ROOT/'src/main/resources/assets/xiantu'
GAME=Path.home()/'Desktop/仙途/.minecraft/versions/1.20.1-Forge_47.4.26'
MANAGED=GAME/'resourcepacks/mcpmod-generated-vfx/assets/xiantu'
PALETTE={'fire':'#FF983F','water':'#8DDEED','earth':'#CAA773','wood':'#A5D878','metal':'#E9CF84','thunder':'#ACA8FF'}

def sprite(rid,element):
 # Native pixel mask: opaque internal facets, clean alpha boundary, no blurred atlas sampling.
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 if 'spider' in rid:
  for i in range(8):
   a=i*math.pi/4;d.line([(32,32),(32+29*math.sin(a),32+29*math.cos(a))],fill=(255,255,255,225),width=2)
  for r in [10,19,28]:d.regular_polygon((32,32,r),8,outline=(210,210,210,220),width=2)
 elif 'mantis' in rid or element=='metal':
  d.polygon([(4,58),(19,29),(37,8),(59,4),(42,24),(29,45)],fill=(205,205,205,230));d.line([(4,58),(37,8),(59,4)],fill='white',width=3)
  d.polygon([(19,29),(37,8),(29,35)],fill=(115,115,115,230))
 elif element=='wood':
  d.polygon([(6,57),(9,30),(28,8),(55,5),(54,34),(33,55)],fill=(190,190,190,235));d.line([(6,57),(45,16)],fill=(255,255,255,255),width=2)
  for x,y in [(20,43),(28,35),(36,27)]:d.line([(x-9,y-8),(x,y),(x+8,y+5)],fill=(225,225,225,225),width=2)
 elif element=='thunder':
  d.line([(40,3),(26,21),(38,28),(19,42),(26,47),(13,61)],fill=(140,140,140,95),width=9)
  d.line([(40,3),(26,21),(38,28),(19,42),(26,47),(13,61)],fill='white',width=3)
  d.line([(31,26),(15,25),(7,35)],fill=(225,225,225,230),width=2)
 elif element=='fire':
  d.polygon([(30,3),(39,18),(36,28),(48,21),(56,40),(48,55),(31,61),(12,51),(9,33),(22,40),(24,22)],fill=(150,150,150,120))
  d.polygon([(30,14),(35,33),(45,30),(44,48),(32,56),(20,47),(23,33)],fill=(240,240,240,220));d.polygon([(31,33),(38,46),(31,52),(25,45)],fill='white')
 elif element=='water':
  d.polygon([(9,51),(16,28),(36,6),(54,5),(39,22),(31,43),(15,58)],fill=(180,180,180,150));d.line([(9,51),(16,28),(36,6),(54,5)],fill='white',width=3)
  d.polygon([(31,43),(39,22),(42,34),(37,49)],fill=(220,220,220,195))
 else:
  d.polygon([(13,14),(40,7),(56,29),(47,53),(20,57),(6,35)],fill=(125,125,125,240));d.polygon([(13,14),(40,7),(30,31),(6,35)],fill=(205,205,205,245));d.line([(13,14),(30,31),(47,53)],fill=(250,250,250,230),width=2)
 if any(k in rid for k in ['wolf','lion','bear','badger']):
  # Three directional claws make close attacks distinguishable from spell motes.
  im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
  for k in range(3):
   dx=k*13;d.polygon([(5+dx,60),(13+dx,24),(20+dx,4),(17+dx,30),(9+dx,55)],fill=(255,255,255,230))
 if any(k in rid for k in ['raven','falcon','vulture','bat']):
  im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
  d.polygon([(10,58),(16,31),(40,6),(54,3),(49,25),(28,50)],fill=(190,190,190,210));d.line([(10,58),(48,10)],fill='white',width=2)
  for i in range(5):d.line([(18+i*5,46-i*6),(27+i*5,46-i*6)],fill=(255,255,255,220),width=2)
 path=RES/'textures/mcp/beasts/v5'/f'{rid}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
 target=MANAGED/'textures/mcp/beasts/v5'/path.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
 return 'xiantu:textures/mcp/beasts/v5/'+path.name

def layer(rid,element,stage,texture,index):
 c=PALETTE[element];impact=stage=='impact';flee=stage=='flee';warn=stage=='warn'
 count=12 if index==0 else 8 if index==1 else 5
 duration=4 if impact else 9 if flee else 16 if warn else 12
 life=10 if index==0 else 15;speed=.13 if impact else .035 if flee else .015
 size=(.30 if impact else .19) if index==0 else .08 if index==1 else .45
 radius=.55 if impact else .25
 cfg={'simulationSpace':'World','duration':duration,'looping':False,'maxParticles':count,
  'startLifetime':number(life),'startSpeed':number(speed),'startSize':{k:number(size) for k in 'xyz'},
  'startColor':{'_type':'Color','number':color(c if index!=1 else '#FFF1CD')},
  'emission':{'emissionRate':number(2 if impact else .7)},
  'shape':{'shape':{'_type':'cone','angle':65. if impact else 25.,'radius':radius,'radiusThickness':.8,'arc':360.}},
  'material':{'cull':False,'depthTest':True,'depthMask':False,'material':{'_type':'TextureMaterial','texture':texture,'discardThreshold':.01}},
  'renderer':{'bloomEffect':False,'shade':False},
  'rotationOverLifetime':{'enable':True,'roll':number((index+1)*.06)},
  'forceOverLifetime':{'enable':True,'simulationSpace':'World','force':{'x':number(0),'y':number(-.012 if impact else .001),'z':number(0)}},
  'colorOverLifetime':{'enable':True,'color':{'_type':'Gradient','a':[0.,0.,.12,1.,.6,.75,1.,0.],'r':[0.,1.,1.,1.],'g':[0.,1.,1.,1.],'b':[0.,1.,1.,1.]}},
  'sizeOverLifetime':{'enable':True,'size':{k:{'_type':'Curve','curves':[[0.,.4,.2,1.,.7,.65,1.,0.]],'defaultValue':1.,'min':0.,'max':1.,'lower':0.,'upper':1.,'lockControlPoint':True,'xAxis':'life','yAxis':'size'} for k in 'xyz'}}}
 if index==2:
  cfg['emission']['emissionRate']=number(.3);cfg['material']['material']['texture']='minecraft:textures/particle/glow.png'
 return {'type':'particle','name':rid+' '+stage+' '+['signature fragments','bright sparks','soft atmosphere'][index],'y':.18 if impact else .65,'config':snbt(cfg)}

def main(only=None):
 guard();OUT.mkdir(parents=True,exist_ok=True);records=[]
 for rid,(name,element,motif,desc) in BEASTS.items():
  if only and rid not in only:continue
  texture=sprite(rid,element)
  paths=sorted((RES/'fx/beasts'/rid).glob('*.fx'))
  for path in paths:
   stage=path.stem
   if stage not in ['warn','trail','impact','projectile','flee']:continue
   backup=OUT/'backup'/rid/path.name;backup.parent.mkdir(parents=True,exist_ok=True)
   if not backup.exists():shutil.copy2(path,backup)
   emitters=[layer(rid,element,stage,texture,i) for i in range(3 if stage!='flee' else 2)]
   # Keep authored gameplay warning outlines, only replace their old generic cloud.
   if stage=='warn':
    old=ROOT/'asset_sources/beast-photon-20261017'/(rid+'-warn.json')
    if not old.exists():old=ROOT/'asset_sources/boss-combat-v4-20261018/photon'/(rid+'-warn.json')
    if old.exists():emitters=[e for e in json.loads(old.read_text())['emitters'] if e['type']=='beam'][:29]+emitters
   doc={'emitters':emitters};(OUT/(rid+'-'+stage+'.json')).write_text(json.dumps(doc,indent=2),encoding='utf8')
   effect='xiantu:beasts/'+rid+'/'+stage
   guard();result=call('photon_write_fx',id=effect,mode='replace' if (MANAGED/'fx/beasts'/rid/path.name).exists() else 'create',document=doc)
   records.append({'id':effect,'motif':motif,'emitters':len(emitters),'write':result})
   shutil.copy2(MANAGED/'fx/beasts'/rid/path.name,path)
  print('AUTHORED',rid,len(paths),flush=True)
 deadline=time.monotonic()+55
 while time.monotonic()<deadline:
  state=call('photon_status')
  if state.get('reload')=='complete':break
  if str(state.get('reload')).startswith('failed'):raise RuntimeError(state)
  time.sleep(.5)
 else:raise TimeoutError('reload')
 call('photon_clear_fx_cache')
 for entry in records:entry['diagnostic']=call('photon_diagnose_fx',id=entry['id'])
 (OUT/('manifest-'+('-'.join(only) if only else 'all')+'.json')).write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf8')
 print('NATIVE_GRAPH_CHECKED',len(records),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--only',nargs='*');args=p.parse_args();main(args.only)
