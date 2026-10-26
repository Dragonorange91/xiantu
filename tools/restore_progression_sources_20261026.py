"""Restore missing acquisition entries from outline 3.5/3.6/8.8/8.10."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/xiantu'
p=DATA/'refining/facilities.json'
rows=json.loads(p.read_text('utf8'));ids={r['id'] for r in rows}
def add(id,name,output,tier,furnace,realm,ingredients):
    if id not in ids:rows.append(dict(id=id,name=name,output='xiantu:'+output,tier=tier,furnace=furnace,realm=realm,upgrade=False,ingredients=[dict(item=item if ':' in item else 'xiantu:'+item,count=count) for item,count in ingredients]));ids.add(id)
for tier,metal,wood,stone,furnace,realm in [(2,'chisui_copper_ingot','chi_xin_birch_planks','ling_jing',2,1),(3,'jiuyou_iron_ingot','xuan_tie_pine_planks','xuan_jing',4,3)]:
    prev=tier-1
    for part,label in [('eye','阵眼'),('base','阵基'),('rune_brick','阵纹砖'),('conduit_pillar','导灵柱')]:
        old='formation_core' if part=='eye' and prev==1 else f'array_{part}_{prev}'
        ingredients=[(old,1 if part=='eye' else 4),(metal,4 if part=='eye' else 2 if part in ['base','conduit_pillar'] else 1),(wood,2 if part in ['eye','base'] else 1)]
        if part=='eye':ingredients.append((stone,2))
        add(f'array_{part}_{tier}',f'{tier}级聚灵{label}',f'array_{part}_{tier}',tier,furnace,realm,ingredients)
for id,name,metal,wood,previous,tier,realm in [('jiuyou_pickaxe','九幽镐','zixiao_copper_ingot','xuan_tie_pine_stick','hanxing_pickaxe',4,3),('hundun_pickaxe','混沌镐','zifu_silver_ingot','xing_wen_wutong_stick','jiuyou_pickaxe',7,6)]:
    ingredients=[(metal,3),(wood,1),(previous,1)]
    add(id,name,id,tier,min(6,tier),realm,ingredients)
    next(row for row in rows if row['id']==id)['furnace']=min(6,tier)
    recipe=dict(type='xiantu:facility_crafting',category='equipment',facility=id,ingredients=[dict(item='xiantu:'+item) for item,count in ingredients for _ in range(count)],result=dict(item='xiantu:'+id))
    (DATA/f'recipes/{id}.json').write_text(json.dumps(recipe,ensure_ascii=False,indent=2)+'\n','utf8')
p.write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n','utf8')
# Higher formation plans belong in existing high core loot, with ordinary loot retained.
for path in (DATA/'loot_tables/chests/ruins').glob('tier_[5-8]_core_*.json'):
    table=json.loads(path.read_text('utf8'))
    if any(pool.get('name')=='xiantu:formation_plans' for pool in table['pools']):continue
    entries=[dict(type='minecraft:item',name='xiantu:equipment_blueprint',functions=[dict(function='minecraft:set_nbt',tag='{BlueprintVersion:1,Facility:"'+id+'"}')]) for id in ['array_eye_3','array_base_3','array_structure_3']]
    table['pools'].append(dict(name='xiantu:formation_plans',rolls=1,conditions=[dict(condition='minecraft:random_chance',chance=.2)],entries=entries))
    path.write_text(json.dumps(table,ensure_ascii=False,indent=2)+'\n','utf8')
print('Restored 8 formation assembly recipes, 2 sequential pickaxes and high formation plan loot')
