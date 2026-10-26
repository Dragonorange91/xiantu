"""Validate exported trough silhouettes, seams and native state routing."""
import json
from build_stone_water_channel import boxes
from generate_building_material_resources import ROOT,ASSETS,DATA

def verify():
    checks=0
    state=json.loads((ASSETS/'blockstates/spring_stone_water_channel.json').read_text())['variants']
    assert len(state)==16;checks+=1
    for mask in range(16):
        name=f'spring_stone_water_channel_{mask}'
        model=json.loads((ASSETS/f'models/block/{name}.json').read_text())
        source=json.loads((ROOT/f'asset_sources/building_materials/models/{name}.bbmodel').read_text())
        assert len(model['elements'])==len(source['elements'])==len(boxes(mask));checks+=1
        for e,(n,lo,hi,tex) in zip(model['elements'],boxes(mask)):
            assert e['from']==lo and e['to']==hi;checks+=1
            for face,d in e['faces'].items():
                assert model['textures'][d['texture'][1:]]==f'xiantu:block/spring_stone_{tex}'
                axes=(0,1) if face in ('north','south') else (2,1) if face in ('east','west') else (0,2)
                u,v,uu,vv=d['uv'];assert (abs(uu-u),abs(vv-v))==tuple(hi[a]-lo[a] for a in axes);checks+=1
                if 'cullface' in d:
                    axis,k,limit={'north':(2,'from',0),'south':(2,'to',16),'west':(0,'from',0),
                                  'east':(0,'to',16),'down':(1,'from',0),'up':(1,'to',16)}[face]
                    assert e[k][axis]==limit;checks+=1
        # Central depression is hollow, with floor eight units tall.
        assert not any(lo[0]<=8<hi[0] and lo[1]<=12<hi[1] and lo[2]<=8<hi[2] for _,lo,hi,_ in boxes(mask));checks+=1
        for bit,(x,z) in [(1,(8,.5)),(2,(15.5,8)),(4,(8,15.5)),(8,(.5,8))]:
            blocked=any(lo[0]<=x<hi[0] and lo[1]<=12<hi[1] and lo[2]<=z<hi[2] for _,lo,hi,_ in boxes(mask))
            assert blocked==(not bool(mask&bit));checks+=1
    recipe=json.loads((DATA/'recipes/spring_stone_water_channel.json').read_text())
    assert recipe['pattern']==['S S','SCS'] and recipe['result']['count']==4;checks+=1
    assert json.loads((ASSETS/'models/item/spring_stone_water_channel.json').read_text())=={'parent':'xiantu:block/spring_stone_water_channel_0'};checks+=1
    return checks

if __name__=='__main__':print(verify(),'water channel resource checks passed')
