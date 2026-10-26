from pathlib import Path
import json,hashlib
from PIL import Image
from held_item_edge_contract import verify_icon
from revise_resources_20261020 import ROOT,OUT,MINERALS,BEASTS

ASSETS=ROOT/'src/main/resources/assets/xiantu'
DATA=ROOT/'src/main/resources/data'
checks=0
def check(v,label):
 global checks
 assert v,label
 checks+=1
def read(p):return json.loads(p.read_text(encoding='utf-8'))
for row in read(OUT/'integrated-plants.json'):
 ident=row['id'];heights=[];hashes=[]
 for age,record in enumerate(row['stages']):
  p=ASSETS/f'textures/block/{ident}_stage{age+1}.png';im=Image.open(p)
  check(im.size==(64,64) and im.mode=='RGBA','64 RGBA plant')
  bounds=im.getchannel('A').getbbox();check(bounds is not None and bounds[3]==64,'plant touches baseline')
  check((bounds[0]+bounds[2])//2 in (31,32),'centered silhouette')
  heights.append(bounds[3]-bounds[1]);hashes.append(hashlib.sha256(p.read_bytes()).hexdigest())
  check(hashes[-1]==record['sha256'],'runtime equals reviewed revision')
 check(len(set(hashes))==4,'four unique stages')
 check(all(a<b for a,b in zip(heights,heights[1:])),'four increasing visible heights')
 check(hashlib.sha256((ASSETS/f'textures/block/{ident}_wild.png').read_bytes()).hexdigest()==hashes[3],'wild mature stage')
for row in read(OUT/'integrated-materials.json'):
 folder='block' if row['kind']=='ore' else 'item';p=ASSETS/f'textures/{folder}/{row["id"]}.png';im=Image.open(p)
 check(im.size==(64,64) and im.mode=='RGBA','64 RGBA material')
 if row['kind']=='ore':check(hashlib.sha256(p.read_bytes()).hexdigest()==row['sha256'],'accepted ore copied intact')
 else:verify_icon(p,row['sha256'],ROOT)
 if row['kind']=='ore':check(im.getchannel('A').getextrema()==(255,255),'opaque ore')
 else:check(im.getchannel('A').getextrema()==(0,255),'real icon alpha')
identifiable=read(DATA/'xiantu/tags/items/identifiable_materials.json')['values']
for ident,name,lo,hi,ch,mn,mx,habitat,desc in MINERALS:
 ore='lodestone_ore' if ident=='lodestone_shard' else ident+'_ore'
 loot=read(DATA/f'xiantu/loot_tables/blocks/{ore}.json')
 check(loot['pools'][0]['rolls']==1 and loot['pools'][0]['entries']==[{'type':'minecraft:item','name':'xiantu:'+ident}],'one material no silk/fortune override')
 placed=read(DATA/f'xiantu/worldgen/placed_feature/{ore}.json')
 check(len(placed['placement'])==3 and all(p['type'] not in ('minecraft:count','minecraft:rarity_filter') for p in placed['placement']),'single feature origin with one internal chance roll')
 check('xiantu:'+ident in identifiable,'mineral ingredient appraisal')
 check('xiantu:'+ore not in identifiable,'ore block not appraisable')
 for suffix in ('_powder','_raw','_ingot','_block'):check(not (ASSETS/f'models/item/{ident}{suffix}.json').exists(),'no invented processing forms')
for ident,_,_ in BEASTS:check('xiantu:'+ident in identifiable,'beast auxiliary appraisal')
source=(ROOT/'src/main/java/com/longju/xiantu/terrain/TerrainWorldgenRegistration.java').read_text(encoding='utf-8')
check('SurfaceRules.abovePreliminarySurface()' in source,'underground floor excluded from surface replacement')
for tier in range(7):check(read(ASSETS/f'models/block/spirit_farmland_{tier}.json')['parent']=='minecraft:block/farmland','farmland visual and collision height agree')
print(f'Resource revision: {checks} checks passed')
