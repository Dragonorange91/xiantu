"""Audit the shipped town templates, complete furniture/workstations and legacy tiles."""
from pathlib import Path
import json,re
from package_ruins_20261024 import read

P=Path(__file__).resolve().parents[1]
D=P/'src/main/resources/data/xiantu'
specs={m['id']:m['dimensions'] for m in json.loads((D/'xian_furniture.json').read_text(encoding='utf8'))['models']}
specs.update(qingmu_alchemy_furnace=[2,2,2],lifeng_refining_furnace=[2,2,2],artisan_forging_table=[2,1,1])
def unpack(s):
    return s.split(':')[-1].split('[')[0],dict(re.findall(r'(\w+)=([^,\]]+)',s))
def offset(x,y,z,f):
    X,Z={'north':(x,z),'east':(-z,x),'south':(-x,-z),'west':(z,-x)}[f]
    return X,y,Z
def load(p):
    _,rows=read(p)
    return {tuple(v[:3]):v[3] for v in rows}
assemblies=0
for name in ['alchemy_pavilion','forging_shop','residence_small','talisman_workshop','cultivation_town']:
    cells=load(D/f'structures/town/{name}.nbt')
    for p,s in cells.items():
        id,props=unpack(s)
        if id not in specs:continue
        dims=specs[id]
        if dims==[1,1,1]:continue
        assert 'part' in props,(name,p,id,'missing multicell property')
        part=tuple(map(int,props['part'].split('_')));f=props['facing']
        diff=offset(*part,f);anchor=tuple(p[i]-diff[i] for i in range(3))
        if part!=(0,0,0):
            assert unpack(cells.get(anchor,''))[0]==id,(name,p,'orphan')
            continue
        assemblies+=1
        for x in range(dims[0]):
         for y in range(dims[1]):
          for z in range(dims[2]):
            off=offset(x,y,z,f);at=tuple(anchor[i]+off[i]for i in range(3))
            actual,ap=unpack(cells.get(at,''))
            assert actual==id and ap.get('part')==f'{x}_{y}_{z}' and ap.get('facing')==f,(name,id,at,'incomplete or rotated wrong')
            if y==0:
                under=cells.get((at[0],at[1]-1,at[2]),'minecraft:air')
                assert not any(under.startswith(t)for t in ['minecraft:air','minecraft:water']),(name,id,at,'unsupported')
    if name=='cultivation_town':town=cells
layout=json.loads((D/'town-layout.json').read_text(encoding='utf8'))
for npc in layout['merchants']:
    pos=npc['position'];x,y,z=[int(v//1)for v in pos]
    for dy in [0,1]:
        s=town.get((x,y+dy,z),'minecraft:air')
        assert 'xian_'not in s and 'furnace'not in s and 'forging_table'not in s,(npc,'new furniture blocks merchant',s)
tiles={}
for file in (D/'structures/town/tiles').glob('cell_*.nbt'):
    _,x,y,z=file.stem.split('_');ox,oy,oz=int(x)*16,int(y)*32,int(z)*16
    for (x,y,z),s in load(file).items():tiles[x+ox,y+oy,z+oz]=s
assert tiles==town,('legacy tile content differs',len(tiles),len(town))
print(f'Town PASS: {assemblies} complete supported assemblies, merchant clearances and {len(tiles)} tile cells')
