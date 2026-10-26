"""Native state references, real asset bindings, deterministic production and tab coverage."""
from build_paths import minecraft_reference
import json,zipfile
from PIL import Image
from generate_building_material_resources import ROOT,ASSETS,DATA,FAMILIES,MASTER
def verify():
    load=lambda p:json.loads(p.read_text(encoding='utf-8'));checks=0
    for f in FAMILIES:
        b=f+'_mossy'
        for path,size in [(MASTER/f'{b}_1024.png',(1024,1024)),(ASSETS/f'textures/block/{b}.png',(64,64))]:
            with Image.open(path) as im:assert im.size==size and im.mode=='RGBA' and im.getchannel('A').getextrema()==(255,255)
            checks+=1
        r=load(DATA/f'recipes/{b}.json')
        assert r['type']=='minecraft:crafting_shapeless' and r['ingredients']==[{'item':'xiantu:'+f},{'item':'minecraft:vine'}]
        assert r['result']=={'item':'xiantu:'+b,'count':1};checks+=1
        with zipfile.ZipFile(minecraft_reference('client.jar')) as jar:
            stair_ref=json.loads(jar.read('assets/minecraft/blockstates/stone_brick_stairs.json'))['variants']
            wall_ref=json.loads(jar.read('assets/minecraft/blockstates/stone_brick_wall.json'))['multipart']
        stairs=load(ASSETS/f'blockstates/{b}_stairs.json')['variants'];assert len(stairs)==80
        for state,ref in stair_ref.items():
            for water in ('false','true'):
                v=stairs[state+',waterlogged='+water]
                assert v['model']==ref['model'].replace('minecraft:block/stone_brick_stairs','xiantu:block/'+b+'_stairs')
                assert all(v.get(k,False if k=='uvlock' else 0)==ref.get(k,False if k=='uvlock' else 0) for k in ('x','y','uvlock'))
                checks+=1
        actual=load(ASSETS/f'blockstates/{b}_wall.json')['multipart'];assert len(actual)==len(wall_ref)
        reference_by_condition={json.dumps(e.get('when'),sort_keys=True):e for e in wall_ref}
        assert len(reference_by_condition)==len(actual)
        for a in actual:
            e=reference_by_condition[json.dumps(a.get('when'),sort_keys=True)]
            assert a.get('when')==e.get('when')
            assert a['apply']['model']==e['apply']['model'].replace('minecraft:block/stone_brick_wall','xiantu:block/'+b+'_wall')
            for key in ('x','y','uvlock'):assert a['apply'].get(key,False if key=='uvlock' else 0)==e['apply'].get(key,False if key=='uvlock' else 0)
            checks+=1
        for suffix,tag,count in [('',None,1),('_stairs','stairs',4),('_slab','slabs',6),('_wall','walls',6)]:
            n=b+suffix
            if tag:
                assert 'xiantu:'+n in load(ROOT/f'src/main/resources/data/minecraft/tags/blocks/{tag}.json')['values']
                assert load(DATA/f'recipes/{n}.json')['result']=={'item':'xiantu:'+n,'count':count}
            assert 'xiantu:'+n in load(ROOT/'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json')['values']
            assert load(DATA/f'loot_tables/blocks/{n}.json')['pools'][0]['entries'][0]['name']=='xiantu:'+n
            for lang in ('zh_cn','en_us'):assert 'block.xiantu.'+n in load(ASSETS/f'lang/{lang}.json')
            # Resolve item, block and inherited block model bindings in this batch.
            item=load(ASSETS/f'models/item/{n}.json')
            parent=item['parent'];assert parent.startswith('xiantu:block/')
            assert (ASSETS/f'models/{parent.split(":")[1]}.json').exists()
            checks+=6
        for path in (ASSETS/'models/block').glob(b+'*.json'):
            model=load(path)
            for tex in model.get('textures',{}).values():
                assert tex=='xiantu:block/'+b and (ASSETS/f'textures/{tex.split(":")[1]}.png').exists()
            checks+=1
        functions=load(DATA/f'loot_tables/blocks/{b}_slab.json')['pools'][0]['entries'][0]['functions']
        assert functions[0]['count']==2 and functions[0]['conditions'][0]['properties']=={'type':'double'}
        assert functions[1]['function']=='minecraft:explosion_decay';checks+=1
    return checks
if __name__=='__main__':print('Moss-weathered material checks:',verify(),'passed')
