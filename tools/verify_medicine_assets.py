"""Validate and build review sheets for the Xiantu medicine artwork batch."""
from build_paths import review_font
from pathlib import Path
import argparse
import json
from collections import Counter
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1] / "asset_sources" / "medicine-20261012-v2"
MANIFEST = ROOT / "manifest.json"
TILE = 112
FONT = review_font(14)

def main(check_only: bool = False):
    plant_block = (Path(__file__).resolve().parents[1]
                   / "src/main/java/com/longju/xiantu/medicine/MedicinePlantBlock.java").read_text(encoding="utf-8")
    if "return state.is(Blocks.GRASS_BLOCK);" not in plant_block:
        raise AssertionError("wild medicine plants must only survive on vanilla grass blocks")
    rows = json.loads(MANIFEST.read_text(encoding="utf-8"))
    problems = []
    for row in rows:
        master = Path(row["master"])
        if not master.is_file():
            problems.append(f"{row['id']}: missing master")
            continue
        with Image.open(master) as image:
            if image.size != (1024, 1024): problems.append(f"{row['id']}: master {image.size}")
        for path_index, path_string in enumerate(row["runtime"]):
            path = Path(path_string)
            if not path.is_file():
                problems.append(f"{row['id']}: missing {path.name}")
                continue
            with Image.open(path) as image:
                if image.size != (64, 64): problems.append(f"{row['id']}: {path.name} {image.size}")
                if row["kind"] not in ("spirit_soil", "spirit_farmland") and image.getextrema()[3][0] == 255:
                    problems.append(f"{row['id']}: opaque cutout {path.name}")
                if image.getpixel((0, 0))[:3] == (255, 0, 255):
                    problems.append(f"{row['id']}: magenta key remains {path.name}")
                if row["kind"] == "plant_lifecycle" and path_index >= 2:
                    bounds = image.convert("RGBA").getchannel("A").getbbox()
                    if bounds is None or bounds[3] != 64:
                        problems.append(f"{row['id']}: {path.name} does not touch its ground baseline")
                    elif abs((bounds[0] + bounds[2]) / 2 - 32) > 2:
                        problems.append(f"{row['id']}: {path.name} is horizontally off-center")
    if problems:
        print("FAIL", len(problems))
        print("\n".join(problems))
        raise SystemExit(1)
    plants = [row for row in rows if row["kind"] == "plant_lifecycle"]
    print("PASS", len(rows), dict(Counter(row["kind"] for row in rows)))
    print("seed-eligible plants", sum(bool(row.get("seed_registered")) for row in plants))
    if check_only:
        return
    for panel in ("item", "seed", "stage1", "stage2", "stage3", "wild_mature"):
        entries = plants if panel != "item" else [row for row in rows if row["kind"] not in ("spirit_soil", "spirit_farmland")]
        columns = 8
        cell_h = TILE + 28
        sheet = Image.new("RGB", (columns * TILE, ((len(entries) + columns - 1) // columns) * cell_h), (37, 38, 40))
        draw = ImageDraw.Draw(sheet)
        for i, row in enumerate(entries):
            x, y = (i % columns) * TILE, (i // columns) * cell_h
            path = Path(row["runtime"][0]) if row["kind"] == "spirit_soil" else Path(row["runtime"][0] if panel == "item" else row["runtime"][("item", "seed", "stage1", "stage2", "stage3", "wild_mature").index(panel)])
            with Image.open(path) as image:
                tile = image.convert("RGBA").resize((TILE, TILE), Image.Resampling.NEAREST)
            sheet.paste(tile, (x, y), tile)
            draw.text((x + 2, y + TILE + 2), row["name"][:10], font=FONT, fill=(240, 238, 230))
        sheet.save(ROOT / f"review_{panel}.png")
    for kind, preview in (("spirit_soil", "review_soils.png"), ("spirit_farmland", "review_farmland.png")):
        entries = [row for row in rows if row["kind"] == kind]
        if not entries:
            continue
        columns = 7
        sheet = Image.new("RGB", (columns * TILE, ((len(entries) + columns - 1) // columns) * (TILE + 28)), (37, 38, 40))
        draw = ImageDraw.Draw(sheet)
        for i, row in enumerate(entries):
            x, y = (i % columns) * TILE, (i // columns) * (TILE + 28)
            with Image.open(row["runtime"][0]) as image:
                sheet.paste(image.convert("RGB").resize((TILE, TILE), Image.Resampling.NEAREST), (x, y))
            draw.text((x + 2, y + TILE + 2), row["name"], font=FONT, fill=(240, 238, 230))
        sheet.save(ROOT / preview)
    print("review sheets", ROOT)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check-only", action="store_true", help="validate without writing review sheets")
    main(parser.parse_args().check_only)
