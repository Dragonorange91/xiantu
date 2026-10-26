"""Import all eleven user-supplied item GLBs, retaining original geometry and maps."""
import hashlib
import argparse
import io
import json
import struct
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/xiantu'
SOURCES = Path(r'F:\ComfyUI-aki-v3.2\ComfyUI\output\minecraft\weapons_equipment')
MODELS = {
    'fan_ling_shi': '凡灵石.glb', 'ling_jing': '灵晶.glb',
    'ling_yu': '灵玉.glb', 'xuan_jing': '玄晶.glb',
    'di_sui': '地髓.glb', 'tian_jing': '天晶.glb',
    'xian_yu': '仙玉.glb', 'dao_shi': '道石.glb',
    'hanxing_pickaxe': '寒星镐.glb', 'jiuyou_pickaxe': '九幽镐.glb',
    'hundun_pickaxe': '混沌镐.glb',
}


def parse_glb(raw):
    if len(raw) < 20 or struct.unpack_from('<III', raw) != (0x46546C67, 2, len(raw)):
        raise ValueError('Invalid GLB 2 header')
    chunks = {}
    cursor = 12
    while cursor < len(raw):
        length, kind = struct.unpack_from('<II', raw, cursor)
        cursor += 8
        if length % 4 or cursor + length > len(raw) or kind in chunks:
            raise ValueError('Invalid GLB chunk')
        chunks[kind] = raw[cursor:cursor + length]
        cursor += length
    return json.loads(chunks[0x4E4F534A]), chunks[0x004E4942]


def encode_glb(doc, binary):
    json_chunk = json.dumps(doc, separators=(',', ':'), ensure_ascii=False).encode('utf-8')
    json_chunk += b' ' * (-len(json_chunk) % 4)
    total = 12 + 8 + len(json_chunk) + 8 + len(binary)
    return (struct.pack('<III', 0x46546C67, 2, total)
            + struct.pack('<II', len(json_chunk), 0x4E4F534A) + json_chunk
            + struct.pack('<II', len(binary), 0x004E4942) + binary)


