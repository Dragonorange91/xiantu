"""Repair the three retained grand layouts; dress all new interiors deterministically.
The new layouts are never passed through the retired universal side-wing scripts.
"""
from pathlib import Path
import json,math
P=Path(__file__).resolve().parents[1];O=P/'asset_sources/ruins-rework-20261024'
def dump(p,v):p.write_text(json.dumps(v,ensure_ascii=False),encoding='utf8')
for bid in ['fallen_cultivator_necropolis','five_elements_prison_grand','starfall_sanctuary_grand']:
    b=json.loads((O/'baseline'/f'{bid}.json').read_text(encoding='utf8'));cells={tuple(c[:3]):c[3]for c in b.pop('blocks')}
    def put(x,y,z,s):cells[x,y,z]=s
    def get(x,y,z):return cells.get((x,y,z),'minecraft:air')
    for roof in b['roofs']:
        x,y,z,X,Y,Z=roof['bounds']
        # Continuous perimeter ties and wall-top infill beneath the original tiles.
        for xx in range(x+1,X):
            for zz in [z+1,Z-1]:
                put(xx,y-1,zz,'minecraft:stripped_dark_oak_log[axis=x]')
        for zz in range(z+1,Z):
            for xx in [x+1,X-1]:put(xx,y-1,zz,'minecraft:stripped_dark_oak_log[axis=z]')
        for xx in [x+2,X-2]:
            for zz in [z+2,Z-2]:
                for yy in range(y-2,max(0,y-16),-1):
                    if get(xx,yy,zz)!='minecraft:air':break
                    put(xx,yy,zz,'minecraft:stripped_dark_oak_log[axis=y]')
    # Closed rooms receive wood furnishings at real floor height only, not highest roof surfaces.
    for room in b['rooms']:
        x,y,z,X,Y,Z=room['bounds'];fy=y+1
        for xx,zz,mat in [(x+2,z+2,'xian_bookshelf'),(X-2,z+2,'xian_wood_cabinet'),(x+2,Z-2,'xian_display_shelf')]:
            if get(xx,fy,zz)=='minecraft:air'and get(xx,fy-1,zz)!='minecraft:air':put(xx,fy,zz,f'xiantu:{mat}[facing=south]')
    # Move any exposed main-route treasure into a nearby reachable room corner.
    for m in b['markers']:
        if m['type']!='reward':continue
        pos=m['position'];x,y,z=pos
        candidates=[]
        for room in b['rooms']:
            a,f,c,A,F,C=room['bounds']
            if not(f<=y<=F):continue
            for xx in [a+2,A-2]:
                for zz in [c+2,C-2]:
                    if abs(xx-x)+abs(zz-z)>38:continue
                    dz=1 if zz<c+(C-c)/2 else -1
                    if all(get(xx,y+k,zz)=='minecraft:air'and get(xx,y+k,zz+dz)=='minecraft:air'for k in range(3))and get(xx,y-1,zz)not in ['minecraft:air','minecraft:water']and get(xx,y-1,zz+dz)not in ['minecraft:air','minecraft:water']:
                        candidates.append((abs(xx-x)+abs(zz-z),xx,zz,dz))
        if candidates:
            _,xx,zz,dz=min(candidates);put(x,y,z,'minecraft:air');f='south'if dz>0 else 'north';put(xx,y,zz,f'xiantu:ruin_reward_chest[facing={f},type=single,waterlogged=false]');m['position']=[xx,y,zz];m['approach']=[xx,y,zz+dz]
    b['blocks']=[[*k,v]for k,v in cells.items()];b['revision']='grand-furnishings-20261024';b['placement']['artificialSurfaceEntrance']=False;dump(O/'structures'/f'{bid}.json',b)
print('Retained three grand topologies; repaired roof ties, wood furnishings and hidden chests.')
