"""Keep every finished talisman fully inscribed in red after resource regeneration."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/xiantu'
ART = ROOT / 'asset_sources/talisman-20261025'
manifest = json.loads((ART / 'art-integration-manifest.json').read_text(encoding='utf-8'))
assert manifest['color'] == 'cinnabar_red'
assert manifest['generated_from_scratch'] and not manifest['reference_pixels_used']
assert manifest['finished_count'] == 22 and manifest['materials_separate']
assert len(manifest['symbols']) == len({x['id'] for x in manifest['symbols']}) == 22

for record in manifest['symbols']:
    name = 'talisman_' + record['id']
    path = ASSETS / 'textures/item' / (name + '.png')
    assert hashlib.sha256(path.read_bytes()).hexdigest() == record['sha256'], name + ' source regression'
    image = Image.open(path).convert('RGBA')
    assert image.size == (64, 64), name
    a = np.array(image).astype(np.int32)
    assert set(np.unique(a[:, :, 3])) == {0, 255}, name + ' alpha fringe'
    solid = a[:, :, 3] > 0
    assert not (ndimage.binary_fill_holes(solid) & ~solid).any(), name + ' paper pinholes'
    red = solid & (a[:, :, 0] > 110) & (a[:, :, 0] > a[:, :, 1]*1.6) & (a[:, :, 0] > a[:, :, 2]*1.5)
    bounds = image.getbbox()
    quarters = [int(x.sum()) for x in np.array_split(red[bounds[1]:bounds[3]], 4)]
    assert red.sum() >= 100 and min(quarters) >= 10, name + ' missing inscription'
    assert not (solid & (a[:, :, :3].max(axis=2) < 70)).any(), name + ' black inscription'
    model = json.loads((ASSETS / 'models/item' / (name + '.json')).read_text(encoding='utf-8'))
    assert model['textures']['layer0'] == 'xiantu:item/' + name, name + ' wrong model route'

print('PASS: 22 unique complete RED cinnabar talismans; 64px hard alpha; no paper pinholes; item models resolve')
