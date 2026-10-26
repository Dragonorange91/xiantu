"""Validate the supplied reincarnation-stone GLB and Forge/GFBS item routes."""
import hashlib
import json
import math
import struct
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/xiantu'
ID = 'reincarnation_stone'


def parse_glb(raw):
    assert len(raw) >= 20 and struct.unpack_from('<III', raw) == (0x46546C67, 2, len(raw))
    chunks = {}
    offset = 12
    while offset < len(raw):
        length, kind = struct.unpack_from('<II', raw, offset)
        offset += 8
        assert length % 4 == 0 and offset + length <= len(raw) and kind not in chunks
        chunks[kind] = raw[offset:offset + length]
        offset += length
    return json.loads(chunks[0x4E4F534A].rstrip(b' ')), chunks.get(0x004E4942, b'')


def main():
    manifest = json.loads((ROOT / 'asset_sources/reincarnation_stone/import-manifest.json').read_text(encoding='utf-8'))
    raw = (ASSETS / 'models/item' / f'{ID}.glb').read_bytes()
    assert hashlib.sha256(raw).hexdigest() == manifest['sha256']
    doc, _ = parse_glb(raw)
    assert doc['asset']['version'] == '2.0'
    assert len(doc['nodes']) == len(doc['meshes']) == 1
    assert not doc.get('skins') and not doc.get('animations') and not doc.get('extensionsRequired')
    primitive, = doc['meshes'][0]['primitives']
    assert primitive.get('mode', 4) == 4 and 'TEXCOORD_0' in primitive['attributes']
    assert len(doc.get('materials', [])) == manifest['materials']
    assert len(doc.get('images', [])) == manifest['images']
    assert all(image.get('bufferView') is not None and image.get('mimeType', '').startswith('image/')
               for image in doc['images'])
    positions = doc['accessors'][primitive['attributes']['POSITION']]
    assert positions['count'] > 0 and all(math.isfinite(v) for v in positions['min'] + positions['max'])
    assert doc['accessors'][primitive['indices']]['count'] // 3 == manifest['triangles']
    assert manifest['renderer'] == 'native_triangles_gpu_cache' and manifest['embedded_materials_preserved']
    model = json.loads((ASSETS / 'models/item' / f'{ID}_glb.json').read_text(encoding='utf-8'))
    assert model['parent'] == 'minecraft:builtin/entity' and model['model'] == f'xiantu:models/item/{ID}.glb'
    assert set(model['display']) == {'gui', 'ground', 'fixed', 'head', 'thirdperson_righthand',
                                     'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand'}
    for axis in range(3):
        a = positions['min'][axis] * model['scale'] + model['translation'][axis]
        b = positions['max'][axis] * model['scale'] + model['translation'][axis]
        assert 0.04 <= a < b <= 0.96 and abs((a + b) / 2 - 0.5) < 1e-6
    wrapper = json.loads((ASSETS / 'models/item' / f'{ID}.json').read_text(encoding='utf-8'))
    assert wrapper['loader'] == 'forge:separate_transforms'
    assert wrapper['base'] == {'parent': f'xiantu:item/{ID}_glb'}
    assert wrapper['perspectives']['gui']['parent'] == 'minecraft:item/generated'
    icon_path = ASSETS / 'textures/item' / f'{ID}.png'
    assert hashlib.sha256(icon_path.read_bytes()).hexdigest() == manifest['texture_sha256']
    with Image.open(icon_path) as image:
        image.load()
        assert image.mode == 'RGBA' and image.size == (64, 64) and image.getchannel('A').getextrema() == (0, 255)
    source = Path(manifest['source']).read_bytes()
    assert hashlib.sha256(source).hexdigest() == manifest['sha256'], 'Imported GLB no longer matches supplied source'
    print(f"Reincarnation Stone GLB PASS: {manifest['triangles']} triangles; embedded PBR maps intact; GUI and native routes valid")


if __name__ == '__main__':
    main()
