"""Forge 1.20.1 schemas; no live registry mutation. Do not generate resources or ores here."""
import json,re
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(name,value):
    p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8')
write('data/xiantu/dimension_type/instance.json',dict(ultrawarm=False,natural=False,coordinate_scale=1.0,has_skylight=True,has_ceiling=False,ambient_light=0.0,piglin_safe=False,bed_works=False,respawn_anchor_works=False,has_raids=False,logical_height=384,min_y=-64,height=384,infiniburn='#minecraft:infiniburn_overworld',effects='minecraft:overworld',monster_spawn_block_light_limit=0,monster_spawn_light_level=0))
write('data/xiantu/worldgen/biome/instance.json',dict(has_precipitation=False,temperature=.7,downfall=0,effects=dict(fog_color=12638463,water_color=4159204,water_fog_color=329011,sky_color=7907327),spawners={},spawn_costs={},carvers={},features=[[] for _ in range(11)]))
for cave,count in [(False,16),(True,256)]:
    for i in range(count):
        name=f'dongfu_{i:03}' if cave else f'realm_{i:02}'
        write(f'data/xiantu/dimension/{name}.json',dict(type='xiantu:instance',generator=dict(type='xiantu:instance',cave=cave,salt=i,biome_source=dict(type='minecraft:fixed',biome='xiantu:instance'))))
write('assets/xiantu/blockstates/instance_core.json',{'variants':{'':{'model':'xiantu:block/instance_core'}}})
write('assets/xiantu/models/block/instance_core.json',{'parent':'minecraft:block/cube_all','textures':{'all':'minecraft:block/crying_obsidian'}})
for item,texture in [('dongfu_token','minecraft:item/ender_eye'),('dongfu_child_token','minecraft:item/ender_pearl')]:
    write(f'assets/xiantu/models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture}})
for lang,values in [('zh_cn',{'item.xiantu.dongfu_token':'洞府令牌','item.xiantu.dongfu_child_token':'洞府子令牌','block.xiantu.instance_core':'洞天核心'}),('en_us',{'item.xiantu.dongfu_token':'Cave Token','item.xiantu.dongfu_child_token':'Guest Cave Token','block.xiantu.instance_core':'Realm Core'})]:
    p=root/f'assets/xiantu/lang/{lang}.json';v=json.loads(p.read_text(encoding='utf-8-sig'));v.update(values);write(f'assets/xiantu/lang/{lang}.json',v)
catalog=(Path(__file__).resolve().parents[1]/'src/main/java/com/longju/xiantu/medicine/MedicineCatalog.java').read_text(encoding='utf-8')
herbs=re.findall(r'new MedicineDefinition\("([^"]+)", "[^"]+", (\d+), (?:true|false), true, (?:true|false)\)',catalog)
stones=['fan_ling_shi','ling_jing','ling_yu','xuan_jing','di_sui','tian_jing','xian_yu','dao_shi']
for tier,stone in enumerate(stones,1):
    entries=[{'type':'minecraft:item','name':'xiantu:'+stone,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}}]}, {'type':'minecraft:item','name':'xiantu:beast_bone'}]
    write(f'data/xiantu/loot_tables/instance/material_{tier}.json',{'type':'minecraft:chest','pools':[{'rolls':2,'entries':entries},{'rolls':1,'entries':[{'type':'minecraft:item','name':'xiantu:'+name} for name,rank in herbs if int(rank)==tier]}]})
print('272 dimension slots and instance resources generated')
