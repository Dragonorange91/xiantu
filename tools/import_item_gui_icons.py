"""Make 64px inventory sprites from the user's approved, grey-backdrop references."""
import hashlib
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageOps

from import_item_glb import ASSETS, ROOT

SOURCE = (Path.home() / 'Documents/Codex/2026-10-10/ka/outputs/images')
FILES = {
    'hanxing_pickaxe': ('修仙镐子', '寒星_修仙重设计.png'),
    'jiuyou_pickaxe': ('修仙镐子', '九幽_修仙重设计.png'),
    'hundun_pickaxe': ('修仙镐子', '混沌_正面对称版.png'),
    'fan_ling_shi': ('八阶灵石_建模参考', '01阶_凡灵石_高清建模参考.png'),
    'ling_jing': ('八阶灵石_建模参考', '02阶_灵晶_高清建模参考.png'),
    'ling_yu': ('八阶灵石_建模参考', '03阶_灵玉_高清建模参考.png'),
    'xuan_jing': ('八阶灵石_建模参考', '04阶_玄晶_高清建模参考.png'),
    'di_sui': ('八阶灵石_建模参考', '05阶_地髓_高清建模参考.png'),
    'tian_jing': ('八阶灵石_建模参考', '06阶_天晶_高清建模参考.png'),
    'xian_yu': ('八阶灵石_建模参考', '07阶_仙玉_高清建模参考.png'),
    'dao_shi': ('八阶灵石_建模参考', '08阶_道石_高清建模参考.png'),
}


def isolate(image):
    # GrabCut learns a foreground/background model, rather than color-keying
    # away grey/white facets inside the silver tool and transparent crystals.
    small = ImageOps.contain(image.convert('RGB'), (640, 640), Image.Resampling.LANCZOS)
    rgb = np.asarray(small)
    pixels = rgb.astype(np.int16)
    chroma = pixels.max(axis=2) - pixels.min(axis=2)
    light = pixels.mean(axis=2)
    mask = np.full(rgb.shape[:2], cv2.GC_PR_BGD, np.uint8)
    foreground = (chroma > 22) | (light < 160) | (light > 240)
    mask[foreground] = cv2.GC_FGD
    mask[:6, :] = mask[-6:, :] = mask[:, :6] = mask[:, -6:] = cv2.GC_BGD
    cv2.setRNGSeed(0)
    cv2.grabCut(cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR), mask, None,
                np.zeros((1, 65), np.float64), np.zeros((1, 65), np.float64),
                5, cv2.GC_INIT_WITH_MASK)
    alpha = np.where((mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 255, 0).astype(np.uint8)
    alpha = cv2.morphologyEx(alpha, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8))
    n, labels, stats, _ = cv2.connectedComponentsWithStats(alpha, 8)
    assert n > 1, 'Foreground silhouette missing'
    largest = 1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])
    alpha = np.where(labels == largest, 255, 0).astype(np.uint8)
    # Preserve inner light/grey facets enclosed by the exterior silhouette.
    contours, _ = cv2.findContours(alpha, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    cv2.drawContours(alpha, contours, -1, 255, cv2.FILLED)
    alpha_image = Image.fromarray(alpha).resize(image.size, Image.Resampling.LANCZOS)
    result = image.convert('RGBA')
    result.putalpha(alpha_image)
    return result


def main():
    target = ROOT / 'asset_sources/item_glb/gui_icons'
    target.mkdir(parents=True, exist_ok=True)
    records = []
    overview = Image.new('RGBA', (4 * 176, 3 * 180), (55, 57, 62, 255))
    for index, (item, (folder, filename)) in enumerate(FILES.items()):
        source = SOURCE / folder / filename
        with Image.open(source) as image:
            image.load()
            separated = isolate(image)
            bounds = separated.getchannel('A').point(lambda a: 255 if a >= 128 else 0).getbbox()
            assert bounds
            cropped = separated.crop(bounds)
            rotation = -40 if item == 'hundun_pickaxe' else 0
            if rotation:
                # Rotate the high-resolution silhouette before reduction, not the 64px sprite.
                cropped = cropped.rotate(rotation, Image.Resampling.BICUBIC, expand=True)
                rotated_bounds = cropped.getchannel('A').point(lambda a: 255 if a >= 128 else 0).getbbox()
                assert rotated_bounds
                cropped = cropped.crop(rotated_bounds)
            sprite = ImageOps.contain(cropped, (60, 60), Image.Resampling.LANCZOS)
            canvas = Image.new('RGBA', (64, 64))
            canvas.alpha_composite(sprite, ((64 - sprite.width) // 2, (64 - sprite.height) // 2))
            # Minecraft cutout alpha: stable contour with no grey backdrop fringe.
            canvas.putalpha(canvas.getchannel('A').point(lambda a: 255 if a >= 128 else 0))
            canvas.save(ASSETS / 'textures/item' / (item + '.png'))
            canvas.save(target / (item + '_64.png'))
            separated.save(target / (item + '_isolated.png'))
            overview.alpha_composite(canvas.resize((160, 160), Image.Resampling.NEAREST),
                                     ((index % 4) * 176 + 8, (index // 4) * 180 + 4))
            records.append({'item': item, 'source': str(source),
                            'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                            'source_size': list(image.size), 'crop': list(bounds),
                            'rotation_degrees': rotation,
                            'texture_sha256': hashlib.sha256((target / (item + '_64.png')).read_bytes()).hexdigest()})
            print('64px GUI sprite:', item)
    overview.save(target / 'overview.png')
    (target / 'manifest.json').write_text(json.dumps(records, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