def set_pixel_art_sampling(doc):
    samplers = doc.get('samplers', [])
    if not samplers:
        raise ValueError('Pixel-art GLB must declare at least one texture sampler')
    for sampler in samplers:
        sampler['magFilter'] = 9728  # NEAREST
        sampler['minFilter'] = 9728  # NEAREST, avoiding interpolation on enlarged pixel textures


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--use-imported', action='store_true',
                        help='Rebuild display JSON using verified project GLBs, without requiring external source files')
    args = parser.parse_args()
    previous = {}
    if args.use_imported:
        manifest = ROOT / 'asset_sources/item_glb/import-manifest.json'
        previous = {r['item']: r for r in json.loads(manifest.read_text(encoding='utf-8'))}
    records = []
    for item, filename in MODELS.items():
        source = SOURCES / filename
        if args.use_imported:
            raw = (ASSETS / 'models/item' / (item + '.glb')).read_bytes()
            assert hashlib.sha256(raw).hexdigest() == previous[item]['sha256'], 'Imported GLB changed'
            source = Path(previous[item]['source'])
        else:
            raw = source.read_bytes()
        doc, binary = parse_glb(raw)
        set_pixel_art_sampling(doc)
        raw = encode_glb(doc, binary)
        # These supplied files are one untransformed mesh. Fail rather than mis-normalize a new layout.
        assert doc['nodes'] == [{'mesh': 0}] and len(doc['meshes']) == len(doc['materials']) == 1
        assert not doc.get('extensionsRequired') and not doc.get('animations')
        primitive, = doc['meshes'][0]['primitives']
        assert primitive.get('mode', 4) == 4 and 'TEXCOORD_0' in primitive['attributes']
        positions = doc['accessors'][primitive['attributes']['POSITION']]
        low, high = positions['min'], positions['max']
        extent = [b - a for a, b in zip(low, high)]
        scale = 0.90 / max(extent)
        translation = [0.5 - (a + b) * 0.5 * scale for a, b in zip(low, high)]
        material = doc['materials'][primitive.get('material', 0)]
        tex_index = material['pbrMetallicRoughness']['baseColorTexture']['index']
        image = doc['images'][doc['textures'][tex_index]['source']]
        view = doc['bufferViews'][image['bufferView']]
        offset = view.get('byteOffset', 0)
        png = binary[offset:offset + view['byteLength']]
        with Image.open(io.BytesIO(png)) as im:
            assert im.format == 'PNG'
            resolution = list(im.size)
        texture = item + '_glb_base_color'
        (ASSETS / 'textures/item' / (texture + '.png')).write_bytes(png)
        (ASSETS / 'models/item' / (item + '.glb')).write_bytes(raw)
        model = {
            # BEWLR submits native triangles through GFBS GPU caches, without baked quad expansion.
            'parent': 'minecraft:builtin/entity',
            'model': 'xiantu:models/item/' + item + '.glb',
            'textures': {'particle': 'xiantu:item/' + item},
            'gui_light': 'front',
            'ambientocclusion': False,
            'scale': scale,
            'translation': translation,
            'display': {
                'gui': {'rotation': [12, -24, -8], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
                'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
                'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]},
                'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]},
                'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
                'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
            },
        }
        if item.endswith('_pickaxe'):
            # Enlarge only hand contexts, keeping icons, dropped items and source GLBs intact.
            for context in ('firstperson', 'thirdperson'):
                for side in ('righthand', 'lefthand'):
                    display = model['display'][context + '_' + side]
                    display['scale'] = [round(value * 1.15, 4) for value in display['scale']]
                    if item == 'hundun_pickaxe':
                        # Its source handle is vertical; the other two are already tilted ~40 degrees.
                        display['rotation'][2] += -40 if side == 'righthand' else 40
        else:
            # Crystals are palm-sized objects, not long-handled tools.
            for side in ('righthand', 'lefthand'):
                model['display']['thirdperson_' + side] = {
                    'rotation': [0, 0, 0], 'translation': [0, 3, 1], 'scale': [0.5, 0.5, 0.5]}
                model['display']['firstperson_' + side] = {
                    'rotation': [0, -90 if side == 'righthand' else 90, 25 if side == 'righthand' else -25],
                    'translation': [1.13, 3.2, 1.13], 'scale': [0.5, 0.5, 0.5]}
        (ASSETS / 'models/item' / (item + '_glb.json')).write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
        wrapper = {
            'parent': 'minecraft:block/block', 'loader': 'forge:separate_transforms',
            'gui_light': 'front', 'textures': {'particle': 'xiantu:item/' + item},
            'base': {'parent': 'xiantu:item/' + item + '_glb'},
            'perspectives': {'gui': {
                'parent': 'minecraft:item/generated', 'gui_light': 'front',
                'textures': {'layer0': 'xiantu:item/' + item},
            }},
        }
        (ASSETS / 'models/item' / (item + '.json')).write_text(json.dumps(wrapper, indent=2) + '\n', encoding='utf-8')
        triangles = doc['accessors'][primitive['indices']]['count'] // 3
        records.append({'item': item, 'source': str(source), 'sha256': hashlib.sha256(raw).hexdigest(),
                        'texture_sha256': hashlib.sha256(png).hexdigest(), 'texture_size': resolution,
                        'triangles': triangles, 'source_bounds': [low, high],
                        'scale': scale, 'translation': translation,
                        'static_baked_quads_avoided': triangles * 3 * (2 if material.get('doubleSided') else 1),
                        'renderer': 'native_triangles_gpu_cache'})
        print(item + ': ' + str(triangles) + ' triangles; original texture ' + str(resolution))
    target = ROOT / 'asset_sources/item_glb'
    target.mkdir(parents=True, exist_ok=True)
    (target / 'import-manifest.json').write_text(json.dumps(records, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
