"""Validate new finishes against vanilla state tables and existing texture bindings."""
from build_paths import minecraft_reference
import json,zipfile
from generate_building_material_resources import ROOT,ASSETS,DATA,FAMILIES

def verify():
    load=lambda p:json.loads(p.read_text(encoding='utf-8'));checks=0
    with zipfile.ZipFile(minecraft_reference('client.jar')) as jar:
        reference=json.loads(jar.read('assets/minecraft/blockstates/stone_brick_stairs.json'))['variants']
    for f in FAMILIES:
        for finish in ('smooth','carved'):
            base=f+'_'+finish
            actual=load(ASSETS/f'blockstates/{base}_stairs.json')['variants'];assert len(actual)==80
            for key,expected in reference.items():
                for water in ('false','true'):
                    value=actual[key+',waterlogged='+water]
                    assert value['model']==expected['model'].replace('minecraft:block/stone_brick_stairs','xiantu:block/'+base+'_stairs')
                    assert all(value.get(k,False if k=='uvlock' else 0)==expected.get(k,False if k=='uvlock' else 0) for k in ('x','y','uvlock'));checks+=1
            for shape,count,tag in [('stairs',4,'stairs'),('slab',6,'slabs')]:
                n=base+'_'+shape;r=load(DATA/f'recipes/{n}.json')
                assert r['key']['B']['item']=='xiantu:'+base and r['result']=={'item':'xiantu:'+n,'count':count}
                assert 'xiantu:'+n in load(ROOT/f'src/main/resources/data/minecraft/tags/blocks/{tag}.json')['values']
                assert load(ASSETS/f'models/item/{n}.json')=={'parent':'xiantu:block/'+n}
                for suffix in ('','_inner','_outer') if shape=='stairs' else ('','_top'):
                    assert set(load(ASSETS/f'models/block/{n}{suffix}.json')['textures'].values())=={'xiantu:block/'+base};checks+=1
                checks+=3
            fn=load(DATA/f'loot_tables/blocks/{base}_slab.json')['pools'][0]['entries'][0]['functions']
            assert fn[0]['count']==2 and fn[0]['conditions'][0]['properties']=={'type':'double'}
            assert load(ASSETS/f'models/block/{base}_slab_double.json')=={'parent':'xiantu:block/'+base};checks+=2
    return checks

if __name__=='__main__':print(verify(),'finished-step resource checks passed')
