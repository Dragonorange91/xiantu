"""Validate appraisal resources against real registries, source and Minecraft recipes."""
from build_paths import minecraft_reference
import json,zipfile
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources';A=R/'assets/xiantu';D=R/'data/xiantu'
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def main():
    model=read(A/'models/block/appraisal_altar.json');assert len(model['elements'])==4
    for e in model['elements']:
        assert all(0<=v<=16 for v in e['from']+e['to'])
        for face in e['faces'].values():assert face['texture'].lstrip('#') in model['textures']
    for t in model['textures'].values():
        assert t.startswith('xiantu:');p=A/'textures'/str(t.split(':')[1]+'.png');assert p.exists()
        with Image.open(p) as im:assert im.size==(64,64) and im.mode=='RGBA'
    tag=read(D/'tags/items/identifiable_materials.json');assert len(tag['values'])>=48 and len(set(tag['values']))==len(tag['values'])
    assert sum(s.split(':')[1].endswith(('_log','_stripped_log','_planks','_stick','_long_shaft','_fiber')) for s in tag['values'])==48
    assert not any(s.split(':')[1].endswith(('_raw','_ingot','_block')) for s in tag['values'])
    assert not any(s.split(':')[1].endswith('_ore') for s in tag['values'])
    assert all((A/'models/item'/str(v.split(':')[1]+'.json')).exists() for v in tag['values'])
    assert not list((D/'recipes').glob('*_purification.json'))
    recipe=read(D/'recipes/appraisal_altar.json');assert recipe['pattern']==['SSS','SGS','C C']
    def sig(recipe):
        if 'pattern' not in recipe:return None
        return tuple(tuple(json.dumps(recipe['key'].get(c,{}),sort_keys=True) for c in row) for row in recipe['pattern'])
    target=sig(recipe)
    for path in (D/'recipes').glob('*.json'):
        if path.name!='appraisal_altar.json':assert sig(read(path))!=target,path
    with zipfile.ZipFile(minecraft_reference('client.jar')) as jar:
        for name in jar.namelist():
            if name.startswith('data/minecraft/recipes/') and name.endswith('.json'):assert sig(json.loads(jar.read(name)))!=target,name
    for lang in ['zh_cn','en_us']:
        labels=read(A/f'lang/{lang}.json');assert 'block.xiantu.appraisal_altar' in labels and 'item.xiantu.appraisal_altar' in labels
    source=ROOT/'src/main/java/com/longju/xiantu'
    assert not (source/'wood/WoodPurity.java').exists()
    for file in (source/'wood').glob('*.java'):assert 'WoodPurity' not in file.read_text(encoding='utf-8')
    assert (ROOT/'asset_sources/material_appraisal/appraisal_altar.bbmodel').exists()
    assert 'material_quality' not in (ROOT/'src/main/java/com/longju/xiantu/mining/SpiritOreBlock.java').read_text(encoding='utf-8')
    print('Material appraisal resources PASS:48 spirit-wood materials plus refining additives and beast materials; metal ores/ingots/storage excluded; static MCP model,64px tiles, no refinement or recipe collisions')
if __name__=='__main__':main()
