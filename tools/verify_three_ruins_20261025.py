"""Regressions for the reported bad planting, disconnected steps and cut hull."""
from pathlib import Path
import json
from collections import deque
from package_ruins_20261024 import read
P=Path(__file__).resolve().parents[1];D=P/'src/main/resources/data/xiantu'
def load(id):
    b=json.loads((D/f'ruin_blueprints/{id}.json').read_text(encoding='utf8'))
    _,cells=read(D/f'structures/ruins/{id}.nbt')
    return b,{tuple(c[:3]):c[3]for c in cells}
b,m=load('guiyuan_herb_garden');herbs=0
for (x,y,z),s in m.items():
    if not s.startswith('xiantu:herb_'):continue
    herbs+=1;assert m.get((x,y-1,z))=='minecraft:grass_block',('unsupported herb',x,y,z)
    assert not any(r['bounds'][0]<=x<=r['bounds'][3]and r['bounds'][2]<=z<=r['bounds'][5]for r in b['rooms']),('indoor herb',x,y,z)
assert herbs>=30,('herb garden accidentally emptied',herbs)
for r in b['rooms']:
    x,y,z,X,_,Z=r['bounds']
    assert all(m.get((a,y,c))!='minecraft:grass_block'for a in range(x,X+1)for c in range(z,Z+1))
b,m=load('ninefold_star_observatory')
occupied={p for p,s in m.items()if s!='minecraft:air'};seen={(50,20,46)};q=deque(seen)
while q:
    x,y,z=q.popleft()
    for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:
        p=(x+dx,y+dy,z+dz)
        if p in occupied and p not in seen:seen.add(p);q.append(p)
assert occupied==seen,('detached observatory blocks',list(occupied-seen)[:10])
for (x,y,z),s in m.items():
    if (s.startswith('minecraft:quartz_stairs')or s.startswith('xiantu:cloud_jade_smooth_stairs'))or s.startswith('xiantu:ruin_star_dial'):
        assert m.get((x,y-1,z),'minecraft:air')!='minecraft:air',(x,y,z,'unsupported astronomy stair/instrument')
b,m=load('skyship_wreck')
def radius(x,y):return max(1,min(16,int((x-2)*.85),int((82-x)*.85))-__import__('math').ceil((13-y)*.28))
for x in range(3,82):
    for y in range(1,14):
        r=radius(x,y)
        for z in range(19-r,20+r):
            shell=y<=4 or abs(z-19)>=r-1 or x in (3,81)
            if shell:assert m.get((x,y,z),'minecraft:air')!='minecraft:air',('open hull',x,y,z)
for (x,y,z),s in m.items():
    if 1<=y<=13 and s!='minecraft:air':assert 3<=x<=81 and abs(z-19)<=radius(x,y),('protruding lower hull',x,y,z)
assert all(m.get((x,13,z),'minecraft:air')=='minecraft:air'for x in range(58,65)for z in range(11,14)), 'deck hatch closed'
print(f'Three ruin structural regressions PASS: {herbs} outdoor supported herbs; observatory fully connected; complete tapered hull and retained hatch')
