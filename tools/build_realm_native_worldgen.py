"""Copy 1.20.1 vanilla generation recipes, isolating realm boosts from the Overworld."""
from build_paths import minecraft_reference
from pathlib import Path
import zipfile,json,copy
root=Path(__file__).resolve().parents[1]/'src/main/resources/data/xiantu/worldgen'
vanilla=minecraft_reference('client-extra.jar')
biomes=['forest','birch_forest','flower_forest','savanna','windswept_savanna','plains','swamp','meadow','windswept_forest','windswept_hills','taiga','old_growth_pine_taiga','lush_caves','dripstone_caves']
# Extra passes add 50% count attempts, retaining exact vanilla height and air-exposure rules.
rare=['ore_diamond','ore_diamond_large','ore_diamond_buried','ore_gold','ore_gold_lower','ore_lapis','ore_lapis_buried','ore_redstone','ore_redstone_lower','ore_emerald']
def write(kind,name,data):
 p=root/kind/(name+'.json');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
with zipfile.ZipFile(vanilla) as z:
 for ore in rare:
  d=json.loads(z.read(f'data/minecraft/worldgen/placed_feature/{ore}.json'))
  for modifier in d['placement']:
   if modifier['type']=='minecraft:count':
    value=modifier['count']
    if isinstance(value,int):modifier['count']=max(1,round(value*.5))
    else:
     # Vanilla gold_lower is a uniform count provider; retain its distribution.
     value['value']['min_inclusive']=max(0,round(value['value']['min_inclusive']*.5))
     value['value']['max_inclusive']=max(1,round(value['value']['max_inclusive']*.5))
   elif modifier['type']=='minecraft:rarity_filter':modifier['chance']*=2
  write('placed_feature','realm_bonus_'+ore,d)
 for name in biomes:
  d=json.loads(z.read(f'data/minecraft/worldgen/biome/{name}.json'))
  d['spawners']={};d['spawn_costs']={}
  for features in d['features']:
   features.extend('xiantu:realm_bonus_'+ore for ore in rare if 'minecraft:'+ore in features)
  write('biome','realm_'+name,d)
print('Realm native worldgen: 14 vanilla-derived biomes, 10 isolated rare-ore bonus recipes')
