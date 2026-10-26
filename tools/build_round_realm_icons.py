"""Derive circular HUD badges; preserve accepted source artwork unchanged."""
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/xiantu/textures/gui/realm'
output = root / 'round'
output.mkdir(exist_ok=True)
for source in sorted(root.glob('*.png')):
    image = Image.open(source).convert('RGBA')
    width, height = image.size
    assert width == height
    for y in range(height):
        for x in range(width):
            if (x - (width - 1) / 2) ** 2 + (y - (height - 1) / 2) ** 2 > (width / 2) ** 2:
                r, g, b, _ = image.getpixel((x, y))
                image.putpixel((x, y), (r, g, b, 0))
    image.save(output / source.name)
print('Circular realm badges generated from original artwork')
