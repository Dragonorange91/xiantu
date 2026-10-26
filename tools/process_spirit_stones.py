from pathlib import Path

from PIL import Image, ImageDraw


ROOT = (Path.home() / 'Documents/Codex/2026-10-05/f/outputs/images/spirit_stone_rebuild')
IDS = ["fan_ling_shi", "ling_jing", "ling_yu", "xuan_jing", "di_sui", "tian_jing", "xian_yu", "dao_shi"]


sheet = Image.new("RGB", (800, 520), (32, 34, 38))
draw = ImageDraw.Draw(sheet)
for index, asset_id in enumerate(IDS):
    master_path = ROOT / f"{asset_id}_master.png"
    master = Image.open(master_path).convert("RGBA")
    original_size = master.size
    master = master.resize((1024, 1024), Image.Resampling.LANCZOS)
    master.save(master_path)

    icon = master.resize((64, 64), Image.Resampling.LANCZOS)
    icon_path = ROOT / f"{asset_id}_64.png"
    icon.save(icon_path)

    tile = Image.new("RGBA", (128, 128))
    pixels = tile.load()
    for y in range(128):
        for x in range(128):
            shade = 205 if (x // 16 + y // 16) % 2 == 0 else 157
            pixels[x, y] = (shade, shade, shade, 255)
    tile.alpha_composite(icon.resize((128, 128), Image.Resampling.NEAREST))

    x = 24 + index % 4 * 194
    y = 18 + index // 4 * 245
    sheet.paste(tile.convert("RGB"), (x, y))
    draw.text((x, y + 134), asset_id, fill="white")
    print(f"{asset_id}: master {original_size} -> 1024x1024, icon 64x64, alpha={icon.getchannel('A').getextrema()}")

sheet.save(ROOT / "icon_contact.png")
