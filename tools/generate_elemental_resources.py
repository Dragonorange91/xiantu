"""Generate explicit elemental tags; no custom damage is inferred from item names/NBT."""
from pathlib import Path
import json, re, shutil

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
original = {
    'metal': ['iron_tooth_badger', 'cracked_blade_wolf', 'gold_mane_lion'],
    'wood': ['vine_deer', 'moss_back_bear'],
    'water': ['cold_fang_wolf'],
    'fire': ['yanwei_fox', 'red_sun_lion'],
    'earth': ['rock_back_boar', 'thick_horn_rhino', 'crag_rock_spider', 'mountain_bear'],
    'thunder': ['thunder_deer', 'thunder_feather_raven'],
}
source = (ROOT / 'src/main/java/com/longju/xiantu/entity/CompletionBestiary.java').read_text(encoding='utf-8')
for entity, element in re.findall(r'new Definition\("([^"]+)",\s*"[^"]+",\s*"[^"]+",\s*"[^"]+",\s*"([^"]+)"', source):
    original[element].append(entity)
assert sum(map(len, original.values())) == 36
assert len({x for group in original.values() for x in group}) == 36
weapons = {'metal': ['azure_artifact'], 'fire': ['flame_artifact'], 'earth': ['mountain_artifact']}
for element, entities in original.items():
    for kind, folder, values in [('entity_types','elemental_affinity',entities), ('items','elemental_weapons',weapons.get(element,[]))]:
        p = RES / 'data/xiantu/tags' / kind / folder / f'{element}.json'
        p.parent.mkdir(parents=True,exist_ok=True)
        p.write_text(json.dumps({'replace': False, 'values': ['xiantu:'+x for x in values]},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

for lang in ['zh_cn','en_us']:
    p = RES / 'assets/xiantu/lang' / f'{lang}.json'
    backup = ROOT / 'work/elemental-combat-20261018/before' / p.name
    if not backup.exists(): shutil.copyfile(p,backup)
    data = json.loads(p.read_text(encoding='utf-8-sig'))
    zh = lang == 'zh_cn'
    data['tooltip.xiantu.elemental_attack'] = '攻击属性：%s' if zh else 'Attack element: %s'
    for element,name in zip(original,['金','木','水','火','土','雷']):
        label = name if zh else element.title()
        data[f'element.xiantu.{element}'] = label
        data[f'attribute.xiantu.{element}_resistance'] = label+('属性抗性' if zh else ' Resistance')
        data[f'attribute.xiantu.{element}_penetration'] = label+('属性穿透' if zh else ' Penetration')
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Elemental tags: 30 beasts + 6 bosses; 3 existing artifact weapons; zh_cn/en_us updated')
