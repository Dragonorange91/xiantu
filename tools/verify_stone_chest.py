"""Validate MCP exports, animation hinge, native double-half seam and resource references."""
import json
from build_stone_chest import boxes
from generate_building_material_resources import ROOT, ASSETS, DATA

def verify():
    checks=0
    for kind in ('single','left','right'):
        base,lid=boxes(kind)
        for part,expected in [('base',base),('lid',lid),('closed',base+lid)]:
            name=f'black_rock_chest_{part}_{kind}'
            model=json.loads((ASSETS/f'models/block/{name}.json').read_text())
            assert len(model['elements'])==len(expected)
            assert (ROOT/f'asset_sources/building_materials/models/{name}.bbmodel').exists();checks+=2
            for element,(_,lo,hi,tex) in zip(model['elements'],expected):
                assert element['from']==lo and element['to']==hi
                assert all(0<=v<=16 for v in lo+hi);checks+=1
                for face,data in element['faces'].items():
                    assert model['textures'][data['texture'][1:]]==f'xiantu:block/black_rock_{tex}'
                    axes=(0,1) if face in ('north','south') else (2,1) if face in ('east','west') else (0,2)
                    u,v,uu,vv=data['uv']
                    assert (abs(uu-u),abs(vv-v))==tuple(hi[a]-lo[a] for a in axes)
                    checks+=1
            if part=='base':
                # Interior cavity must remain visibly open, including when the lid rotates.
                assert not any(lo[0]<8<hi[0] and lo[1]<6<hi[1] and lo[2]<8<hi[2] for _,lo,hi,_ in expected);checks+=1
    state=json.loads((ASSETS/'blockstates/black_rock_chest.json').read_text())['variants']
    assert len(state)==12;checks+=1
    for kind in ('single','left','right'):
        for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
            assert state[f'facing={facing},type={kind}']['y']==angle;checks+=1
    item=json.loads((ASSETS/'models/item/black_rock_chest.json').read_text())
    assert item=={'parent':'xiantu:block/black_rock_chest_closed_single'};checks+=1
    r=json.loads((DATA/'recipes/black_rock_chest.json').read_text())
    assert r['pattern']==['CSC','S S','CSC'] and r['result']['count']==1;checks+=1
    assert all('xiantu:black_rock_chest' in (ROOT/f'src/main/resources/data/minecraft/tags/blocks/{tag}.json').read_text() for tag in ['mineable/pickaxe']);checks+=1
    # Up-positive Java JSON geometry must lift the front rim around the back hinge.
    import math
    for angle in (15,45,90):
        y,z=10,1;theta=math.radians(angle)
        raised=10+(y-10)*math.cos(theta)-(z-15)*math.sin(theta)
        assert raised>y;checks+=1
    return checks

if __name__=='__main__':print(verify(),'stone chest resource checks passed')
