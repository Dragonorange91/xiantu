"""Build a labeled contact sheet from Blender-rendered Yanwei Fox action previews."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
PREVIEWS = ROOT / "asset_sources/yanwei-fox-gfbs-animation-20261014/previews"
NAMES = ("idle", "walk", "alert", "attack", "hurt", "death")
LABELS = ("Idle", "Walk", "Alert", "Attack", "Hurt", "Death")
CELL_W, CELL_H = 450, 378
sheet = Image.new("RGB", (3 * CELL_W, 2 * CELL_H), (24, 27, 32))
draw = ImageDraw.Draw(sheet)
font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 24)

for index, (name, label) in enumerate(zip(NAMES, LABELS)):
    image = Image.open(PREVIEWS / f"{name}.png").convert("RGB")
    image.thumbnail((430, 330), Image.Resampling.LANCZOS)
    x = index % 3 * CELL_W + (CELL_W - image.width) // 2
    y = index // 3 * CELL_H + 34
    sheet.paste(image, (x, y))
    draw.text((index % 3 * CELL_W + 16, index // 3 * CELL_H + 8), label, fill=(235, 238, 242), font=font)

output = PREVIEWS / "animation-contact-sheet.png"
sheet.save(output)
print(output)
