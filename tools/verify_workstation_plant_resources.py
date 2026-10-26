from pathlib import Path
import json,hashlib
from PIL import Image
from held_item_edge_contract import verify_icon
from extract_herb_stages import extract_stages
from integrate_large_workstations import ROOT,ASSET,OUT
count=0
def require(v,message):
 global count
 assert v,message
 count+=1
def read(p):return json.loads(p.read_text(encoding='utf-8'))
for m in read(OUT/'workstations.json'):
 ident=m['id'];bs=read(ASSET/f'blockstates/{ident}.json')['variants'];n=8 if m['height']==2 else 2
 require(len(bs)==4*n,'complete four facing states')
 total=0
 for key,value in bs.items():
  p=ASSET/('models/'+value['model'].split(':')[1]+'.json');model=read(p)
  require('format_version' not in model,'1.20.1 native resource')
  for tex in model['textures'].values():
   if tex.startswith('#'):continue
   require((ASSET/('textures/'+tex.split(':')[1]+'.png')).exists(),'texture resolves')
  for cube in model['elements']:
   require(all(-16<=v<=32 for k in ['from','to'] for v in cube[k]),'legal cell coordinates')
   require(all('cullface' not in face for face in cube['faces'].values()),'no neighbour cell face culling')
  if key.startswith('facing=north'):total+=len(model['elements'])
 require(total==len(read(ASSET/f'models/block/workstations/{ident}.json')['elements']),'each cuboid rendered once')
 loot=read(ROOT/f'src/main/resources/data/xiantu/loot_tables/blocks/{ident}.json')
 require(loot['pools'][0]['conditions'][0]['properties']=={'part':'0_0_0'},'single master drop')
for row in read(OUT/'art/plants.json'):
 master=Image.open(ROOT/f'asset_sources/biome-herb-materials-20261020/art/masters/{row["id"]}.png').convert('RGBA')
 expected,extraction=extract_stages(master,row['id'])
 record=read(ROOT/f'asset_sources/biome-herb-materials-20261020/art/extraction/{row["id"]}.json')
 require(record==extraction,'full-source extraction provenance is current')
 require(sum(s['source_pixels'] for s in record['stages'])+record['ignored_speck_pixels']==sum(a>=100 for a in master.getchannel('A').getdata()),'complete source subjects retained before explicit root removal')
 for age in range(1,5):
  p=ASSET/f'textures/block/{row["id"]}_stage{age}.png';im=Image.open(p)
  require(im.size==(64,64),'native runtime size')
  require(im.getchannel('A').tobytes()==expected[age-1].getchannel('A').tobytes(),'full extracted silhouette survives palette reduction')
  bounds=im.getbbox()
  require(bounds and bounds[3]==64 and bounds[0]>=4 and bounds[2]<=60,'complete plant grounded with horizontal margin')
  require(len(im.getcolors(4096))<=49,'intentional restrained palette, no blur gradients')
  require(hashlib.sha256(p.read_bytes()).hexdigest()==row['stages'][age-1]['sha256'],'matches revision')
 require((ASSET/f'textures/block/{row["id"]}_wild.png').read_bytes()==(ASSET/f'textures/block/{row["id"]}_stage4.png').read_bytes(),'wild plant uses complete mature art')
for ident,top in [('herb_0007',437),('herb_0008',485)]:
 record=read(ROOT/f'asset_sources/biome-herb-materials-20261020/art/extraction/{ident}.json')
 require(record['stages'][3]['source_bounds'][1]==top,'regression: mature cap/flower above panel boundary retained')
for i in range(7):
 soil=read(ASSET/f'models/block/spirit_soil_{i}.json');require(soil['textures']['side']!=soil['textures']['top'],'distinct side and top')
 variants=read(ASSET/f'blockstates/spirit_farmland_{i}.json')['variants']
 require(variants['moisture=0']!=variants['moisture=7'],'hydration visually distinct')
 for s in ['top','side','dry','wet']:
  im=Image.open(ASSET/f'textures/block/spirit_soil_{i}_{s}.png')
  require(im.size==(64,64) and im.mode=='RGBA' and im.getchannel('A').getextrema()==(255,255),'soil valid opaque64')
for crop in [False,True]:
 m=read(ASSET/f'models/block/herb_{"crop" if crop else "wild"}_cross.json')
 require(len(m['elements'])==2,'eight triangles per plant')
 require(all(e['from'][1]==(-1 if crop else 0) for e in m['elements']),'grounded on farmland or grass')
core_overrides=read(ASSET/'models/item/demon_core.json')['overrides']
require(len(core_overrides)==54,'all approved core variants have model overrides')
approved=ROOT/'asset_sources/demon-core-approved'
for e,element in enumerate(['metal','wood','water','fire','earth','thunder']):
 for grade in range(1,10):
  ident=f'{element}_core_t{grade}';override=core_overrides[e*9+grade-1]
  require(override=={'predicate':{'xiantu:core_model':e*9+grade},'model':f'xiantu:item/demon_cores/{ident}'},'core model ordering matches item NBT')
  model=read(ASSET/f'models/item/demon_cores/{ident}.json')
  require(model['textures']['layer0']==f'xiantu:item/demon_cores/{ident}','core model resolves to corresponding element and grade')
  texture=ASSET/f'textures/item/demon_cores/{ident}.png'
  verify_icon(texture,hashlib.sha256((approved/f'{ident}.png').read_bytes()).hexdigest(),ROOT)
  require(Image.open(texture).size==(64,64),'native64 core texture')
print(f'Workstation, plant and demon core visual resources: {count} assertions passed')
