import json
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'src/main/resources/data/xiantu/refining'
data=json.loads((r/'weapon_skill_settings.json').read_text(encoding='utf-8'));recipes=json.loads((r/'catalog.json').read_text(encoding='utf-8-sig'))['recipes']
assert data['schema_version']==1 and len(data['ordinary'])==30
assert data['rules']['condition_min_tier']==5 and data['rules']['primary_hand_only']
assert data['rules']['sword_axe_staff_attack']=='vanilla'
for s in data['ordinary']:
    tiers=[x['tier'] for x in recipes if x['enabled'] and x['theme']==s['theme'] and x['kind'] in ('sword','axe','spear')]
    assert (s['min_tier'],s['max_tier'])==(min(tiers),max(tiers))
    assert s['condition_available']==(max(tiers)>=5) and s['condition_min_tier']==5
    assert s['name'] and s['base'] and s['condition'] and s['activation']
assert data['special']['extend']['cost']==5000 and data['special']['quake']['cost']==12000
print('Weapon skills PASS: all 30 themes, real recipe bounds, no quality bypass, vanilla attack policy, Qitian costs')
