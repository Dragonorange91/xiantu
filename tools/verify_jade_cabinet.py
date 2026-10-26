"""Check native MCP exports, UV density, occupancy, resource routing and anchor loot."""
import json
from math import isclose
from build_jade_cabinet import clipped,boxes
from generate_building_material_resources import ROOT,ASSETS,DATA

def verify():
    checks=0
    for opened in (False,True):
        for part in range(4):
            name=f'cloud_jade_cabinet_{"open" if opened else "closed"}_{part}'
            m=json.loads((ASSETS/f'models/block/{name}.json').read_text())
            source=json.loads((ROOT/f'asset_sources/building_materials/models/{name}.bbmodel').read_text())
            expected=clipped(part,opened)
            assert len(m['elements'])==len(expected)==len(source['elements']);checks+=1
            for element,(n,lo,hi,tex) in zip(m['elements'],expected):
                assert element['from']==lo and element['to']==hi;checks+=1
                assert 0<=lo[0]<hi[0]<=16 and 0<=lo[1]<hi[1]<=16 and -16<=lo[2]<hi[2]<=16;checks+=1
                for face,d in element['faces'].items():
                    assert m['textures'][d['texture'][1:]]==f'xiantu:block/cloud_jade_{tex}'
                    axes=(0,1) if face in ('north','south') else (2,1) if face in ('east','west') else (0,2)
                    u,v,uu,vv=d['uv'];assert all(isclose(actual,hi[a]-lo[a],abs_tol=1e-6) for actual,a in zip((abs(uu-u),abs(vv-v)),axes))
                    checks+=1
    variants=json.loads((ASSETS/'blockstates/cloud_jade_cabinet.json').read_text())['variants'];assert len(variants)==32;checks+=1
    for part in range(4):
        for op in ('true','false'):
            for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
                assert variants[f'facing={facing},open={op},part={part}']['y']==angle;checks+=1
    loot=json.loads((DATA/'loot_tables/blocks/cloud_jade_cabinet.json').read_text())
    assert any(c.get('properties')=={'part':'0'} for c in loot['pools'][0]['conditions']);checks+=1
    item=json.loads((ASSETS/'models/item/cloud_jade_cabinet.json').read_text());assert item=={'parent':'xiantu:block/cloud_jade_cabinet'};checks+=1
    m=json.loads((ASSETS/'models/block/cloud_jade_cabinet.json').read_text());assert len(m['elements'])==len(boxes());checks+=1
    assert 'cloud_jade_cabinet' in (ROOT/'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json').read_text();checks+=1
    closed=boxes();opened=boxes(True)
    assert sum(n.startswith('foot_') for n,_,_,_ in closed)==4;checks+=1
    assert sum(n.startswith('drawer_panel_') for n,_,_,_ in closed)==4;checks+=1
    assert sum(n.endswith('_panel') and n.startswith('door_') for n,_,_,_ in closed)==2;checks+=1
    assert any(n.startswith('door_') and lo[2]<0 for n,lo,hi,t in opened);checks+=1
    assert not any(lo[0]<=16<=hi[0] and lo[1]<=1<=hi[1] and lo[2]<=7<=hi[2] for n,lo,hi,t in closed);checks+=1
    return checks

if __name__=='__main__':print(verify(),'jade cabinet resource checks passed')
