from __future__ import annotations

import json
import subprocess
import sys
from collections import deque
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "asset_sources/terrain-20261012"
MASTERS = OUT / "masters/plants"
RUNTIME = OUT / "runtime/plants"
API = Path.home() / ".codex/skills/image-api-generator/scripts/generate_image.py"
MANIFEST = OUT / "plants_manifest.json"
PLANTS = [
    ("thunder_sedge", "电弧草", "a small upright clump of four angular indigo-silver grass blades with a few short muted bronze tips, storm-shaped but not glowing"),
    ("meteor_ash_grass", "灰烬草", "a low dry tuft of ash-gray grass with rust-colored broken seed heads and wind-bent blades, sparse and brittle"),
    ("valley_jade_fern", "谷地蕨", "a compact jade-green fern with three broad stepped fronds and a curled young shoot, lush but small"),
]
BASE = (
    "Create a single centered Minecraft Java Edition crossed-plant sprite texture, "
    "1024x1024 source, front orthographic view, complete plant from tips to base visible, "
    "true hard-edged pixel clusters, stepped diffuse shading, restrained vanilla-compatible palette, "
    "transparent-looking solid pure magenta #FF00FF background for edge masking, "
    "no soil, no pot, no shadow, no text, no extra objects, no clipped leaves. Subject: "
)


def key_background(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    pixels = image.load()
    width, height = image.size
    seen = bytearray(width * height)
    queue = deque()

    def is_background(x: int, y: int) -> bool:
        r, g, b, _ = pixels[x, y]
        return r > 170 and g < 100 and b > 145

    for x in range(width):
        if is_background(x, 0): queue.append((x, 0))
        if is_background(x, height - 1): queue.append((x, height - 1))
    for y in range(height):
        if is_background(0, y): queue.append((0, y))
        if is_background(width - 1, y): queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        index = y * width + x
        if seen[index] or not is_background(x, y):
            continue
        seen[index] = 1
        pixels[x, y] = (0, 0, 0, 0)
        if x: queue.append((x - 1, y))
        if x + 1 < width: queue.append((x + 1, y))
        if y: queue.append((x, y - 1))
        if y + 1 < height: queue.append((x, y + 1))

    return image


def main() -> None:
    MASTERS.mkdir(parents=True, exist_ok=True)
    RUNTIME.mkdir(parents=True, exist_ok=True)
    records = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {}
    for key, name, description in PLANTS:
        master = MASTERS / f"{key}_1024.png"
        runtime = RUNTIME / f"{key}_64.png"
        if not (master.exists() and runtime.exists() and records.get(key, {}).get("status") == "generated"):
            for attempt in range(1, 4):
                result = subprocess.run([
                    sys.executable, str(API), "--prompt", BASE + description,
                    "--size", "1024x1024", "--quality", "high", "--output", str(master),
                ], check=False)
                if result.returncode == 0 and master.exists():
                    break
                if attempt == 3:
                    raise SystemExit(f"Image API failed for {name} after three attempts")
            with Image.open(master) as source:
                image = source.convert("RGBA")
                if image.size != (1024, 1024):
                    image = image.resize((1024, 1024), Image.Resampling.LANCZOS)
                image = key_background(image)
                bounds = image.getchannel("A").getbbox()
                if bounds is None:
                    raise SystemExit(f"The keyed sprite for {name} is empty")
                sprite = image.crop(bounds)
                sprite.thumbnail((56, 60), Image.Resampling.LANCZOS)
                canvas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
                canvas.alpha_composite(sprite, ((64 - sprite.width) // 2, 64 - sprite.height))
                canvas.save(runtime)
            records[key] = {"name": name, "master": str(master), "runtime": str(runtime), "status": "generated"}
            MANIFEST.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            print(f"Generated {name}: {master} -> {runtime}", flush=True)
        else:
            print(f"Reused {name}: {runtime}", flush=True)

    preview = Image.new("RGB", (5 * 192, 240), (32, 35, 40))
    for index, (key, _, _) in enumerate(PLANTS):
        sprite = Image.open(RUNTIME / f"{key}_64.png").convert("RGBA")
        preview.paste(sprite.resize((128, 128), Image.Resampling.NEAREST), (index * 192 + 32, 16),
                      sprite.resize((128, 128), Image.Resampling.NEAREST))
    preview.save(OUT / "terrain-plants-review.png")


if __name__ == "__main__":
    main()
