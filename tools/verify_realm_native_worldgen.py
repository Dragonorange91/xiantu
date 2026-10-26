from build_paths import minecraft_reference
from pathlib import Path
import json,zipfile,copy,re
root=Path(__file__).resolve().parents[1];r=root/'src/main/resources/data/xiantu/worldgen'
vanilla=minecraft_reference('client-extra.jar')
with zipfile.ZipFile(vanilla) as z:
 for p in (r/'biome').glob('realm_*.json'):
  original=json.loads(z.read('data/minecraft/worldgen/biome/'+p.stem.removeprefix('realm_')+'.json'));d=json.loads(p.read_text(encoding='utf8'));assert not d['spawners'] and not d['spawn_costs']
  d['spawners']=original['spawners'];d['spawn_costs']=original['spawn_costs'];d['features']=[[v for v in stage if not v.startswith('xiantu:realm_bonus_')] for stage in d['features']]
  assert d==original,(p,'native vegetation/carvers altered')
 for p in (r/'placed_feature').glob('realm_bonus_*.json'):
  o=json.loads(z.read('data/minecraft/worldgen/placed_feature/'+p.stem.removeprefix('realm_bonus_')+'.json'));d=json.loads(p.read_text(encoding='utf8'))
  assert d['feature']==o['feature']
  for a,b in zip(d['placement'],o['placement']):
   if a['type'] not in ('minecraft:count','minecraft:rarity_filter'):assert a==b,(p,'native height or exposure changed')
   elif a['type']=='minecraft:rarity_filter':assert a['chance']==b['chance']*2
   elif isinstance(a['count'],int):assert a['count']==max(1,round(b['count']*.5))
herbs=dict(re.findall(r'new MedicineDefinition\("(herb_\d+)", "[^"]+", (\d+)',(root/'src/main/java/com/longju/xiantu/medicine/MedicineCatalog.java').read_text(encoding='utf8')))
for i in [1,2]:
 t=json.loads((root/f'src/main/resources/data/xiantu/loot_tables/instance/material_{i}.json').read_text())
 for pool in t['pools']:
  for e in pool['entries']:
   name=e['name'].split(':')[1];assert name in {'fan_ling_shi','ling_jing','beast_bone'} or name in herbs and int(herbs[name])<=i,name
print('PASS: native biome features/carvers exact, bonus ores retain vanilla heights; low-tier tables contain only Qi/Foundation materials')
