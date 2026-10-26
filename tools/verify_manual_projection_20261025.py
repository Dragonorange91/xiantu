from pathlib import Path
import json,re
from PIL import Image
from package_ruins_20261024 import read
P=Path(__file__).resolve().parents[1];J=P/'src/main/java/com/longju/xiantu';R=P/'src/main/resources';D=R/'data/xiantu';A=R/'assets/xiantu'
removed=['basic_talisman','waterbreathing_talisman','nightvision_talisman','ward_talisman']
for name in removed:
 assert f'register("{name}"'not in (J/'registry/ModItems.java').read_text('utf8')
 assert not (A/f'models/item/{name}.json').exists()
 assert not (A/f'textures/item/{name}.png').exists()
assert (A/'models/item/talisman_ward.json').exists(),'modern ward talisman retained'
seal=(J/'worldgen/RuinEncounterBlock.java').read_text('utf8');assert 'noCollission()'in seal and 'RenderShape.INVISIBLE'in seal and 'Shapes.empty()'in seal
layout=(J/'refining/WorkstationLayout.java').read_text('utf8');assert 'FIRE_X=BAGUA_X-8,FIRE_Y=BAGUA_Y-8'in layout
screen=(J/'instance/InstanceScreen.java').read_text('utf8');assert 'FittedContainerScreen<InstanceMenu>'in screen and '聚灵阵'in screen and '2000+planTier'in screen
plan=(J/'formation/FormationPlan.java').read_text('utf8');assert 'FormationTrace.ringStage'in plan and 'blocks(tier).values()'in plan
placement=(J/'formation/FormationProjection.java').read_text('utf8')
for feature in ['onBlockPlace','snapshot','restore(true,false)','RuinProtection.protectedAt','mayAct(p,pos)','inventory.items.set','isFaceSturdy','canBeReplaced','getBlockEntity','getFluidState','distanceToSqr','lastClick','isShiftKeyDown']:assert feature in placement,feature
assert placement.index('complete=true;')<placement.index('inventory.items.set')
assert 'ManualIssued'in (J/'trade/TownJourney.java').read_text('utf8')
im=Image.open(A/'textures/item/cultivation_manual.png');assert im.size==(64,64)and im.mode=='RGBA';assert im.getchannel('A').getbbox()
water=json.loads((D/'ruin_blueprints/water_curtain_caverns_grand.json').read_text('utf8'));assert water['waterfall']['ledges']==[59,52,45,38,31,24,17]
_,cells=read(D/'structures/ruins/water_curtain_caverns_grand.nbt');cells={tuple(c[:3]):c[3]for c in cells}
for z in range(111,140):
 for y in [17,18,19]:assert cells[(80,y,z)]=='minecraft:air',(z,y,'sealed entrance passage')
assert cells[(80,17,140)].startswith('minecraft:water')
report=json.loads((P/'asset_sources/ruin-palette-waterfall-20261025/report.json').read_text('utf8'));assert len(report)==20 and len({(v['stone'],v['wood'])for v in report})>=10
for v in report:assert v['customMaterials']>=5 and v['changed']>0
for name in ['cultivation_probe','dimension_waymark']:assert json.loads((D/f'recipes/{name}.json').read_text('utf8'))['result']['item']=='xiantu:'+name
for f in (D/'loot_tables/chests/ruins').glob('tier_*_core_*.json'):
 tier=int(f.stem.split('_')[1]);pools={v.get('name'):v for v in json.loads(f.read_text('utf8'))['pools']}
 if tier>=3:assert pools['xiantu_reincarnation_stone']['conditions'][0]['chance']==.05
 if tier>=4:assert pools['xiantu_gongfa_inheritance']['conditions'][0]['chance']==.08
old=json.loads((P/'asset_sources/manual-formation-20261025/refining-catalog.json.before').read_text('utf8'))
new=json.loads((D/'refining/catalog.json').read_text('utf8'))
rare={'xiantu:'+v for v in ['jingu_fragment','spirit_gold_essence','qitian_battle_soul','purple_gold_monkey_hide','nine_heaven_cloud_silk']}
increased=0
for a,b in zip(old['recipes'],new['recipes']):
 assert a['id']==b['id'] and a['seconds']==b['seconds'] and len(a['ingredients'])==len(b['ingredients'])
 for x,y in zip(a['ingredients'],b['ingredients']):
  if x['item']in rare or x['item']=='xiantu:demon_core':assert x['count']==y['count']
  else:assert x['count']<=y['count']<=64;increased+=y['count']>x['count']
assert increased>500
print(f'Manual/projection/ruin revision PASS: 20 palettes, stepped cave entrance, removal, source pools and {increased} increased common refining inputs')
