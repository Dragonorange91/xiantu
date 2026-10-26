from pathlib import Path
import json,hashlib,struct
P=Path(__file__).resolve().parents[1];R=P/'src/main/resources'
rows=json.loads((R/'data/xiantu/pills.json').read_text(encoding='utf8'));assert len(rows)==36
for d in rows:
 base=json.loads((R/f'assets/xiantu/models/item/{d["id"]}.json').read_text())
 assert [v['predicate']['xiantu:pill_quality'] for v in base['overrides']]==[2,3,4,5]
 for q in range(1,6):
  name=f'{d["id"]}_q{q}'
  model=json.loads((R/f'assets/xiantu/models/item/pills/{name}.json').read_text())
  assert model['textures']['layer0']==f'xiantu:item/pills/{name}'
  png=(R/f'assets/xiantu/textures/item/pills/{name}.png').read_bytes()
  assert png[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',png[16:24])==(64,64)
for old in ['basic_pill','foundation_pill','spirit_recovery_pill','body_tempering_pill']:
 assert not (R/f'data/xiantu/recipes/{old}.json').exists()
print('Pill resources: 36 definitions, 180 textures, 216 models PASS')
