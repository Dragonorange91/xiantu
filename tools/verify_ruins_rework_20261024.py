"""Runtime NBT invariants and topology containment, independent of authoring functions."""
import json,re,zipfile
from pathlib import Path
import numpy as np
from scipy.ndimage import label
from package_ruins_20261024 import read
P=Path(__file__).resolve().parents[1];D=P/'src/main/resources/data/xiantu';A=P/'src/main/resources/assets/xiantu'
from preview_realm_architecture import state_models,model,resolve_tex,resource
bundles={}
for p in sorted((D/'ruin_blueprints').glob('*.json')):
 b=json.loads(p.read_text(encoding='utf8'));size,blocks=read(D/f'structures/ruins/{p.stem}.nbt');assert size==b['size'];cells={tuple(v[:3]):v[3]for v in blocks};bundles[b['id']]=(b,cells)
 assert all(0<=c[i]<size[i]for c in blocks for i in range(3)),p.stem
 for m in b['markers']:
  if m['type']=='reward':
   assert cells.get(tuple(m['position']),'').startswith('xiantu:ruin_reward_chest'),(p.stem,m['id'],'missing chest')
   assert (D/f'loot_tables/{m["loot"].split(":")[1]}.json').exists()
 for e in b['encounters']:assert cells.get(tuple(e['position']))=='xiantu:ruin_encounter_seal',(p.stem,e['id'])
 if b['placement']['mode']=='underground':assert not b['placement'].get('artificialSurfaceEntrance',True)
 for s in set(cells.values()):
  if not s.startswith('xiantu:'):continue
  vs=state_models(s);assert vs,(p.stem,'invalid blockstate',s)
  for v in vs:
   m=model(v['model']);assert m,(p.stem,v)
   for e in m.get('elements',[]):
    for face in e['faces'].values():
     t=resolve_tex(face['texture'],m['textures']);ns,name=t.split(':');resource(ns,'textures',name+'.png')
def sealed(id,gate,points):
 b,c=bundles[id];w,h,d=b['size'];free=np.ones((w,h,d),dtype=bool)
 for (x,y,z),s in c.items():free[x,y,z]=s.startswith('minecraft:air')or s.startswith('minecraft:water')
 for x,y,z in gate:free[x,y,z]=False
 labels,n=label(free);outer=set(np.unique(np.concatenate([labels[0].ravel(),labels[-1].ravel(),labels[:,0].ravel(),labels[:,-1].ravel(),labels[:,:,0].ravel(),labels[:,:,-1].ravel()])))
 for p in points:assert labels[tuple(p)]not in outer and labels[tuple(p)]!=0,(id,p,'second exterior opening')
b,c=bundles['water_curtain_caverns_grand']
sealed(b['id'],[(x,y,110)for x in range(76,85)for y in range(16,25)],[[80,25,46],[43,25,60],[120,25,65],[68,25,85],[102,25,90],[80,10,50],[60,10,90]])
assert any(v.get('feature')=='minecraft:oak'for v in b['vegetation'])
assert not any('moss_block'in s or s.endswith('_leaves')or'log['in s for s in c.values()),'Mountain uses actual vanilla features, not assembled trees'
sealed('water_palace',[(x,y,96)for x in range(49,52)for y in range(3,5)],[[50,8,50],[24,8,53],[72,8,55]])
assert bundles['water_palace'][0]['placement']['mode']=='underwater'
assert 'ruins_underwater'in json.loads((D/'worldgen/structure/water_palace.json').read_text())['biomes']
assert bundles['earthvein_mine_palace'][0]['size'][0]>=160 and bundles['taixu_tomb_passage'][0]['size'][0]>=140
assert bundles['xuantian_ruined_city'][0]['size'][0]>=200
spec,c=bundles['skyship_wreck'];cabin=next(r for r in spec['rooms']if r['name']=='紧凑航图舱')['bounds'];assert cabin[3]-cabin[0]<=20
# Every newly exported furniture collision equals the shipped model, all UVs legal.
manifest=json.loads((D/'xian_furniture.json').read_text(encoding='utf8'));registry=(P/'src/main/java/com/longju/xiantu/building/XianFurniture.java').read_text(encoding='utf8')
for f in manifest['models']:
 id=f['id'];m=json.loads((A/f'models/block/{id}.json').read_text());assert np.allclose(sorted(e['from']+e['to']for e in m['elements']),sorted(f['boxes']),rtol=0,atol=1e-6),id
 assert id in registry
 for e in m['elements']:
  assert all(0<=a<b<=16*d for a,b,d in zip(e['from'],e['to'],f['dimensions'])),id
  for face in e['faces'].values():assert all(0<=v<=16 for v in face['uv']),id
 # A multicell block renders each source cube once, shifted to its cell origin.
 if f['dimensions']!=[1,1,1]:
  bs=json.loads((A/f'blockstates/{id}.json').read_text())['variants'];parts=[]
  assert len(bs)==4*np.prod(f['dimensions']),id
  for key,v in bs.items():
   if not key.startswith('facing=north'):continue
   offset=[int(t)*16 for t in key.split('part=')[1].split('_')]
   mm=json.loads((A/('models/'+v['model'].split(':')[1]+'.json')).read_text())
   for e in mm['elements']:parts.append([a+offset[i]for i,a in enumerate(e['from'])]+[a+offset[i]for i,a in enumerate(e['to'])])
  assert np.allclose(sorted(parts),sorted(f['boxes']),atol=1e-6,rtol=0),id
print('20 runtime blueprints: containment, resources and 11 reference furniture models PASS')
furniture={f['id']:f for f in manifest['models']}
stool_height=max(b[4]for b in furniture['xian_stone_stool']['boxes'])
table_height=max(b[4]for b in furniture['xian_round_stone_table']['boxes'])
assert stool_height<=table_height*.55,('stone stool must be clearly lower than table',stool_height,table_height)
chair=json.loads((A/'models/block/xian_wood_chair.json').read_text())
splat=next(e for e in chair['elements']if e.get('name')=='carved_splat')
for side in ['north','south']:
    assert splat['faces'][side]['uv']==[0,0,16,16]
    assert resolve_tex(splat['faces'][side]['texture'],chair['textures'])=='xiantu:block/xian_furniture_chair_carving'
print(f'Chair complete dedicated carving and low stool proportion PASS: {stool_height}/{table_height}')
