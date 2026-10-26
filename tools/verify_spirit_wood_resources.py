"""Verify actual tree assets, recipe uniqueness, model exports and worldgen wiring."""
from build_paths import minecraft_reference
import json,zipfile
from PIL import Image
from spirit_wood_catalog import ROOT,TREES
from build_spirit_wood_resources import BASE,ASSETS,DATA,BUILDING
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def signature(obj,flip=False):
    pattern=obj.get('pattern');key=obj.get('key',{})
    if not pattern:return None
    rows=[tuple(json.dumps(key.get(c,{}),sort_keys=True) for c in (row[::-1] if flip else row)) for row in pattern]
    return tuple(rows)
def main():
    checks=0
    vanilla=zipfile.ZipFile(minecraft_reference('client.jar'))
    known=set(vanilla.namelist())
    registered={f+'_'+s for f,*_ in TREES for s in ['log','stripped_log','planks','leaves','sapling','stick','long_shaft','fiber']+BUILDING}
    assert len(registered)==264
    patterns={}
    for rel in ['forge/biome_modifier/qing_wen_elm_tree_forest.json','worldgen/placed_feature/qing_wen_elm_tree_forest.json']:
        assert not (DATA/rel).exists(),'obsolete dense forest injection: '+rel
    placement=read(DATA/'worldgen/placed_feature/qing_wen_elm_tree.json')['placement']
    assert [p['type'] for p in placement]==['minecraft:in_square','minecraft:heightmap','minecraft:biome'],'one candidate without duplicate probability gates'
    assert read(DATA/'tags/worldgen/biome/spirit_wood/qing_wen_elm.json')['values']==['#xiantu:spirit_wood/vanilla_forests']
    checks+=4
    labels=read(ASSETS/'lang/zh_cn.json')
    for f,*_ in TREES:
        for suffix in ['log','stripped_log','planks','leaves','sapling','stick','long_shaft','fiber']+BUILDING:
            name=f+'_'+suffix;path=ASSETS/f'models/item/{name}.json';assert path.exists(),path;assert 'item.xiantu.'+name in labels,name;checks+=2
            if suffix not in ['stick','long_shaft','fiber']:
                state=read(ASSETS/f'blockstates/{name}.json');loot=read(DATA/f'loot_tables/blocks/{name}.json');checks+=2
                for model in [v for v in state.get('variants',{}).values()]+[v['apply'] for v in state.get('multipart',[])]:assert (ASSETS/'models'/PathWithNamespace(model['model'])).exists();checks+=1
        for image in list((ASSETS/'textures/block').glob(f+'_*.png'))+list((ASSETS/'textures/item').glob(f+'_*.png')):
            with Image.open(image) as im:assert im.size==(64,64) and im.mode=='RGBA',image
            assert not any(part in image.stem for part in ['short_hilt','reinforced_handle','processing_scroll']),image;checks+=1
        for path in (DATA/'recipes').glob(f+'_*.json'):
            recipe=read(path);assert recipe['result']['item'].removeprefix('xiantu:') in registered,path
            for ingredient in recipe.get('key',{}).values():
                name=ingredient.get('item','');assert not name.startswith('xiantu:'+f) or name.removeprefix('xiantu:') in registered,(path,name)
            assert 'blueprint_id' not in recipe,path
            if recipe['type']=='xiantu:spirit_wood_processing':assert recipe['wood']==f,path
            sig=signature(recipe);mirror=signature(recipe,True)
            if sig:assert sig not in patterns and mirror not in patterns,(path,patterns.get(sig),patterns.get(mirror));patterns[sig]=path
            checks+=1
        for path in (ASSETS/'models/block').glob(f+'_*.json'):
            obj=read(path)
            if obj.get('parent','').startswith('minecraft:'):assert 'assets/minecraft/models/'+obj['parent'].removeprefix('minecraft:')+'.json' in known,(path,obj['parent'])
            for t in obj.get('textures',{}).values():
                if t.startswith('#'):continue
                if t.startswith('xiantu:'):assert (ASSETS/'textures'/PathWithNamespace(t.replace('xiantu:','xiantu:'),'.png')).exists(),(path,t)
                elif t.startswith('minecraft:'):assert 'assets/minecraft/textures/'+t.removeprefix('minecraft:')+'.png' in known,(path,t)
                else:raise AssertionError((path,'Invalid texture namespace',t))
            for cube in obj.get('elements',[]):
                assert all(0<=v<=32 for v in cube['from']+cube['to']),path
                for face in cube.get('faces',{}).values():assert face['texture'].removeprefix('#') in obj['textures'],path
            checks+=1
        for domain in ['configured_feature','placed_feature']:assert (DATA/f'worldgen/{domain}/{f}_tree.json').exists();checks+=1
        assert read(DATA/f'forge/biome_modifier/{f}_tree.json')['step']=='vegetal_decoration';checks+=1
    source=(ROOT/'src/main/java/com/longju/xiantu/wood/SpiritWoodRegistry.java').read_text(encoding='utf-8')
    assert all('"'+old+'"' not in source for old in ['strip','short_hilt','reinforced_handle'])
    models=read(BASE/'models-production.json')['models'];assert len(models)==120
    for entry in models:assert (BASE/'models'/f'{entry["id"]}.bbmodel').exists();checks+=1
    print(f'Spirit wood resources PASS: {checks} checks;264 registered items,240 blocks,24 materials,120 MCP exports')
def PathWithNamespace(value,suffix='.json'):
    from pathlib import Path
    return Path(value.removeprefix('xiantu:')+suffix)
if __name__=='__main__':main()
