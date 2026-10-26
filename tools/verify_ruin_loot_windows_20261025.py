from pathlib import Path
import json,re
from package_ruins_20261024 import read as readnbt
P=Path(__file__).resolve().parents[1];D=P/'src/main/resources/data/xiantu'
def load(p):return json.loads(p.read_text(encoding='utf8'))
rows=load(P/'asset_sources/ruin-loot-20261025/manifest.json');pills={x['id']:x['rank']for x in load(D/'pills.json')};checks=0
for row in rows:
 p=D/'loot_tables/chests'/row['table'];d=load(p);old=load(P/'asset_sources/ruin-loot-20261025/baseline'/row['table'])
 assert d['pools'][1:len(old['pools'])]==old['pools'][1:],(p,'rare pools changed')
 assert d['pools'][0]['entries']==old['pools'][0]['entries']
 pools={x.get('name'):x for x in d['pools']};tier=row['tier']
 for name in ('xiantu_exploration_supplies','xiantu_finished_medicines','xiantu_finished_talismans','xiantu_mineral_assortment'):assert pools[name]['entries']
 supply=pools['xiantu_exploration_supplies'];assert {'minecraft:iron_ingot','minecraft:diamond'}<=set(x['name']for x in supply['entries'])
 for e in pools['xiantu_finished_medicines']['entries']:
  assert 1<=pills[e['name'].split(':')[1]]<=tier
  assert re.search(r'pill_quality:[1-4]',e['functions'][1]['tag']);checks+=1
 for e in pools['xiantu_finished_talismans']['entries']:
  tag=e['functions'][1]['tag'];assert 1<=int(re.search(r'TalismanTier:(\d)',tag)[1])<=tier;assert re.search(r'TalismanQuality:[1-3]',tag);assert e['name']!='xiantu:talisman_blueprint';checks+=1
 assert len(pools['xiantu_mineral_assortment']['entries'])>=max(2,tier)
assert len(rows)==62
b=load(P/'asset_sources/ruin-interiors-20261025/structures/starfall_sanctuary_grand.json');size,blocks=readnbt(D/'structures/ruins/starfall_sanctuary_grand.nbt');cells={tuple(x[:3]):x[3]for x in blocks};report=load(P/'asset_sources/starfall-windows-20261025/changes.json')
assert len(b['windowRepair']['windows'])==54 and len(report['changes'])==432
for x,y,z in b['windowRepair']['windows']:
 s=cells[(x,y,z)];assert 'town_walnut_lattice'in s
 for dx in (-1,0,1):
  for dy in (0,1,2):assert cells[(x+dx,y+dy,z)]==s;checks+=1
print('Ruin loot/windows PASS:',checks,'tier/NBT/window checks;',len(rows),'tables, 54 complete nine-block windows; rare rewards preserved')
