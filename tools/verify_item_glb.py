"""Validate imported meshes/atlas integrity, real 3D bounds and display/dependency wiring."""
import hashlib
import io
import json
import math
import struct
import tomllib
from pathlib import Path

from PIL import Image
from import_item_glb import ASSETS, ROOT, MODELS, parse_glb


def main():
    records = json.loads((ROOT / 'asset_sources/item_glb/import-manifest.json').read_text(encoding='utf-8'))
    assert {r['item'] for r in records} == set(MODELS) and len(records) == len(MODELS)
    for record in records:
        item = record['item']
        raw = (ASSETS / 'models/item' / (item + '.glb')).read_bytes()
        assert hashlib.sha256(raw).hexdigest() == record['sha256'], 'Imported GLB changed'
        doc, binary = parse_glb(raw)
        assert doc['asset']['version'] == '2.0' and not doc.get('extensionsRequired')
        assert doc.get('samplers') and all(
            sampler.get('magFilter') == 9728 and sampler.get('minFilter') == 9728
            for sampler in doc['samplers']), 'Pixel-art material sampling must use nearest filtering'
        primitive, = doc['meshes'][0]['primitives']
        positions = doc['accessors'][primitive['attributes']['POSITION']]
        assert positions['componentType'] == 5126 and positions['type'] == 'VEC3'
        view = doc['bufferViews'][positions['bufferView']]
        offset = view.get('byteOffset', 0) + positions.get('byteOffset', 0)
        stride = view.get('byteStride', 12)
        vertices = [struct.unpack_from('<3f', binary, offset + n * stride) for n in range(positions['count'])]
        assert all(math.isfinite(v) for vertex in vertices for v in vertex)
        bounds = [[min(v[a] for v in vertices) for a in range(3)], [max(v[a] for v in vertices) for a in range(3)]]
        assert bounds == record['source_bounds']
        assert bounds[1][2] - bounds[0][2] > 0.1, 'Mesh must have actual depth'
        indices = doc['accessors'][primitive['indices']]
        fmt = {5121: 'B', 5123: 'H', 5125: 'I'}[indices['componentType']]
        view = doc['bufferViews'][indices['bufferView']]
        offset = view.get('byteOffset', 0) + indices.get('byteOffset', 0)
        values = struct.unpack_from('<' + str(indices['count']) + fmt, binary, offset)
        assert max(values) < len(vertices) and len(values) % 3 == 0
        assert len(values) // 3 == record['triangles']
        assert record['renderer'] == 'native_triangles_gpu_cache'
        assert not doc.get('skins') and not doc.get('animations')
        assert not primitive.get('targets') and primitive.get('mode', 4) == 4
        wrapper = json.loads((ASSETS / 'models/item' / (item + '.json')).read_text(encoding='utf-8'))
        assert wrapper['loader'] == 'forge:separate_transforms'
        assert wrapper['parent'] == 'minecraft:block/block'
        assert wrapper['base'] == {'parent': 'xiantu:item/' + item + '_glb'}
        assert set(wrapper['perspectives']) == {'gui'}
        gui = wrapper['perspectives']['gui']
        assert gui['parent'] == 'minecraft:item/generated' and 'loader' not in gui
        assert gui['textures']['layer0'] == 'xiantu:item/' + item
        model = json.loads((ASSETS / 'models/item' / (item + '_glb.json')).read_text(encoding='utf-8'))
        assert model['model'] == 'xiantu:models/item/' + item + '.glb'
        assert model['parent'] == 'minecraft:builtin/entity'
        assert 'loader' not in model and not model.get('elements')
        for axis in range(3):
            low = bounds[0][axis] * model['scale'] + model['translation'][axis]
            high = bounds[1][axis] * model['scale'] + model['translation'][axis]
            assert 0.049 <= low < high <= 0.951, 'Normalized item fits canvas'
            assert abs((low + high) / 2 - 0.5) < 1e-6
        material = doc['materials'][primitive.get('material', 0)]
        image_index = doc['textures'][material['pbrMetallicRoughness']['baseColorTexture']['index']]['source']
        view = doc['bufferViews'][doc['images'][image_index]['bufferView']]
        offset = view.get('byteOffset', 0)
        embedded = binary[offset:offset + view['byteLength']]
        resource = 'xiantu:item/' + item + '_glb_base_color'
        png = (ASSETS / 'textures' / (resource.split(':')[1] + '.png')).read_bytes()
        assert png == embedded and hashlib.sha256(png).hexdigest() == record['texture_sha256']
        with Image.open(io.BytesIO(png)) as image:
            image.load()
            assert list(image.size) == record['texture_size']
        assert model['textures']['particle'] == 'xiantu:item/' + item
        assert material.get('alphaMode', 'OPAQUE') != 'BLEND', 'Resident GPU route requires opaque or mask'
        contexts = {'gui', 'ground', 'fixed', 'head', 'firstperson_righthand',
                    'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand'}
        assert set(model['display']) == contexts
        for display in model['display'].values():
            assert all(math.isfinite(x) for key in ('rotation', 'translation', 'scale') for x in display[key])
            assert all(x > 0 for x in display['scale'])
        print('GLB PASS ' + item + ': mesh/UV atlas/normalization/display/source integrity')
    icons = json.loads((ROOT / 'asset_sources/item_glb/gui_icons/manifest.json').read_text(encoding='utf-8'))
    assert {r['item'] for r in icons} == set(MODELS)
    for record in icons:
        png = (ASSETS / 'textures/item' / (record['item'] + '.png')).read_bytes()
        assert hashlib.sha256(png).hexdigest() == record['texture_sha256']
        with Image.open(io.BytesIO(png)) as icon:
            assert icon.mode == 'RGBA' and icon.size == (64, 64)
            assert icon.getchannel('A').getextrema() == (0, 255)
            assert icon.getpixel((0, 0))[3] == 0
    print('GUI PASS all 11 reference sprites: source mapping, 64px alpha, GUI-only flat routing')
    metadata = tomllib.loads((ROOT / 'src/main/resources/META-INF/mods.toml').read_text(encoding='utf-8'))
    dependency, = [d for d in metadata['dependencies']['xiantu'] if d['modId'] == 'gfbs_gltf']
    assert dependency['mandatory'] and dependency['side'] == 'BOTH' and dependency['versionRange'] == '[1.5.1,2.0.0)'
    print('GLB PASS runtime dependency declaration')


if __name__ == '__main__':
    main()
