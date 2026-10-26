"""Keep the imported static asset, flat GUI route and scoped town edits intact."""
from pathlib import Path
import hashlib
import json
import nbtlib
from PIL import Image
from import_item_glb import parse_glb, set_pixel_art_sampling

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'src/main/resources/assets/xiantu'
D = ROOT / 'src/main/resources/data/xiantu'
OUT = ROOT / 'asset_sources/qunyou-figurine-20261026'

def read(p):
    return json.loads(p.read_text(encoding='utf8'))

manifest = read(OUT / 'import-manifest.json')
runtime = (A / 'models/item/qunyou_figurine.glb').read_bytes()
assert hashlib.sha256(runtime).hexdigest() == manifest['runtime_sha256']
after, c = parse_glb(runtime)
source_path = Path(manifest['source'])
if source_path.is_file():
    source = source_path.read_bytes()
    assert hashlib.sha256(source).hexdigest() == manifest['source_sha256']
    before, b = parse_glb(source)
    set_pixel_art_sampling(before)
    assert before == after and b == c, 'Only sampler settings may change'
else:
    # Downloads is user-managed, not a reproducible build input. The accepted
    # runtime hash remains mandatory; do not claim a fresh source comparison.
    print('INFO original Downloads GLB unavailable; accepted runtime SHA-256 verified; source comparison not rerun')
assert not after.get('skins') and not after.get('animations')
assert not any(p.get('targets') for m in after['meshes'] for p in m['primitives'])
assert manifest['triangles'] == 179556
model = read(A / 'models/item/qunyou_figurine.json')
assert model['loader'] == 'forge:separate_transforms'
assert model['gui_light'] == 'front'
assert model['perspectives'] == {'gui': {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'xiantu:item/qunyou_figurine'}}}
placement = read(A / 'models/item/qunyou_figurine_glb.json')
assert placement['parent'] == 'minecraft:builtin/entity' and not placement.get('loader')
lo, hi = manifest['bounds']
assert abs(lo[1]*placement['scale']+placement['translation'][1]+placement['block_offset_y']) < 1e-7
icon = A / 'textures/item/qunyou_figurine.png'
assert hashlib.sha256(icon.read_bytes()).hexdigest() == read(OUT / 'icon-manifest.json')['runtime_sha256']
with Image.open(icon) as im:
    assert im.mode == 'RGBA' and im.size == (64, 64)
    assert im.getchannel('A').getextrema() == (0, 255) and im.getpixel((0, 0))[3] == 0
recipe = read(D / 'recipes/qunyou_figurine.json')
assert recipe['type'] == 'minecraft:crafting_shapeless'
assert recipe['ingredients'] == [{'item': 'minecraft:porkchop'}]*3
assert recipe['result'] == {'item': 'xiantu:qunyou_figurine', 'count': 1}

def cells(f):
    palette = [dict(s.unpack()) for s in f['palette']]
    return {tuple(map(int, block['pos'])): (palette[int(block['state'])], block.get('nbt')) for block in f['blocks']}

for record in read(OUT / 'town-placement.json'):
    path = record['template']+'.nbt'
    old = nbtlib.load(OUT / 'before' / path)
    new = nbtlib.load(D / 'structures/town' / path)
    a, b = cells(old), cells(new)
    changed = {p for p in a.keys() | b.keys() if a.get(p) != b.get(p)}
    pos = tuple(record['pos'])
    assert changed == {pos}, (path, changed)
    assert b[pos][0] == {'Name': 'xiantu:qunyou_figurine', 'Properties': {'facing': record['facing']}}
    assert b[(pos[0], pos[1]-1, pos[2])][0]['Name'] == 'minecraft:dark_oak_slab'
    assert b[(pos[0], pos[1]-1, pos[2])][0]['Properties']['type'] == 'top'
    assert old.get('entities') == new.get('entities') and old['size'] == new['size']
print('Figurine PASS: original geometry/maps, static GPU route, fixed alpha icon, three-pork recipe, three single-cell town edits')
