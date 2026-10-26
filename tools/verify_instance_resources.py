import json
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'src/main/resources'
files=list((r/'data/xiantu/dimension').glob('realm_*.json'))+list((r/'data/xiantu/dimension').glob('dongfu_*.json'))
assert len(files)==272
for f in files:
    d=json.loads(f.read_text(encoding='utf-8'));g=d['generator'];assert d['type']=='xiantu:instance' and g['type']=='xiantu:instance'
    assert g['cave']==f.stem.startswith('dongfu_') and g['biome_source']=={'type':'minecraft:fixed','biome':'xiantu:instance'}
b=json.loads((r/'data/xiantu/instance/realm_courts.json').read_text(encoding='utf-8'))
assert b['schema_version']==3 and set(b['layouts'])=={'wood','fire','water','metal','earth','thunder'}
from collections import deque
import hashlib
fingerprints=set();graphs=set()
for theme,layout in b['layouts'].items():
    blocks=layout['blocks'];assert len(blocks)>15000
    assert all(abs(x)<=224 and abs(z)<=240 and -63<=y<=317 for x,y,z,s in blocks)
    assert all((a['x']-c['x'])**2+(a['z']-c['z'])**2>=55**2 for i,a in enumerate(layout['zones']) for c in layout['zones'][i+1:]),(theme,'district spacing')
    assert len(layout['zones'])==6 and len(layout['routes'])>=7
    fingerprints.add(hashlib.sha256(json.dumps(blocks).encode()).hexdigest())
    graphs.add(json.dumps(layout['routes']))
    grid={(x,y,z):s for x,y,z,s in blocks}
    for x,y,z,state in blocks:
        assert ':' in state and not state.startswith('$')
        name=state.split('[')[0]
        if name.startswith('xiantu:'):
            f=r/f'assets/xiantu/blockstates/{name.split(":")[1]}.json';assert f.exists(),name
            # Defaults may be omitted, but supplied properties must exist and be legal.
            variants=json.loads(f.read_text(encoding='utf8')).get('variants',{})
            props=dict(p.split('=') for p in state.partition('[')[2].rstrip(']').split(',') if '=' in p)
            if variants:
                choices=[dict(p.split('=') for p in k.split(',') if '=' in p) for k in variants]
                assert any(all(v.get(k)==value for k,value in props.items()) for v in choices),(theme,'invalid state',state)
        if state=='minecraft:lily_pad':assert grid.get((x,y-1,z))=='minecraft:water',(theme,'lily pad without water',x,y,z)
    assert len(layout['herb_cells'])>=80 and len(layout['ore_cells'])>=96,(theme,'resource capacity')
    for x,y,z in layout['herb_cells']:
        assert grid[x,y-1,z]=='minecraft:grass_block' and grid[x,y,z]=='minecraft:air'
    for x,y,z in layout['ore_cells']:assert grid[x,y,z]=='minecraft:stone'
    def solid(x,y,z):
        state=grid.get((x,y,z),'minecraft:air')
        return state not in ('minecraft:air','minecraft:water') and not any(v in state for v in ('lantern','torch','fence','railing','skull'))
    def walk(p):
        x,y,z=p
        return -224<=x<=224 and -20<=z<=240 and 50<=y<=100 and solid(x,y-1,z) and not solid(x,y,z) and not solid(x,y+1,z)
    seen={(0,65,0)};todo=deque(seen)
    while todo:
        x,y,z=todo.popleft()
        for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
            for dy in (0,1,-1):
                q=(x+dx,y+dy,z+dz)
                if q not in seen and walk(q):seen.add(q);todo.append(q)
    for zone in layout['zones']:
        target=(zone['x'],zone['y'],zone['z'])
        assert target in seen,(theme,'district inaccessible',target)
    for name,at in layout['rewards'].items():
        x,y,z=at;assert at and any((x+dx,y,z+dz) in seen for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]),(theme,'reward inaccessible',name,at)
        assert grid.get((x,y+1,z))=='minecraft:air',(theme,'reward lid blocked',name)
    if layout.get('architecture_revision')==2:
        # Every district has a door aligned to each public approach, not a diagonal
        # late carve through a wall. Floor cells must exist at all four street mouths.
        for zone in layout['zones']:
            x,y,z=zone['x'],zone['y'],zone['z']
            for dx,dz in [(zone['rx'],0),(-zone['rx'],0),(0,zone['rz']),(0,-zone['rz'])]:
                assert (x+dx,y,z+dz) in seen,(theme,'blocked approach',zone['id'],dx,dz)
        assert len([v for v in blocks if v[3]!='minecraft:air' and v[1]>layout['zones'][0]['y']+5])>1000
    print(theme,layout['style'],'reachable',len(seen),'herb beds',len(layout['herb_cells']))
assert len(fingerprints)==len(graphs)==6,'layouts and route coordinates must be independent'
dim=json.loads((r/'data/xiantu/dimension_type/instance.json').read_text());assert dim['min_y']==-64 and dim['height']==384 and not dim['bed_works']
biome=json.loads((r/'data/xiantu/worldgen/biome/instance.json').read_text());assert not biome['spawners'] and not any(biome['features'])
for i in range(1,9):
    t=json.loads((r/f'data/xiantu/loot_tables/instance/material_{i}.json').read_text());assert t['type']=='minecraft:chest'
    assert all('dongfu_token' not in e['name'] and 'inner_demon' not in e['name'] for p in t['pools'] for e in p['entries'])
print(f'Instance resources PASS: {len(files)} fixed dimensions, six independent layouts with traversable districts/rewards, legal herb beds, bounded blueprint, eight material tables')
