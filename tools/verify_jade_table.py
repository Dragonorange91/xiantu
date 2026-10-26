"""Check the exported two-cell desk against collision source and UV proportions."""
import json,re
from generate_building_material_resources import ROOT,ASSETS,DATA

def verify():
    load=lambda p:json.loads(p.read_text(encoding='utf-8'));checks=0
    boxes=load(ROOT/'asset_sources/building_materials/jade-table-boxes.json')
    java=(ROOT/'src/main/java/com/longju/xiantu/building/JadeTableBlock.java').read_text(encoding='utf-8')
    assert [list(map(float,v.split(','))) for v in re.findall(r'\{([\d,]+)\}',java)]==boxes
    def cells(box):
        a,b=box[:3],box[3:]
        return {(x,y,z) for x in range(int(a[0]),int(b[0])) for y in range(int(a[1]),int(b[1])) for z in range(int(a[2]),int(b[2]))}
    for part in range(2):
        name=f'cloud_jade_table_part_{part}';model=load(ASSETS/f'models/block/{name}.json');actual=set()
        for e in model['elements']:
            a,b=e['from'],e['to'];actual|=cells(a+b);assert all(0<=v<=16 for v in a+b)
            spans=[b[i]-a[i] for i in range(3)]
            for face,v in e['faces'].items():
                assert v['texture'].removeprefix('#') in model['textures']
                u=v['uv'];axes=(0,1) if face in ('north','south') else (2,1) if face in ('west','east') else (0,2)
                assert abs(u[2]-u[0])==spans[axes[0]] and abs(u[3]-u[1])==spans[axes[1]];checks+=1
        x=(1-part)*16
        expected={(a-x,y,z) for b in boxes for a,y,z in cells(b) if x<=a<x+16}
        assert actual==expected
        assert (ROOT/f'asset_sources/building_materials/models/{name}.bbmodel').exists();checks+=2
    variants=load(ASSETS/'blockstates/cloud_jade_table.json')['variants'];assert len(variants)==8
    for f,r in [('north',0),('east',90),('south',180),('west',270)]:
        for p in range(2):assert variants[f'facing={f},part={p}']=={'model':f'xiantu:block/cloud_jade_table_part_{p}','y':r};checks+=1
    loot=load(DATA/'loot_tables/blocks/cloud_jade_table.json')
    assert any(c.get('properties')=={'part':'0'} for c in loot['pools'][0]['conditions']);checks+=1
    assert load(DATA/'recipes/cloud_jade_table.json')['pattern']==['CCC','S S'];checks+=1
    return checks

if __name__=='__main__':print(verify(),'jade desk resource checks passed')
