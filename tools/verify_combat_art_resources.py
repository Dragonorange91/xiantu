"""Validate atlas payloads and the custom portal format before they enter a Forge JAR."""
from pathlib import Path
import hashlib
import json
import math
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/xiantu'
manifest = json.loads((ROOT / 'tools/data/weapon-art-sources.json').read_text('utf8'))
assert len(manifest) == 9
frames = 0
for name, record in manifest.items():
    path = ASSETS / f'textures/vfx/weapon_art/{name}.png'
    assert hashlib.sha256(path.read_bytes()).hexdigest() == record['runtime_sha256'], name
    with Image.open(path) as atlas:
        assert atlas.size == (1024, 1024) and atlas.mode == 'RGBA', name
        assert 1 <= record['frames'] <= 16, name
        for i in range(16):
            cell = atlas.crop(((i % 4)*256, (i // 4)*256, (i % 4+1)*256, (i // 4+1)*256))
            alpha = cell.getchannel('A')
            if i < record['frames']:
                assert alpha.getbbox() and alpha.getextrema()[0] == 0, (name, i, 'missing transparency or frame')
                frames += 1
            else:
                assert alpha.getbbox() is None, (name, i, 'unexpected extra frame')

# This is a custom face array, not a vanilla block-model object. models/ makes
# Minecraft's automatic model discovery parse it using the wrong schema.
assert not (ASSETS / 'models/effect/realm_astral_gate.json').exists()
faces = json.loads((ASSETS / 'effects/realm_astral_gate.json').read_text('utf8'))
assert len(faces) == 384
for face in faces:
    assert len(face['vertices']) == 4 and all(len(v) == 3 for v in face['vertices'])
    assert len(face['normal']) == 3 and len(face['uv']) == 4
    assert all(math.isfinite(v) for row in face['vertices'] for v in row)
    assert abs(sum(v*v for v in face['normal']) - 1) < .001
    assert all(0 <= v <= 1 for v in face['uv'])
    assert (ASSETS / ('textures/block/' + face['texture'] + '.png')).is_file()
print(f'Combat art PASS: {len(manifest)} atlases, {frames} nonempty transparent frames, {len(faces)} portal faces')

# The hand adapter negates authored X rotation. At peak thrust the +Y shaft
# must face camera -Z, not stand upright as the rejected in-game candidate did.
clips = json.loads((ASSETS / 'animations/weapon_choreography.json').read_text('utf8'))
rotations = clips['spear_thrust']['tracks']['weapon']['rotation']
peak = next(row for row in rotations if abs(row[0] - .12) < 1e-6)
for index in range(21, 31):
    model = json.loads((ASSETS / f'models/item/preview_equipment_{index:03d}_held.json').read_text('utf8'))
    for hand in ('firstperson_righthand', 'firstperson_lefthand'):
        display = model['display'][hand]['rotation']
        assert abs(display[0] - peak[1] + 90) < .001, (index, hand, 'spear points away from target')
        assert display[2] == 0 and peak[3] == 0, (index, hand, 'unwanted lateral cant')
print('Active spear axis PASS: all ten spears, both hands, authored-to-runtime sign')
