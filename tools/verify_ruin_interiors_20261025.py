"""Independent packaged-cell assembly, full-support and functional-route checks."""
from pathlib import Path
import json,sys,collections
import numpy as np
from package_ruins_20261024 import read
P=Path(__file__).resolve().parents[1];A=P/'src/main/resources/assets/xiantu';D=P/'src/main/resources/data/xiantu';O=P/'asset_sources/ruin-interiors-20261025'
def load(p):return json.loads(p.read_text(encoding='utf8'))
checks=0
for id,dims in [('ancient_burial_coffin',[2,2,5]),('equipment_repair_table',[2,2,1])]:
 source=load(O/(id+'.json'));variants=load(A/f'blockstates/{id}.json')['variants'];assert len(variants)==4*np.prod(dims)
 parts=[]
 for key,v in variants.items():
  if not key.startswith('facing=north'):continue
  part=[int(s)for s in key.split('part=')[1].split('_')];m=load(A/('models/'+v['model'].split(':')[1]+'.json'))
  for e in m['elements']:
   for edge in ['from','to']:assert all(-1e-5<=a<=16.00001 for a in e[edge]),(id,key,'illegal cell export',e)
   for face in e['faces'].values():assert all(-1e-5<=a<=16.00001 for a in face['uv']),(id,key,'UV outside tile')
   parts.append([a+part[i]*16 for i,a in enumerate(e['from'])]+[a+part[i]*16 for i,a in enumerate(e['to'])]);checks+=1
 assert np.allclose(sorted(parts),sorted(e['from']+e['to']for e in source['elements']),atol=1e-5,rtol=0),id
 assert load(D/f'loot_tables/blocks/{id}.json')['pools'][0]['conditions'][0]['properties']=={'part':'0_0_0'}
 assert (O/(id+'.bbmodel')).exists()
 # Item geometry must independently fit the legacy Java model bounds.
 item=load(A/f'models/block/{id}.json');assert all(-16<=c<=32 for e in item['elements']for k in ['from','to']for c in e[k])
repair=load(O/'equipment_repair_table.json');assert next(e for e in repair['elements']if e['name'].startswith('worktop_0'))['to'][1]>12
allwaves=0;groups=0;coffins=0
manifest=load(D/'xian_furniture.json')['models'];dimmap={f['id']:f['dimensions']for f in manifest};dimmap.update(ancient_burial_coffin=[2,2,5],equipment_repair_table=[2,2,1])
for f in sorted((D/'ruin_blueprints').glob('*.json')):
 b=load(f);old=load(O/'baseline'/f.name);size,blocks=read(D/f'structures/ruins/{f.stem}.nbt');cells={tuple(c[:3]):c[3]for c in blocks}
 assert all(r.get('furnishingThemes')for r in b['rooms']),(f.stem,'unfurnished room')
 expected_placement=dict(old['placement'])
 if f.stem=='water_curtain_caverns_grand':
  # The stepped waterfall moved the approach onto the covered passage centerline.
  expected_placement['entry']=[80,17,145]
  assert b['waterfall']['entranceBehindFinalFall']==[80,17,140]
  for z in range(111,140):
   for y in range(17,20):assert cells[(80,y,z)]=='minecraft:air'
 assert b['markers']==old['markers']and b['placement']==expected_placement and b['bossPlan']==old['bossPlan'],f.stem+' functional metadata changed'
 if b['bossPlan']:
  assert [e['id']for e in b['encounters'][:len(old['encounters'])]]==[e['id']for e in old['encounters']],f.stem+' original trial order changed'
  if b['bossPlan']['trial']=='elements':assert len(b['encounters'])==5
  for e in b['encounters']:
   x,y,z=e['position'];assert cells[(x,y,z)]=='xiantu:ruin_encounter_seal';assert not cells[(x,y-1,z)].startswith('minecraft:air')
  x,y,z=b['bossPlan']['position'];assert cells.get((x,y,z))=='minecraft:air'
 for pos,s in cells.items():
  id=s.split('[')[0].removeprefix('xiantu:')
  if id not in dimmap or 'part=0_0_0'not in s:continue
  props=dict(t.split('=')for t in s.split('[')[1].rstrip(']').split(','));turn=['north','east','south','west'].index(props['facing']);dims=dimmap[id]
  for a in range(dims[0]):
   for y in range(dims[1]):
    for c in range(dims[2]):
     dx,dz=a,c
     for _ in range(turn):dx,dz=-dz,dx
     at=(pos[0]+dx,pos[1]+y,pos[2]+dz);actual=cells.get(at,'');assert actual.split('[')[0]=='xiantu:'+id and f'part={a}_{y}_{c}'in actual,(f.stem,id,pos,at,'incomplete assembly')
     if y==0:
      floor=cells.get((at[0],at[1]-1,at[2]),'minecraft:stone'if b['placement']['mode']=='underground'else'minecraft:air');assert floor!='minecraft:air'and not floor.startswith('minecraft:water'),(f.stem,id,at,'unsupported')
  checks+=1
 for e in b['encounters']:
  assert 0<=e['minRealm']<=e['maxRealm']<=8;allwaves+=1
 groups+=len(b['interiorDesign']['groups']);coffins+=b['interiorDesign']['distinctVignettes'].get('burial',0)
 assert b['size']==old['size']
assert groups>=300 and coffins>=20
for f in (D/'tags/worldgen/biome/spirit_wood').glob('*.json'):
 assert not {'minecraft:plains','minecraft:sunflower_plains','minecraft:snowy_plains'}&set(load(f)['values']),f.stem
print(f'Ruin interiors PASS: {checks} model/assembly checks, {groups} themed groups, {coffins} coffins, {allwaves} guardian waves; plains excluded')
