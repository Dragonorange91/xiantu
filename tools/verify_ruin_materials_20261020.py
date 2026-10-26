"""Verify actual MCP model exports agree with collision geometry and registered resources."""
from pathlib import Path
import json,re
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/xiantu';D=R/'data/xiantu'
manifest=json.loads((D/'ruin-materials.json').read_text(encoding='utf8'))
collision=(ROOT/'src/main/java/com/longju/xiantu/building/BrokenPillarBlock.java').read_text(encoding='utf8')
registry=(ROOT/'src/main/java/com/longju/xiantu/building/TownMaterialRegistry.java').read_text(encoding='utf8')
for entry in manifest['models']:
    id=entry['id'];m=json.loads((A/f'models/block/{id}.json').read_text(encoding='utf8'))
    expected=sorted(b['from']+b['to'] for b in entry['cubes']);actual=sorted(e['from']+e['to'] for e in m['elements'])
    assert expected==actual,id
    assert actual and 'format_version' not in m
    text=collision.split('BOXES = ')[1].split(';')[0] if entry['existing'] else next(l for l in registry.splitlines() if f'registry.register("{id}"' in l)
    boxes=sorted([list(map(float,v.split(',')))for v in re.findall(r'\{([\d.,]+)\}',text)])
    assert boxes==actual,(id,'collision differs from exported geometry')
    for tex in m['textures'].values():
        if tex.startswith('#'): continue
        if tex.startswith('#'): continue
        assert tex.startswith('xiantu:')
        im=Image.open(A/f'textures/{tex.split(":")[1]}.png');assert im.size==(64,64)
    for e in m['elements']:
        assert all(0<=a<b<=16 for a,b in zip(e['from'],e['to']))
        for f in e['faces'].values():
            assert f['texture'][1:] in m['textures'] and all(0<=v<=16 for v in f['uv'])
    if not entry['existing']:
        assert id in registry
        for path in [A/f'blockstates/{id}.json',A/f'models/item/{id}.json',D/f'recipes/{id}.json',D/f'loot_tables/blocks/{id}.json']:assert path.is_file()
        assert 'xiantu:'+id in json.loads((R/'data/minecraft/tags/blocks/mineable/pickaxe.json').read_text())['values']
print(f'Ruin models: {len(manifest["models"])} exports, matching collisions, textures, registration, recipes and loot PASS')
