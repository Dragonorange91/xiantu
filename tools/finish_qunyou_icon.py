"""Reduce the actual Blender model capture to a fixed inventory sprite."""
from pathlib import Path
import hashlib
import json
from PIL import Image, ImageEnhance

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'asset_sources/qunyou-figurine-20261026'
source = OUT / 'icon-render.png'
image = Image.open(source).convert('RGBA')
assert image.getchannel('A').getextrema() == (0, 255)
bounds = image.getchannel('A').getbbox()
subject = image.crop(bounds)
subject = ImageEnhance.Brightness(subject).enhance(1.45)
subject.thumbnail((60, 60), Image.Resampling.LANCZOS)
icon = Image.new('RGBA', (64, 64))
icon.alpha_composite(subject, ((64-subject.width)//2, (64-subject.height)//2))
pixels = [(r, g, b, a) if a else (0, 0, 0, 0) for r, g, b, a in icon.getdata()]
icon.putdata(pixels)
target = ROOT / 'src/main/resources/assets/xiantu/textures/item/qunyou_figurine.png'
icon.save(target)
(OUT / 'icon-manifest.json').write_text(json.dumps({
    'source': str(source), 'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
    'alpha_bounds': bounds, 'runtime_sha256': hashlib.sha256(target.read_bytes()).hexdigest(),
    'size': [64, 64], 'type': 'fixed model screenshot', 'brightness': 1.45,
}, indent=2)+'\n', encoding='utf8')
print('Fixed 64x64 RGBA screenshot icon saved')
