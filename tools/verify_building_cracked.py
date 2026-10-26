"""Check cracked assets against Minecraft's state tables and preserve original recipes."""
from build_paths import minecraft_reference
import json,re,zipfile
from PIL import Image
from generate_building_material_resources import ROOT,ASSETS,DATA,FAMILIES,MASTER

def verify():
    checks=0
    load=lambda p:json.loads(p.read_text(encoding='utf-8'))
    registry=ROOT/'src/main/java/com/longju/xiantu/registry'
    blocks=(registry/'ModBlocks.java').read_text(encoding='utf-8')
    items=(registry/'ModBlockItems.java').read_text(encoding='utf-8')
    tab=(registry/'ModCreativeTabs.java').read_text(encoding='utf-8').split('TABS.register("building"',1)[1].split('public static final RegistryObject<CreativeModeTab>',1)[0]
    for family in FAMILIES:
        base=family+'_cracked'
        with Image.open(MASTER/f'{base}_1024.png') as im:assert im.size==(1024,1024)
        with Image.open(ASSETS/f'textures/block/{base}.png') as im:
            assert im.size==(64,64) and im.mode=='RGBA' and im.getchannel('A').getextrema()==(255,255)
        assert load(DATA/f'recipes/{base}.json')=={'type':'minecraft:smelting','category':'blocks','ingredient':{'item':'xiantu:'+family},'result':'xiantu:'+base,'experience':0.1,'cookingtime':200}
        checks+=3
        for suffix in ('','_stairs','_slab'):
            name=base+suffix;key=name.upper()
            assert f'RegistryObject<Block> {key} =' in blocks
            assert f'building("{name}", ModBlocks.{key})' in items
            assert tab.count(f'output.accept(ModBlockItems.{key}.get());')==1
            assert load(ASSETS/f'models/item/{name}.json')=={'parent':'xiantu:block/'+name}
            assert load(DATA/f'loot_tables/blocks/{name}.json')['pools'][0]['entries'][0]['name']=='xiantu:'+name
            assert 'xiantu:'+name in load(ROOT/'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json')['values']
            for lang in ('zh_cn','en_us'):
                assert 'block.xiantu.'+name in load(ASSETS/f'lang/{lang}.json')
            checks+=8
        for suffix,tag in [('_stairs','stairs'),('_slab','slabs')]:
            assert 'xiantu:'+base+suffix in load(ROOT/f'src/main/resources/data/minecraft/tags/blocks/{tag}.json')['values']
            checks+=1
        with zipfile.ZipFile(minecraft_reference('client.jar')) as jar:
            reference=json.loads(jar.read('assets/minecraft/blockstates/stone_brick_stairs.json'))['variants']
        actual=load(ASSETS/f'blockstates/{base}_stairs.json')['variants']
        assert len(actual)==80
        for state,expected in reference.items():
            for water in ('false','true'):
                value=actual[state+',waterlogged='+water]
                assert value['model']==expected['model'].replace('minecraft:block/stone_brick_stairs','xiantu:block/'+base+'_stairs')
                assert all(value.get(k,False if k=='uvlock' else 0)==expected.get(k,False if k=='uvlock' else 0) for k in ('x','y','uvlock'))
                checks+=1
        functions=load(DATA/f'loot_tables/blocks/{base}_slab.json')['pools'][0]['entries'][0]['functions']
        assert functions[0]['count']==2 and functions[0]['conditions'][0]['properties']=={'type':'double'}
        assert functions[1]['function']=='minecraft:explosion_decay'
        # Catch accidental damage to the existing intact material route.
        assert load(DATA/f'recipes/{family}_stairs.json')['key']['B']['item']=='xiantu:'+family
        assert load(DATA/f'recipes/{base}_stairs.json')['key']['B']['item']=='xiantu:'+base
        assert load(DATA/f'recipes/{base}_stairs.json')['result']['count']==4
        assert load(DATA/f'recipes/{base}_slab.json')['result']['count']==6
        assert load(ASSETS/f'models/block/{base}.json')['textures']['all']=='xiantu:block/'+base
        checks+=6
    names=[n.lower() for n in re.findall(r'RegistryObject<Item> ([A-Z_0-9]+) =',items)
           if n.lower().startswith(tuple(FAMILIES)+('wooden_plaque','town_','wanbaoge_plaque','forging_shop_plaque','alchemy_pavilion_plaque','talisman_shop_plaque','ruin_reward_chest','qunyou_figurine'))]
    assert len(names)==len(set(names))
    listed=re.findall(r'output.accept\(ModBlockItems\.([A-Z_]+)\.get\(\)\);',tab)
    assert len(listed)==len(set(listed)) and set(listed)=={n.upper() for n in names}
    checks+=2
    return checks

if __name__=='__main__':print(f'Cracked building resource checks: {verify()} passed')
