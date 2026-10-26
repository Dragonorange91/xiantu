"""Incremental loot-pool revision. Preserve rare diagrams, inheritance and boss rewards."""
from pathlib import Path
import json,re,copy
P=Path(__file__).resolve().parents[1];D=P/'src/main/resources/data/xiantu';O=P/'asset_sources/ruin-loot-20261025';O.mkdir(parents=True,exist_ok=True)
def read(p):return json.loads(p.read_text(encoding='utf8'))
def item(id,weight=1,count=None,tag=None):
 e={'type':'minecraft:item','name':id,'weight':weight};f=[]
 if count:f.append({'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':count[0],'max':count[1]}})
 if tag:f.append({'function':'minecraft:set_nbt','tag':tag})
 if f:e['functions']=f
 return e
def pool(name,entries,low=1,high=1,chance=None):
 d={'name':name,'rolls':low if low==high else {'type':'minecraft:uniform','min':low,'max':high},'entries':entries}
 if chance is not None:d['conditions']=[{'condition':'minecraft:random_chance','chance':chance}]
 return d
pills=read(D/'pills.json');files=list((D/'loot_tables/chests/ruins').glob('tier_*.json'))+list((D/'loot_tables/chests/ruins').glob('boss_*.json'))+list((D/'loot_tables/chests').glob('ruin_*.json'))+list((D/'loot_tables/chests').glob('grand_core_*.json'))
# Derive registered metal IDs rather than inventing names.
metaltext=P/'src/main/java/com/longju/xiantu/mining/MetalMaterialRegistry.java'
registry=metaltext.read_text(encoding='utf8')
report=[]
for f in sorted(files):
 rel=f.relative_to(D/'loot_tables/chests');base=O/'baseline'/rel;base.parent.mkdir(parents=True,exist_ok=True)
 if not base.exists():base.write_bytes(f.read_bytes())
 d=read(base);tiermatch=re.match(r'tier_(\d)_',f.stem);tier=int(tiermatch[1])if tiermatch else {'wood':6,'thunder':7,'earth':8}.get(f.stem.removeprefix('grand_core_'),3)if f.stem.startswith('grand_')else 3
 if f.stem.startswith('boss_'):tier=max(map(int,re.findall(r'(?:core|beast)_grade:(\d)',json.dumps(d))))
 core='core'in f.stem or f.stem.startswith('boss_');vault='vault'in f.stem
 # Keep the old theme pool, but prevent it occupying almost the entire chest.
 if not f.stem.startswith('boss_'):d['pools'][0]['rolls']={'type':'minecraft:uniform','min':2,'max':3 if core else 2}
 commons=[item('minecraft:'+id,w,c)for id,w,c in [('iron_ingot',5,(2,7)),('gold_ingot',3,(1,4)),('copper_ingot',3,(2,8)),('bread',3,(2,5)),('torch',3,(4,12)),('paper',3,(3,8)),('string',2,(2,6)),('bone',2,(2,5)),('coal',3,(3,9)),('lapis_lazuli',2,(2,7)),('arrow',2,(4,10)),('emerald',2,(1,3)),('diamond',1,(1,2))]]
 d['pools'].append(pool('xiantu_exploration_supplies',commons,2,4))
 # Finished medicines, never bare recipe IDs. Keep the actual pill's rank within the chest ceiling.
 allowed=[x for x in pills if max(1,tier-2)<=x['rank']<=tier]
 medicines=[]
 for x in allowed:
  for q,w in [(1,6),(2,4),(3,2),(4,1)]:medicines.append(item('xiantu:'+x['id'],w,(1,2),'{pill_quality:'+str(q)+'}'))
 d['pools'].append(pool('xiantu_finished_medicines',medicines,1,2 if core else 1))
 defs=re.findall(r'new Definition\("([^"]+)","[^"]+","[^"]+",(\d)',(P/'src/main/java/com/longju/xiantu/talisman/TalismanCatalog.java').read_text(encoding='utf8'))
 symbols=[]
 for id,minimum in defs:
  if id in ('rebirth','ward'):continue
  for t in range(max(int(minimum),tier-1),tier+1):
   for q,w in [(1,4),(2,2),(3,1)]:symbols.append(item('xiantu:talisman_'+id,w,(1,2),'{TalismanTier:'+str(t)+',TalismanQuality:'+str(q)+'}'))
 d['pools'].append(pool('xiantu_finished_talismans',symbols,1,1,1 if core else .65))
 # Add a separate assortment pool to reduce repeated theme ore rolls; rare endgame ores stay gated.
 mineralmin={'cinnabar':1,'purple_pattern_crystal':2,'lodestone_shard':2,'moonlit_stone':2,'cold_soul_crystal':3,'flame_heart_stone':3,'thunder_stone':3,'starlight_stone':4,'scoria_core':4,'void_crystal':6,'chaos_crystal':7}
 assortment=[item('xiantu:'+id,2,(1,3))for id,t in mineralmin.items()if t<=tier]
 for id,t in re.findall(r'new Material\("([^"]+)",\s*"[^"]+",\s*(\d),',registry):
  if max(1,tier-2)<=int(t)<=tier:assortment.append(item('xiantu:'+id+'_raw',2,(1,3)))
 d['pools'].append(pool('xiantu_mineral_assortment',assortment,1,2 if core else 1))
 f.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf8');report.append({'table':str(rel),'tier':tier,'medicineEntries':len(medicines),'talismanEntries':len(symbols),'mineralEntries':len(assortment)})
(O/'manifest.json').write_text(json.dumps(report,indent=2),encoding='utf8');print('Expanded',len(report),'loot tables; original rare pools retained')
from sync_early_gongfa_loot import sync
sync()
