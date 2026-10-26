from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1] / "asset_sources/material-textures-20261013"


def main() -> int:
    parser = argparse.ArgumentParser(description="Reduce 1024px mineral masters to 64px Minecraft runtime textures.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--name", help="Process only one asset prefix, e.g. hanxing_iron_ore")
    args = parser.parse_args()

    masters = args.root / "masters"
    runtime = args.root / "runtime"
    originals = args.root / "api-originals"
    runtime.mkdir(parents=True, exist_ok=True)
    paths = sorted(masters.glob("*_1024.png"))
    if args.name:
        paths = [p for p in paths if p.name == f"{args.name}_1024.png"]
    if not paths:
        raise SystemExit("No matching 1024px mineral masters found.")

    entries: list[tuple[str, Image.Image]] = []
    master_entries: list[tuple[str, Image.Image]] = []
    dimension_record = args.root / "api_source_dimensions.json"
    dimensions: dict[str, list[int]] = {}
    if dimension_record.exists():
        dimensions = json.loads(dimension_record.read_text(encoding="utf-8"))
    for path in paths:
        with Image.open(path) as opened:
            source = opened.convert("RGB")
            if source.width != source.height:
                raise SystemExit(f"Expected square image master, found {source.size}: {path}")
            if source.size != (1024, 1024):
                dimensions[path.name] = list(source.size)
            else:
                dimensions.setdefault(path.name, list(source.size))
            if source.size != (1024, 1024):
                originals.mkdir(parents=True, exist_ok=True)
                original = originals / path.name
                if not original.exists():
                    original.write_bytes(path.read_bytes())
                source = source.resize((1024, 1024), Image.Resampling.LANCZOS)
                source.save(path, optimize=True)
            reduced = source.resize((64, 64), Image.Resampling.LANCZOS)
            master_entries.append((path.name, source.copy()))
        target = runtime / f"{path.name.removesuffix('_1024.png')}.png"
        reduced.save(target, optimize=True)
        entries.append((target.name, reduced.copy()))
        print(f"{path.name} -> {target.name}: 1024x1024 -> 64x64 RGB")

    (args.root / "api_source_dimensions.json").write_text(
        json.dumps(dimensions, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    columns = min(4, len(entries))
    cell = 280
    rows = (len(entries) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * cell, rows * (cell + 32)), (34, 36, 42))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 16)
    except OSError:
        font = ImageFont.load_default()
    for index, (name, texture) in enumerate(entries):
        x = (index % columns) * cell
        y = (index // columns) * (cell + 32)
        enlarged = texture.resize((cell, cell), Image.Resampling.NEAREST)
        sheet.paste(enlarged, (x, y))
        draw.text((x + 5, y + cell + 7), name, fill=(242, 242, 242), font=font)
    preview = args.root / "runtime_contact_sheet.png"
    sheet.save(preview, optimize=True)
    print(f"Contact sheet: {preview}")

    master_sheet = Image.new("RGB", (columns * cell, rows * (cell + 32)), (34, 36, 42))
    master_draw = ImageDraw.Draw(master_sheet)
    for index, (name, master) in enumerate(master_entries):
        x = (index % columns) * cell
        y = (index // columns) * (cell + 32)
        thumb = master.resize((cell, cell), Image.Resampling.LANCZOS)
        master_sheet.paste(thumb, (x, y))
        master_draw.text((x + 5, y + cell + 7), name, fill=(242, 242, 242), font=font)
    master_preview = args.root / "master_contact_sheet.png"
    master_sheet.save(master_preview, optimize=True)
    print(f"Master contact sheet: {master_preview}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
