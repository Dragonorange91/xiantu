"""Import the supplied reincarnation-stone GLB and derive a small GUI sprite."""
import hashlib
import json
import struct
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/xiantu'
SOURCE = (Path.home() / 'Downloads/轮回石.glb')
REFERENCE = ROOT / 'asset_sources/references/reincarnation_stone_reference_v2.png'
ID = 'reincarnation_stone'


def parse_glb(raw):
    if len(raw) < 20 or struct.unpack_from('<III', raw) != (0x46546C67, 2, len(raw)):
        raise ValueError('Expected a valid GLB 2.0 file')
    chunks = {}
    offset = 12
    while offset < len(raw):
        length, kind = struct.unpack_from('<II', raw, offset)
        offset += 8
        if length % 4 or offset + length > len(raw) or kind in chunks:
            raise ValueError('Invalid or duplicate GLB chunk')
        chunks[kind] = raw[offset:offset + length]
        offset += length
    return json.loads(chunks[0x4E4F534A].rstrip(b' ')), chunks.get(0x004E4942, b'')


def normalize_placement(doc):
    primitive, = doc['meshes'][0]['primitives']
    accessor = doc['accessors'][primitive['attributes']['POSITION']]
    low, high = accessor['min'], accessor['max']
    extents = [b - a for a, b in zip(low, high)]
    scale = 0.86 / max(extents)
    translation = [0.5 - (a + b) * 0.5 * scale for a, b in zip(low, high)]
    return scale, translation, low, high


def make_icon():
    with Image.open(REFERENCE) as image:
        image.load()
        source = ImageOps.contain(image.convert('RGB'), (700, 700), Image.Resampling.LANCZOS)
    rgb = np.asarray(source)
    h, w = rgb.shape[:2]
    mask = np.full((h, w), cv2.GC_PR_BGD, np.uint8)
    mask[h // 7:6 * h // 7, w // 7:6 * w // 7] = cv2.GC_PR_FGD
    mask[h // 3:2 * h // 3, w // 3:2 * w // 3] = cv2.GC_FGD
    mask[:5, :] = mask[-5:, :] = mask[:, :5] = mask[:, -5:] = cv2.GC_BGD
    cv2.setRNGSeed(0)
    cv2.grabCut(cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR), mask, None,
                np.zeros((1, 65), np.float64), np.zeros((1, 65), np.float64),
                6, cv2.GC_INIT_WITH_MASK)
    alpha = np.where((mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 255, 0).astype(np.uint8)
    alpha = cv2.morphologyEx(alpha, cv2.MORPH_CLOSE, np.ones((7, 7), np.uint8))
    count, labels, stats, _ = cv2.connectedComponentsWithStats(alpha, 8)
    if count <= 1:
        raise ValueError('Could not isolate stone from the reference')
    largest = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    alpha = np.where(labels == largest, 255, 0).astype(np.uint8)
    rgba = source.convert('RGBA')
    rgba.putalpha(Image.fromarray(alpha).resize(source.size, Image.Resampling.LANCZOS))
    bounds = rgba.getchannel('A').getbbox()
    if not bounds:
        raise ValueError('Isolated stone has an empty silhouette')
    sprite = ImageOps.contain(rgba.crop(bounds), (60, 60), Image.Resampling.LANCZOS)
    icon = Image.new('RGBA', (64, 64))
    icon.alpha_composite(sprite, ((64 - sprite.width) // 2, (64 - sprite.height) // 2))
    icon.putalpha(icon.getchannel('A').point(lambda a: 255 if a >= 128 else 0))
    target = ASSETS / 'textures/item' / f'{ID}.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    icon.save(target)
    return hashlib.sha256(target.read_bytes()).hexdigest()


def main():
    raw = SOURCE.read_bytes()
    doc, _ = parse_glb(raw)
    if len(doc.get('nodes', [])) != 1 or len(doc.get('meshes', [])) != 1:
        raise ValueError('Expected one node and one mesh; preserve source geometry for review')
    if doc.get('skins') or doc.get('animations'):
        raise ValueError('Static item GLB unexpectedly contains skinning or animation')
    primitive, = doc['meshes'][0]['primitives']
    if primitive.get('mode', 4) != 4 or 'TEXCOORD_0' not in primitive['attributes']:
        raise ValueError('Expected indexed triangles with UV coordinates')
    scale, translation, low, high = normalize_placement(doc)
    models = ASSETS / 'models/item'
    models.mkdir(parents=True, exist_ok=True)
    (models / f'{ID}.glb').write_bytes(raw)
    model = {
        'parent': 'minecraft:builtin/entity', 'model': f'xiantu:models/item/{ID}.glb',
        'textures': {'particle': f'xiantu:item/{ID}'}, 'gui_light': 'front',
        'ambientocclusion': False, 'scale': scale, 'translation': translation,
        'display': {
            'gui': {'rotation': [12, -24, -8], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
            'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
            'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 3, 1], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 3, 1], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.5, 0.5, 0.5]},
        },
    }
    (models / f'{ID}_glb.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
    wrapper = {
        'parent': 'minecraft:block/block', 'loader': 'forge:separate_transforms',
        'gui_light': 'front', 'textures': {'particle': f'xiantu:item/{ID}'},
        'base': {'parent': f'xiantu:item/{ID}_glb'},
        'perspectives': {'gui': {'parent': 'minecraft:item/generated', 'gui_light': 'front',
                                 'textures': {'layer0': f'xiantu:item/{ID}'}}},
    }
    (models / f'{ID}.json').write_text(json.dumps(wrapper, indent=2) + '\n', encoding='utf-8')
    icon_hash = make_icon()
    triangles = doc['accessors'][primitive['indices']]['count'] // 3
    record = {
        'item': ID, 'source': str(SOURCE), 'sha256': hashlib.sha256(raw).hexdigest(),
        'texture_sha256': icon_hash, 'gui_texture_size': [64, 64], 'triangles': triangles,
        'source_bounds': [low, high], 'scale': scale, 'translation': translation,
        'renderer': 'native_triangles_gpu_cache', 'materials': len(doc.get('materials', [])),
        'images': len(doc.get('images', [])), 'embedded_materials_preserved': True,
    }
    manifest_dir = ROOT / 'asset_sources/reincarnation_stone'
    manifest_dir.mkdir(parents=True, exist_ok=True)
    (manifest_dir / 'import-manifest.json').write_text(json.dumps(record, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f"Imported {ID}: {triangles} triangles, {record['materials']} materials, {record['images']} embedded images")


if __name__ == '__main__':
    main()
