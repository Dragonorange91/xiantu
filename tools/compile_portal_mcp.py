"""Bake the saved Blockbench model to immutable runtime quads, retaining per-face UVs."""
from pathlib import Path
import json,math
ROOT=Path(__file__).resolve().parents[1]
src=ROOT/'asset_sources/combat-realms-20261026/realm_astral_gate.bbmodel'
data=json.loads(src.read_text('utf8'));quads=[]
for c in data['elements']:
    x,y,z=c['from'];X,Y,Z=c['to'];ox,oy,oz=c.get('origin',[0,0,0])
    a=math.radians(c.get('rotation',[0,0,0])[2]);co,si=math.cos(a),math.sin(a)
    faces={'north':([(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],[0,0,-1]),
           'south':([(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],[0,0,1]),
           'east':([(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],[1,0,0]),
           'west':([(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],[-1,0,0]),
           'up':([(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],[0,1,0]),
           'down':([(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)],[0,-1,0])}
    for name,(vertices,n) in faces.items():
        f=c['faces'][name];uv=f['uv'];texture=data['textures'][f['texture']]['name'].removesuffix('.png')
        pts=[[(ox+(v[0]-ox)*co-(v[1]-oy)*si)/16,(oy+(v[0]-ox)*si+(v[1]-oy)*co)/16,v[2]/16] for v in vertices]
        normal=[n[0]*co-n[1]*si,n[0]*si+n[1]*co,n[2]]
        quads.append({'texture':texture,'emissive':c['name'].startswith('jade'), 'vertices':pts,'normal':normal,'uv':[v/16 for v in uv]})
out=ROOT/'src/main/resources/assets/xiantu/effects/realm_astral_gate.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(quads,separators=(',',':')),encoding='utf8');print('Baked MCP gate:',len(quads),'quads')
