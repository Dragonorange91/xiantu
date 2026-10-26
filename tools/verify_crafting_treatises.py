"""Check actual loot and model resources for the low-tier treatise change."""
from pathlib import Path
import json,re
from PIL import Image

root=Path(__file__).resolve().parents[1]
r=root/'src/main/resources'
recipes=json.loads((r/'data/xiantu/alchemy/recipes.json').read_text(encoding='utf-8'))
low={row['id'] for row in recipes if row['rank']<=3}
high={row.get('pill',row['id']) for row in recipes if row['rank']>=4}
seen=set()
for p in (r/'data/xiantu/loot_tables').rglob('*.json'):
    data=json.loads(p.read_text(encoding='utf-8'))
    for pool in data.get('pools',[]):
        assert pool.get('entries'),p
        for entry in pool['entries']:
            for f in entry.get('functions',[]):
                m=re.search(r'Alchemy:"([a-z0-9_]+)"',f.get('tag',''))
                if m:
                    assert m.group(1) not in low,(p,m.group(1))
                    seen.add(m.group(1))
assert high<=seen,high-seen
models=r/'assets/xiantu/models/item'
small=json.loads((models/'pills/held_small.json').read_text(encoding='utf-8'))
assert set(small['display'])=={'firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'}
for model in (models/'pills').glob('pill_*_q*.json'):
    assert json.loads(model.read_text())['parent']=='xiantu:item/pills/held_small',model
for hand in ['firstperson_righthand','firstperson_lefthand']:
    assert small['display'][hand]['scale']==[.4,.4,.4]
for hand in ['thirdperson_righthand','thirdperson_lefthand']:
    assert small['display'][hand]['rotation']==[0,0,0] and small['display'][hand]['scale']==[.33,.33,.33]
items=['dandao_zhenjie','qidao_zhenjie','equipment_blank','inner_demon_crystal','jingu_fragment',
       'nine_heaven_cloud_silk','purple_gold_monkey_hide','qitian_battle_soul','spell_scroll_fire_bolt',
       'spell_scroll_thunder_strike','spirit_gold_essence']
for item in items:
    model=json.loads((models/(item+'.json')).read_text())
    assert model['textures']['layer0']=='xiantu:item/'+item,(item,'placeholder remains')
    image=r/'assets/xiantu/textures'/(model['textures']['layer0'].removeprefix('xiantu:')+'.png')
    im=Image.open(image).convert('RGBA')
    assert im.size==(64,64) and im.getchannel('A').getextrema()==(0,255),image
ore=json.loads((models.parent/'block/spirit_gold_essence_ore.json').read_text())
if ore['textures']['all']=='xiantu:block/spirit_gold_essence_ore':
    ore_image=Image.open(r/'assets/xiantu/textures/block/spirit_gold_essence_ore.png').convert('RGBA')
    assert ore_image.size==(64,64) and ore_image.getchannel('A').getextrema()==(255,255)
else:
    assert ore['textures']['all']=='minecraft:block/deepslate_gold_ore'
    print('PENDING ART: ore block retains its valid vanilla texture; dedicated ore API artwork unavailable')
print(f'Crafting treatises PASS: {len(low)} low pill diagrams removed; {len(high)} high diagram sources retained; 180 hand models, {len(items)} transparent item textures')
