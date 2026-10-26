from __future__ import annotations
from build_paths import review_font

import json
import subprocess
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "asset_sources/terrain-20261012"
MASTERS = OUT / "masters"
RUNTIME = OUT / "runtime"
API = Path.home() / ".codex/skills/image-api-generator/scripts/generate_image.py"
MANIFEST = OUT / "manifest.json"

MATERIALS = [
    ("thunder_rock", "雷纹玄岩", "A Minecraft Java block side texture of storm-forged basalt: charcoal gray angular volcanic plates, sparse thin branching muted-bronze lightning seams embedded in the rock, broken asymmetrical geology, mostly stone with only a few restrained veins. No symbols, no glowing lines, no ore chunks."),
    ("thunder_soil", "焦雷土", "A Minecraft Java block texture of storm-scorched earth: cracked dark umber soil with shallow impact pits, sparse gray ash and a few dull glassy flecks, uneven natural clods, no lightning icons or crystals."),
    ("meteor_rock", "陨坑岩", "A Minecraft Java block side texture of meteor impact breccia: dark iron-gray fused stone with irregular rust-red fragments and several small rounded impact inclusions, fractured and compact, asymmetric natural geology, no giant crystal veins."),
    ("meteor_soil", "暗赭荒土", "A Minecraft Java block texture of dry impact-basin earth: dark ochre and burnt sienna compact soil, coarse stratified clods, fine scattered gravel and wind-cut cracks, no plants and no large rocks."),
    ("valley_streamstone", "溪石", "A Minecraft Java block side texture of stream-worn cultivation valley stone: rounded but still block-readable blue-gray river rock fragments cemented into a rough matrix, subtle moss in a few creases and fine pale mineral specks, no carved pattern."),
    ("valley_fertile_soil", "青纹沃土", "A Minecraft Java block texture of fertile old medicinal valley earth: deep green-brown loam, visible crumbly layers and dense fine roots, a few thin jade-green mineral threads embedded irregularly, no plants on the surface and no decorative sigils."),
]

BASE_PROMPT = (
    "Create one seamless tileable square Minecraft Java Edition block texture, "
    "1024 by 1024 source image, perfectly flat front-facing orthographic material sample, "
    "no cube render, no perspective, no shadows outside the tile, no border, no text, "
    "crisp stepped pixel clusters and restrained diffuse pixel shading compatible with vanilla Minecraft, "
    "clear readable material breakup at 64x64, naturally irregular asymmetrical geology. "
)


def load_manifest() -> dict[str, dict]:
    if not MANIFEST.exists():
        return {}
    return json.loads(MANIFEST.read_text(encoding="utf-8"))


def save_manifest(records: dict[str, dict]) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    MASTERS.mkdir(parents=True, exist_ok=True)
    RUNTIME.mkdir(parents=True, exist_ok=True)
    records = load_manifest()
    for key, name, description in MATERIALS:
        master = MASTERS / f"{key}_1024.png"
        runtime = RUNTIME / f"{key}_64.png"
        record = records.get(key, {})
        if not (master.is_file() and runtime.is_file() and record.get("status") == "generated"):
            prompt = BASE_PROMPT + f"Material identity: {name}. " + description
            for attempt in range(1, 4):
                result = subprocess.run([
                    sys.executable, str(API), "--prompt", prompt, "--size", "1024x1024",
                    "--quality", "high", "--output", str(master),
                ], check=False)
                if result.returncode == 0 and master.is_file():
                    break
                if attempt == 3:
                    raise SystemExit(f"Image API failed for {name} after three attempts")
            with Image.open(master) as source:
                image = source.convert("RGBA")
                if image.size != (1024, 1024):
                    image = image.resize((1024, 1024), Image.Resampling.LANCZOS)
                image.save(master)
                image.resize((64, 64), Image.Resampling.LANCZOS).save(runtime)
            records[key] = {"name": name, "master": str(master), "runtime": str(runtime), "status": "generated"}
            save_manifest(records)
            print(f"Generated {name}: {master} -> {runtime}", flush=True)
        else:
            print(f"Reused {name}: {runtime}", flush=True)

    from PIL import ImageDraw, ImageFont
    preview = Image.new("RGB", (4 * 240, 3 * 300), (28, 31, 35))
    draw = ImageDraw.Draw(preview)
    font = review_font(16)
    for index, (key, name, _) in enumerate(MATERIALS):
        texture = Image.open(RUNTIME / f"{key}_64.png").convert("RGB").resize((192, 192), Image.Resampling.NEAREST)
        x = (index % 4) * 240 + 24
        y = (index // 4) * 300 + 16
        preview.paste(texture, (x, y))
        draw.text((x, y + 200), name, font=font, fill=(238, 239, 235))
    preview.save(OUT / "terrain-textures-review.png")


if __name__ == "__main__":
    main()
