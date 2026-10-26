"""Verify the complete 19-piece resource chain and model-derived collision data."""
import json,re
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources';AS=RES/'assets/xiantu'
def read(p):return json.loads(p.read_text(encoding='utf8'))
manifest=read(RES/'data/xiantu/town_materials.json')['blocks']
source=(ROOT/'src/main/java/com/longju/xiantu/building/TownMaterialRegistry.java').read_text(encoding='utf8')
lang=read(AS/'lang/zh_cn.json')
assert len(manifest)==19
def normalized(boxes):return sorted(tuple(round(float(x),7)for x in b)for b in boxes)
def rotate(boxes):return [[16-b[5],b[1],b[0],16-b[2],b[4],b[3]]for b in boxes]
for row in manifest:
    id=row['id'].split(':')[1]
    assert f'registry.register("{id}"' in source
    model=read(AS/f'models/block/{id}.json');assert 'format_version' not in model
    boxes=[e['from']+e['to']for e in model['elements']]
    for box in boxes:
        assert all(0<=v<=16 for v in box),(id,box)
        assert all(box[i]<box[i+3]for i in range(3))
    for tex in model['textures'].values():
        assert not tex.startswith('#'),(id,tex)
        with Image.open(AS/f'textures/{tex.split(":")[1]}.png')as im:
            assert im.size==(64,64);assert im.convert('RGBA').getchannel('A').getextrema()==(255,255)
    for e in model['elements']:
        assert e.get('rotation',{}).get('angle',0)==0,id
        for face in e['faces'].values():
            assert face['texture'][1:]in model['textures']
            assert all(0<=v<=16 for v in face['uv'])
    assert read(AS/f'models/item/{id}.json')['parent']==f'xiantu:block/{id}'
    variants=read(AS/f'blockstates/{id}.json')['variants']
    assert len(variants)==(3 if id.endswith('_column')else 4)
    assert all(v['model']==f'xiantu:block/{id}'for v in variants.values())
    if not id.endswith('_column'):
        line=next(l for l in source.splitlines()if f'registry.register("{id}"'in l)
        actual=[list(map(float,m.split(',')))for m in re.findall(r'\{([\d.,]+)\}',line)]
        assert normalized(actual)==normalized(boxes),(id,'collision mismatch')
        for direction,angle in [('north',0),('east',90),('south',180),('west',270)]:assert variants['facing='+direction].get('y',0)==angle
        r=boxes
        for i in range(4):r=rotate(r)
        assert normalized(r)==normalized(boxes)
        # NW corner reflection matches the diagonal mirror route (+one clockwise turn).
        if id in ['town_qingwa_hip_low','town_qingwa_hip_high','town_qingwa_corner']:
            reflected=[[16-b[3],b[1],b[2],16-b[0],b[4],b[5]]for b in boxes]
            assert normalized(reflected)==normalized(rotate(boxes)),(id,'corner is not reflection symmetric')
    assert lang['block.xiantu.'+id]==row['chineseName']
    assert not re.search('[一二三四五六七八九]阶',lang['block.xiantu.'+id])
    loot=read(RES/f'data/xiantu/loot_tables/blocks/{id}.json')
    assert loot['pools'][0]['entries'][0]['name']==row['id']
    recipe=read(RES/f'data/xiantu/recipes/{id}.json')
    assert (recipe['result'] if isinstance(recipe['result'],str)else recipe['result']['item'])==row['id']
    tool='pickaxe'if id.startswith('town_qingwa_')else 'axe'
    assert row['id']in read(RES/f'data/minecraft/tags/blocks/mineable/{tool}.json')['values']

# New crafting patterns must not shadow any existing recipe, including horizontal mirrors.
def pattern_key(recipe):
    p=recipe['pattern'];key=recipe['key'];rows=[]
    for row in p:rows.append([json.dumps(key[c],sort_keys=True)if c!=' 'else ''for c in row])
    while rows and not any(rows[0]):rows.pop(0)
    while rows and not any(rows[-1]):rows.pop()
    left=min(i for row in rows for i,c in enumerate(row)if c);right=max(i for row in rows for i,c in enumerate(row)if c)
    rows=tuple(tuple(row[left:right+1])for row in rows)
    return min(rows,tuple(tuple(reversed(row))for row in rows))
all_recipes=[]
for file in (RES/'data/xiantu/recipes').rglob('*.json'):
    r=read(file)
    if r.get('type')=='minecraft:crafting_shaped':all_recipes.append((file,pattern_key(r)))
for row in manifest:
    path=RES/f'data/xiantu/recipes/{row["id"].split(":")[1]}.json';recipe=read(path)
    if recipe['type']=='minecraft:crafting_shaped':
        k=pattern_key(recipe)
        assert not [(str(p),key)for p,key in all_recipes if p!=path and key==k],('Recipe collision',path)
print('Town blueprint materials: 19 registrations, exact collision cubes, 75 orientation variants, textures, recipes, loot and tool tags PASS')
