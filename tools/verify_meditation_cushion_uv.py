"""Catch image-origin mistakes between Blockbench, glTF and Forge's OBJ baker."""
from pathlib import Path
import base64, io, json, struct
from PIL import Image

root = Path(__file__).resolve().parents[1]
source = root / 'asset_sources/cushion-uv-fix-20261020'
assets = root / 'src/main/resources/assets/xiantu'
bb = json.loads((source / 'source.bbmodel').read_text(encoding='utf8'))
gltf = json.loads((source / 'source-export.gltf').read_text(encoding='utf8'))
model = json.loads((assets / 'models/block/meditation_cushion.json').read_text(encoding='utf8'))
assert model['loader'] == 'forge:obj' and model['flip_v'] is False, 'glTF UV already has image-top V=0'
original = Image.open(io.BytesIO(base64.b64decode(bb['textures'][0]['source'].split(',')[1]))).convert('RGBA')
runtime = Image.open(assets / 'textures/block/meditation_cushion.png').convert('RGBA')
assert original.size == runtime.size and original.tobytes() == runtime.tobytes(), 'preserve original pixels'
buffers = [base64.b64decode(b['uri'].split(',')[1]) for b in gltf['buffers']]
def read(index):
    a = gltf['accessors'][index]; v = gltf['bufferViews'][a['bufferView']]
    size = {'VEC2': 2, 'VEC3': 3}[a['type']]
    assert a['componentType'] == 5126
    start = a.get('byteOffset', 0) + v.get('byteOffset', 0)
    return [struct.unpack_from('<'+'f'*size, buffers[v['buffer']], start+i*v.get('byteStride', size*4)) for i in range(a['count'])]
expected = []
directions = {(0,1,0):'up',(0,-1,0):'down',(1,0,0):'east',(-1,0,0):'west',(0,0,1):'south',(0,0,-1):'north'}
assert len(bb['elements']) == len(gltf['meshes']) == 77
for cube, mesh in zip(bb['elements'], gltf['meshes']):
    attrs = mesh['primitives'][0]['attributes']
    uv, normals = read(attrs['TEXCOORD_0']), read(attrs['NORMAL'])
    expected.extend(uv)
    for normal, face in directions.items():
        coords = [t for t, n in zip(uv, normals) if all(abs(n[i]-normal[i]) < 1e-6 for i in range(3))]
        assert len(coords) == 4
        actual = [min(t[0] for t in coords)*64,min(t[1] for t in coords)*64,max(t[0] for t in coords)*64,max(t[1] for t in coords)*64]
        rect = cube['faces'][face]['uv']
        bounds = [min(rect[0],rect[2]),min(rect[1],rect[3]),max(rect[0],rect[2]),max(rect[1],rect[3])]
        assert all(abs(a-b)<1e-5 for a,b in zip(actual,bounds)), (cube['name'],face,'changed UV rectangle')
obj = (assets / 'models/block/meditation_cushion.obj').read_text(encoding='utf8').splitlines()
actual = [tuple(map(float,l.split()[1:])) for l in obj if l.startswith('vt ')]
assert len(actual) == len(expected) == 1848
assert all(abs(a-b)<1e-7 for p,q in zip(actual,expected) for a,b in zip(p,q)), 'per-vertex UV order must survive export'
assert sum(l.startswith('f ') for l in obj) == 924
print('Cushion UV PASS: 77 parts, 462 face rectangles, 1848 UV vertices, identical source pixels, no V inversion')
